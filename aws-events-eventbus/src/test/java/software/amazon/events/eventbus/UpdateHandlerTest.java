package software.amazon.events.eventbus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.eventbridge.model.DescribeEventBusRequest;
import software.amazon.awssdk.services.eventbridge.model.DescribeEventBusResponse;
import software.amazon.awssdk.services.eventbridge.model.PutPermissionRequest;
import software.amazon.awssdk.services.eventbridge.model.PutPermissionResponse;
import software.amazon.awssdk.services.eventbridge.model.TagResourceRequest;
import software.amazon.awssdk.services.eventbridge.model.TagResourceResponse;
import software.amazon.awssdk.services.eventbridge.model.UntagResourceRequest;
import software.amazon.awssdk.services.eventbridge.model.UntagResourceResponse;
import software.amazon.cloudformation.Action;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;


   private UpdateHandler handler;
    @BeforeEach
    public void setup() {
        reset(eventBridgeClient);
        handler = new UpdateHandler(eventBridgeClient);
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
    }

    @AfterEach
    public void tear_down() {
        verify(eventBridgeClient, atLeastOnce()).serviceName();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final String testEventBusName = "testEventBus1234";
        final String testArn = "arn";
        final String testPolicy1 = "testPolicy1"; // before update
        final String testPolicy2 = "testPolicy2"; // after update
        final String testKey1 = "testKey1";
        final String testValue1 = "testValue1";
        final String testValue1update = "testValue1update"; // to be updated
        final String testKey2 = "testKey2";
        final String testValue2 = "testValue2"; // to be removed
        final String testKey3 = "testKey3";
        final String testValue3 = "testValue3"; // to be added
        List<Tag> prevTags = new ArrayList<>();
        prevTags.add(Tag.builder().key(testKey1).value(testValue1).build());
        prevTags.add(Tag.builder().key(testKey2).value(testValue2).build());

        List<Tag> currTags = new ArrayList<>();
        currTags.add(Tag.builder().key(testKey1).value(testValue1update).build());
        currTags.add(Tag.builder().key(testKey3).value(testValue3).build());

        final ResourceModel model = ResourceModel.builder()
                .name(testEventBusName)
                .arn(testArn)
                .policy(testPolicy2)
                .tags(currTags)
                .build();
        final ResourceModel prevModel = ResourceModel.builder()
                .name(testEventBusName)
                .arn(testArn)
                .policy(testPolicy1)
                .tags(prevTags)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .previousResourceState(prevModel)
                .build();

        final DescribeEventBusResponse describeEventBusResponse1 = DescribeEventBusResponse.builder()
                .name(testEventBusName)
                .policy(testPolicy1)
                .arn(testArn)
                .build();

        final UntagResourceResponse untagResourceResponse = UntagResourceResponse.builder()
                .build();

        final TagResourceResponse tagResourceResponse = TagResourceResponse.builder()
                .build();

        final PutPermissionResponse putPermissionResponse = PutPermissionResponse.builder().build();

        when(eventBridgeClient.describeEventBus(any(DescribeEventBusRequest.class)))
                .thenReturn(describeEventBusResponse1);

        when(eventBridgeClient.untagResource(any(UntagResourceRequest.class)))
                .thenReturn(untagResourceResponse);

        when(eventBridgeClient.tagResource(any(TagResourceRequest.class)))
                .thenReturn(tagResourceResponse);

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
    public void handleThrottleWithReplaysDescribeEventBus_assert() throws Exception {

        when(eventBridgeClient.serviceName()).thenReturn("eventBridgeClient");

        final TagResourceResponse tagResourceResponse = TagResourceResponse.builder()
                .build();

        final PutPermissionResponse putPermissionResponse = PutPermissionResponse.builder()
                .build();

        final ArgumentMatcher<DescribeEventBusRequest> matchEventBusDescribe = (request) ->
                request.name().equals(TEST_EVENT_BUS_NAME) &&
                        checkCredentialsOverride(request.overrideConfiguration());

        final ArgumentMatcher<TagResourceRequest> matchTagResource = (currentRequest) ->
                currentRequest.tags().size() > 0 &&
                        checkCredentialsOverride(currentRequest.overrideConfiguration());

        final ArgumentMatcher<PutPermissionRequest> matchPutPermission = (currentRequest) ->
                currentRequest.policy().equals(TEST_POLICY) &&
                        checkCredentialsOverride(currentRequest.overrideConfiguration());

        final AwsServiceException throttleException = createThrottleException();

        when(eventBridgeClient.describeEventBus(argThat(matchEventBusDescribe)))
                .thenThrow(throttleException)
                .thenReturn(buildDescribeEventBusResponse());

        when(eventBridgeClient.tagResource(argThat(matchTagResource)))
                .thenReturn(tagResourceResponse);

        when(eventBridgeClient.putPermission(argThat(matchPutPermission)))
                .thenReturn(putPermissionResponse);


        final ResourceModel resourceModel = buildResourceModelWithAllProperties();

        final ProgressEvent<ResourceModel, CallbackContext> result = makeCall(
                resourceModel, null, Action.UPDATE, false
        );

        assertThat(result).isNotNull();

        assertThat(result.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);

        final ProgressEvent<ResourceModel, CallbackContext> nextAttempt = makeCall(
                resourceModel, null, result.getCallbackContext(), Action.UPDATE, false
        );


        assertThat(nextAttempt).isNotNull();
        assertThat(nextAttempt.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(nextAttempt.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(nextAttempt.getResourceModel()).isNotNull();
        final ResourceModel nextAttemptResult = nextAttempt.getResourceModel();
        assertThat(nextAttemptResult.getArn()).isEqualTo(TEST_ARN);
        assertThat(nextAttemptResult.getName()).isEqualTo(TEST_EVENT_BUS_NAME);
        assertThat(nextAttemptResult.getPolicy()).isEqualTo(TEST_POLICY);


        verify(eventBridgeClient, times(2)).describeEventBus(argThat(matchEventBusDescribe));
        verify(eventBridgeClient, times(1)).tagResource(argThat(matchTagResource));
        verify(eventBridgeClient, times(1)).putPermission(argThat(matchPutPermission));
    }

    @Test
    public void handleThrottleWithReplaysUpdateTags_assert() throws Exception {

        final TagResourceResponse tagResourceResponse = TagResourceResponse.builder()
                .build();

        final PutPermissionResponse putPermissionResponse = PutPermissionResponse.builder()
                .build();

        when(eventBridgeClient.serviceName()).thenReturn("eventBridgeClient");

        final ArgumentMatcher<DescribeEventBusRequest> matchEventBusDescribe = (currentRequest) ->
                currentRequest.name().equals(TEST_EVENT_BUS_NAME) &&
                        checkCredentialsOverride(currentRequest.overrideConfiguration());

        final ArgumentMatcher<TagResourceRequest> matchTagResource = (currentRequest) ->
                currentRequest.tags().size() > 0 &&
                        checkCredentialsOverride(currentRequest.overrideConfiguration());

        final ArgumentMatcher<PutPermissionRequest> matchPutPermission = (currentRequest) ->
                currentRequest.policy().equals(TEST_POLICY) &&
                        checkCredentialsOverride(currentRequest.overrideConfiguration());

        final AwsServiceException throttleException = createThrottleException();
        when(eventBridgeClient.describeEventBus(argThat(matchEventBusDescribe)))
                .thenReturn(buildDescribeEventBusResponse());

        when(eventBridgeClient.tagResource(argThat(matchTagResource)))
                .thenThrow(throttleException)
                .thenReturn(tagResourceResponse);

        when(eventBridgeClient.putPermission(argThat(matchPutPermission)))
                .thenReturn(putPermissionResponse);


        final ResourceModel resourceModel = buildResourceModelWithAllProperties();

        final ProgressEvent<ResourceModel, CallbackContext> result = makeCall(
                resourceModel, null, Action.UPDATE, false
        );

        assertThat(result).isNotNull();

        assertThat(result.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);

        final ProgressEvent<ResourceModel, CallbackContext> nextAttempt = makeCall(
                resourceModel, null, result.getCallbackContext(), Action.UPDATE, false
        );


        assertThat(nextAttempt).isNotNull();
        assertThat(nextAttempt.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(nextAttempt.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(nextAttempt.getResourceModel()).isNotNull();
        final ResourceModel nextAttemptResult = nextAttempt.getResourceModel();
        assertThat(nextAttemptResult.getArn()).isEqualTo(TEST_ARN);
        assertThat(nextAttemptResult.getName()).isEqualTo(TEST_EVENT_BUS_NAME);
        assertThat(nextAttemptResult.getPolicy()).isEqualTo(TEST_POLICY);
        assertThat(nextAttemptResult.getTags().size()).isEqualTo(2);


        verify(eventBridgeClient, times(1)).describeEventBus(argThat(matchEventBusDescribe));
        verify(eventBridgeClient, times(2)).tagResource(argThat(matchTagResource));
        verify(eventBridgeClient, times(1)).putPermission(argThat(matchPutPermission));
    }

    @Test
    public void handleThrottleWithReplaysUpdatePolicy_assert() throws Exception {


        final TagResourceResponse tagResourceResponse = TagResourceResponse.builder()
                .build();

        final PutPermissionResponse putPermissionResponse = PutPermissionResponse.builder()
                .build();

        when(eventBridgeClient.serviceName()).thenReturn("eventBridgeClient");

        final ArgumentMatcher<DescribeEventBusRequest> matchEventBusDescribe = (currentRequest) ->
                currentRequest.name().equals(TEST_EVENT_BUS_NAME) &&
                        checkCredentialsOverride(currentRequest.overrideConfiguration());

        final ArgumentMatcher<TagResourceRequest> matchTagResource = (currentRequest) ->
                currentRequest.tags().size() > 0 &&
                        checkCredentialsOverride(currentRequest.overrideConfiguration());

        final ArgumentMatcher<PutPermissionRequest> matchPutPermission = (currentRequest) ->
                currentRequest.policy().equals(TEST_POLICY) &&
                        checkCredentialsOverride(currentRequest.overrideConfiguration());

        final AwsServiceException throttleException = createThrottleException();
        when(eventBridgeClient.describeEventBus(argThat(matchEventBusDescribe)))
                .thenReturn(buildDescribeEventBusResponse());

        when(eventBridgeClient.tagResource(argThat(matchTagResource)))
                .thenReturn(tagResourceResponse);

        when(eventBridgeClient.putPermission(argThat(matchPutPermission)))
                .thenThrow(throttleException)
                .thenReturn(putPermissionResponse);


        final ResourceModel resourceModel = buildResourceModelWithAllProperties();

        final ProgressEvent<ResourceModel, CallbackContext> result = makeCall(
                resourceModel, null, Action.UPDATE, false
        );

        assertThat(result).isNotNull();

        assertThat(result.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);

        final ProgressEvent<ResourceModel, CallbackContext> nextAttempt = makeCall(
                resourceModel, null, result.getCallbackContext(), Action.UPDATE, false
        );


        assertThat(nextAttempt).isNotNull();
        assertThat(nextAttempt.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(nextAttempt.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(nextAttempt.getResourceModel()).isNotNull();
        final ResourceModel nextAttemptResult = nextAttempt.getResourceModel();
        assertThat(nextAttemptResult.getArn()).isEqualTo(TEST_ARN);
        assertThat(nextAttemptResult.getName()).isEqualTo(TEST_EVENT_BUS_NAME);
        assertThat(nextAttemptResult.getPolicy()).isEqualTo(TEST_POLICY);
        assertThat(nextAttemptResult.getTags().size()).isEqualTo(2);


        verify(eventBridgeClient, times(1)).describeEventBus(argThat(matchEventBusDescribe));
        verify(eventBridgeClient, times(1)).tagResource(argThat(matchTagResource));
        verify(eventBridgeClient, times(2)).putPermission(argThat(matchPutPermission));
    }
}
