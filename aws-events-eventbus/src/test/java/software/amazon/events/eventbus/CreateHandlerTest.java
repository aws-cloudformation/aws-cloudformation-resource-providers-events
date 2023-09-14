package software.amazon.events.eventbus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.model.*;
import software.amazon.cloudformation.Action;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.DelayFactory;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.HandlerRequest;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.WaitStrategy;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    private CreateHandler handler;

    @BeforeEach
    public void setup() {
        reset(eventBridgeClient);
        handler = new CreateHandler(eventBridgeClient);
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, DelayFactory.CONSTANT_DEFAULT_DELAY_FACTORY, WaitStrategy.scheduleForCallbackStrategy());
    }

    @AfterEach
    public void tear_down() {
        verify(eventBridgeClient, atLeastOnce()).serviceName();
    }

    @Test
    public void handleRequest_SimpleSuccess() {

        final ResourceModel model = buildResourceModelWithAllProperties();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        request.getDesiredResourceState().setArn(TEST_ARN);

        final DescribeEventBusResponse describeEventBusResponse = buildDescribeEventBusResponse();

        final CreateEventBusResponse createEventBusResponse = buildCreateEventBusResponse();

        final PutPermissionResponse putPermissionResponse = PutPermissionResponse.builder().build();

        when(eventBridgeClient.describeEventBus(any(DescribeEventBusRequest.class)))
                .thenReturn(describeEventBusResponse);

        when(eventBridgeClient.createEventBus(any(CreateEventBusRequest.class)))
                .thenReturn(createEventBusResponse);

        when(eventBridgeClient.putPermission(any(PutPermissionRequest.class)))
                .thenReturn(putPermissionResponse);


        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_AlreadyExist() {

        final ResourceModel model = buildResourceModelWithAllProperties();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        request.getDesiredResourceState().setArn(TEST_ARN);

        when(eventBridgeClient.createEventBus(any(CreateEventBusRequest.class)))
                .thenThrow(ResourceAlreadyExistsException.class);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode().getMessage()).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AlreadyExists);
    }


    @Test
    public void handleThrottleWithReplaysCreateEventBus_assert() throws Exception {
        //
        // Setup request expectations
        //

        //
        // Setting up API expectations
        //
        when(eventBridgeClient.serviceName()).thenReturn("eventBridgeClient");

        //
        // Java SDK will not match on credentials, so you need to match them with Mockito.argThat()
        //
        final ArgumentMatcher<CreateEventBusRequest> matchEventBusCreate = (request) ->
                request.name().equals(TEST_EVENT_BUS_NAME) &&
                        //
                        // This ensures we are injecting our expected MOCK_CREDENTIALS
                        //
                        checkCredentialsOverride(request.overrideConfiguration());

        final AwsServiceException throttleException = createThrottleException();
        when(eventBridgeClient.createEventBus(argThat(matchEventBusCreate)))
                //
                // first time throw a throttle exception
                //
                .thenThrow(throttleException)
                //
                // second time make it succeed.
                //
                .thenReturn(buildCreateEventBusResponse());


        //DescribeEventBus for stabilization

        final DescribeEventBusResponse describeEventBusResponse = buildDescribeEventBusResponse();

        when(eventBridgeClient.describeEventBus(any(DescribeEventBusRequest.class)))
                .thenReturn(describeEventBusResponse);

        //
        // Test handler calling end-2-end
        //
        final ResourceModel resourceModel = buildResourceModelWithAllProperties();
        //
        // Prepare how CFN will call
        //
        final ProgressEvent<ResourceModel, CallbackContext> result = makeCall(
                resourceModel, null, Action.CREATE, false
        );

        assertThat(result).isNotNull();
        //
        //RPDK recognizes any throttling failures and resets operation status to IN_PROGRESS
        //
        assertThat(result.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);

        //
        // Retry the call with the returned model and callback context
        //
        final ProgressEvent<ResourceModel, CallbackContext> nextAttempt = makeCall(
                resourceModel, null, result.getCallbackContext(), Action.CREATE, false
        );


        assertThat(nextAttempt).isNotNull();
        assertThat(nextAttempt.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(nextAttempt.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(nextAttempt.getResourceModel()).isNotNull();
        final ResourceModel nextAttemptResult = nextAttempt.getResourceModel();
        assertThat(nextAttemptResult.getArn()).isEqualTo(TEST_ARN);
        //
        // Now verify API expectations, we called it 1 time even though CFN makes 2 calls
        //
        verify(eventBridgeClient, times(2)).createEventBus(argThat(matchEventBusCreate));

    }

    @Test
    public void handleThrottleWithReplaysAssociatePolicy_assert() throws Exception {
        //
        // Setup request expectations
        //

        //
        // Setting up API expectations
        //
        when(eventBridgeClient.serviceName()).thenReturn("eventBridgeClient");

        //
        // Java SDK will not match on credentials, so you need to match them with Mockito.argThat()
        //
        final ArgumentMatcher<CreateEventBusRequest> matchEventBusCreate = (request) ->
                request.name().equals(TEST_EVENT_BUS_NAME) &&
                        //
                        // This ensures we are injecting our expected MOCK_CREDENTIALS
                        //
                        checkCredentialsOverride(request.overrideConfiguration());

        final AwsServiceException throttleException = createThrottleException();
        when(eventBridgeClient.createEventBus(argThat(matchEventBusCreate)))
                .thenReturn(buildCreateEventBusResponse());

        //DescribeEventBus for stabilization

        final DescribeEventBusResponse describeEventBusResponse = buildDescribeEventBusResponse();

        when(eventBridgeClient.describeEventBus(any(DescribeEventBusRequest.class)))
                .thenReturn(describeEventBusResponse);


        final ArgumentMatcher<PutPermissionRequest> matchPutPermissionRequest = (request) ->
                request.policy().equals(TEST_POLICY) &&
                        //
                        // This ensures we are injecting our expected MOCK_CREDENTIALS
                        //
                        checkCredentialsOverride(request.overrideConfiguration());

        when(eventBridgeClient.putPermission(argThat(matchPutPermissionRequest)))
                //
                // first time throw a throttle exception
                //
                .thenThrow(throttleException)
                //
                // second time make it succeed.
                //
                .thenReturn(PutPermissionResponse.builder().build());


        //
        // Test handler calling end-2-end
        //
        final ResourceModel resourceModel = buildResourceModelWithAllProperties();
        //
        // Prepare how CFN will call
        //
        final ProgressEvent<ResourceModel, CallbackContext> result = makeCall(
                resourceModel, null, Action.CREATE, false
        );

        assertThat(result).isNotNull();
        //
        //RPDK recognizes any throttling failures and resets operation status to IN_PROGRESS
        //
        assertThat(result.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);

        //
        // Retry the call with the returned model and callback context
        //
        final ProgressEvent<ResourceModel, CallbackContext> nextAttempt = makeCall(
                resourceModel, null, result.getCallbackContext(), Action.CREATE, false
        );


        assertThat(nextAttempt).isNotNull();
        assertThat(nextAttempt.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(nextAttempt.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(nextAttempt.getResourceModel()).isNotNull();
        final ResourceModel nextAttemptResult = nextAttempt.getResourceModel();
        assertThat(nextAttemptResult.getArn()).isEqualTo(TEST_ARN);
        //
        // Now verify API expectations, we called it 1 time even though CFN makes 2 calls
        //
        verify(eventBridgeClient, times(2)).putPermission(argThat(matchPutPermissionRequest));

    }


    @Override
    protected HandlerRequest<ResourceModel, CallbackContext, TypeConfigurationModel> prepareRequest(
            ResourceModel model,
            ResourceModel previousModel,
            CallbackContext callbackContext,
            AwsSessionCredentials credentials,
            Action action,
            Region region,
            boolean isCCAPI) {
        HandlerRequest<ResourceModel, CallbackContext, TypeConfigurationModel> request = super.prepareRequest(
                model, null, callbackContext, credentials, action, region, isCCAPI);
        if (!isCCAPI) {
            request.getRequestData().setLogicalResourceId("myLogicalId");
            request.setStackId("mystack");
        }
        return request;
    }
}
