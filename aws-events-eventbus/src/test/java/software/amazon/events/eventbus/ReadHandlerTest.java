package software.amazon.events.eventbus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.eventbridge.model.DescribeEventBusRequest;
import software.amazon.awssdk.services.eventbridge.model.DescribeEventBusResponse;
import software.amazon.awssdk.services.eventbridge.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.eventbridge.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.eventbridge.model.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;


    private ReadHandler handler;

    @BeforeEach
    public void setup() {
        reset(eventBridgeClient);
        handler = new ReadHandler(eventBridgeClient);
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
    }

    @AfterEach
    public void tear_down() {
        verify(eventBridgeClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(eventBridgeClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final String eventBusName = "eventbus1234";
        final String testKey = "testKey";
        final String testValue = "testValue";

        final ResourceModel model = ResourceModel.builder()
                .name(eventBusName)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final DescribeEventBusResponse describeEventBusResponse = DescribeEventBusResponse.builder()
                .name(eventBusName).build();

        final software.amazon.awssdk.services.eventbridge.model.Tag modelTags =
                software.amazon.awssdk.services.eventbridge.model.Tag.builder()
                        .key(testKey).value(testValue).build();
        final ListTagsForResourceResponse listTagsForResourceResponse = ListTagsForResourceResponse.builder()
                .tags(modelTags).build();

        when(eventBridgeClient.describeEventBus(any(DescribeEventBusRequest.class)))
                .thenReturn(describeEventBusResponse);

        when(eventBridgeClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(listTagsForResourceResponse);

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
    public void handleRequest_DoesNotExist() {
        final String eventBusName = "eventbus1234";

        final ResourceModel model = ResourceModel.builder()
                .name(eventBusName)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();


        when(eventBridgeClient.describeEventBus(any(DescribeEventBusRequest.class)))
                .thenThrow(ResourceNotFoundException.class);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(),  logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }
}
