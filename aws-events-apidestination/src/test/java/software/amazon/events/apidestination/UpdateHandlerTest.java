package software.amazon.events.apidestination;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.ConcurrentModificationException;
import software.amazon.awssdk.services.eventbridge.model.DescribeApiDestinationResponse;
import software.amazon.awssdk.services.eventbridge.model.LimitExceededException;
import software.amazon.awssdk.services.eventbridge.model.ResourceNotFoundException;
import software.amazon.awssdk.services.eventbridge.model.UpdateApiDestinationResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotUpdatableException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static software.amazon.events.apidestination.TestConstants.API_DESTINATION_STATE;
import static software.amazon.events.apidestination.TestConstants.ENDPOINT;
import static software.amazon.events.apidestination.TestConstants.API_DESTINATION_NAME;
import static software.amazon.events.apidestination.TestConstants.API_DESTINATION_ARN;
import static software.amazon.events.apidestination.TestConstants.CONNECTION_ARN;
import static software.amazon.events.apidestination.TestConstants.INVOCATION_RATE_LIMIT;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<EventBridgeClient> proxyClient;

    @Mock
    EventBridgeClient eventBridgeClient;

    final UpdateHandler handler = new UpdateHandler();

    @BeforeEach
    public void setup() {
        // Creates a Mockito wrapper around this object, allowing us to replace the output of the invoke() method.
        proxy = Mockito.spy(new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis()));
        eventBridgeClient = mock(EventBridgeClient.class);
        proxyClient = MOCK_PROXY(proxy, eventBridgeClient);
    }

    //@Test
    public void handleRequest_SimpleSuccess() {

        final ResourceModel model = ResourceModel.builder()
                .name(API_DESTINATION_NAME)
                .connectionArn(CONNECTION_ARN)
                .httpMethod("GET")
                .invocationRateLimitPerSecond(INVOCATION_RATE_LIMIT)
                .invocationEndpoint(ENDPOINT)
                .build();


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        // The expected responses
        UpdateApiDestinationResponse updateResponse = UpdateApiDestinationResponse.builder()
                .apiDestinationArn(API_DESTINATION_NAME)
                .apiDestinationState(API_DESTINATION_STATE)
                .build();

        DescribeApiDestinationResponse describeResponse = DescribeApiDestinationResponse.builder()
                .name(API_DESTINATION_NAME)
                .connectionArn(CONNECTION_ARN)
                .httpMethod("GET")
                .invocationRateLimitPerSecond(INVOCATION_RATE_LIMIT)
                .invocationEndpoint(ENDPOINT)
                .build();

        // The expected responses
        doReturn(updateResponse, describeResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(ArgumentMatchers.any(), ArgumentMatchers.any());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response.getResourceModel()).isEqualTo(model);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    //@Test
    public void handleRequest_UpdateResourceFailureDifferentName() {
        final ResourceModel prevModel = ResourceModel.builder()
                .name(API_DESTINATION_NAME)
                .build();

        final ResourceModel currModel = ResourceModel.builder()
                .name("API_DESTINATION-2")
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(currModel)
                .previousResourceState(prevModel)
                .build();

        assertThrows(CfnNotUpdatableException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_UpdateResourceFailureDueToArn() {
        final ResourceModel prevModel = ResourceModel.builder()
                .name(API_DESTINATION_NAME)
                .arn(API_DESTINATION_ARN)
                .build();

        final ResourceModel currModel = ResourceModel.builder()
                .name(API_DESTINATION_NAME)
                .arn("API_DESTINATION_ARN_2")
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(currModel)
                .previousResourceState(prevModel)
                .build();

        assertThrows(CfnNotUpdatableException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_ConnectionNotFoundFailure() {
        final ResourceModel model = ResourceModel.builder()
                .name(API_DESTINATION_NAME)
                .connectionArn("connectionNotExist")
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        doThrow(ResourceNotFoundException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()
                );

        assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(eventBridgeClient, atLeastOnce()).serviceName();
    }

    @Test
    public void handleRequest_ConflictFailure() {
        final ResourceModel model = ResourceModel.builder()
                .name(API_DESTINATION_NAME)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        doThrow(ConcurrentModificationException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()
                );

        assertThrows(CfnResourceConflictException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(eventBridgeClient, times(1)).serviceName();
    }

    //@Test
    public void handleRequest_LimitExceededFailure() {
        final ResourceModel model = ResourceModel.builder()
                .name(API_DESTINATION_NAME)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        doThrow(LimitExceededException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()
                );

        assertThrows(CfnServiceLimitExceededException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(eventBridgeClient, times(1)).serviceName();
    }

    //@Test
    public void handleRequest_OtherFailure() {
        final ResourceModel model = ResourceModel.builder()
                .name(API_DESTINATION_NAME)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        doThrow(AwsServiceException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()
                );

        assertThrows(CfnGeneralServiceException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(eventBridgeClient, atLeastOnce()).serviceName();
    }

}
