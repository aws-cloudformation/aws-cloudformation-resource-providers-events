package software.amazon.events.eventbus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.eventbridge.model.DeleteEventBusRequest;
import software.amazon.awssdk.services.eventbridge.model.DeleteEventBusResponse;
import software.amazon.awssdk.services.eventbridge.model.DescribeEventBusRequest;
import software.amazon.awssdk.services.eventbridge.model.DescribeEventBusResponse;
import software.amazon.awssdk.services.eventbridge.model.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.*;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    private DeleteHandler handler;
    @BeforeEach
    public void setup() {
        reset(eventBridgeClient);
        handler = new DeleteHandler(eventBridgeClient);
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
    }

    @AfterEach
    public void tear_down() {
        verify(eventBridgeClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(eventBridgeClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {

        final String testEventBusName = "eventBus1234";
        final ResourceModel model = ResourceModel.builder()
                .name(testEventBusName).build();

        final DescribeEventBusResponse describeEventBusResponse = DescribeEventBusResponse.builder()
                .name(testEventBusName).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final DeleteEventBusResponse deleteEventBusResponse = DeleteEventBusResponse.builder().build();

        when(eventBridgeClient.describeEventBus(any(DescribeEventBusRequest.class)))
                .thenReturn(describeEventBusResponse);

        when(eventBridgeClient.deleteEventBus(any(DeleteEventBusRequest.class)))
                .thenReturn(deleteEventBusResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_DoesNotExistCFN() {

        final String testEventBusName = "eventBus1234";
        final ResourceModel model = ResourceModel.builder()
                .name(testEventBusName).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .stackId("arn:aws:cloudformation:us-east-1:512376104196:stack/mystack/671aeb40-053f-11ee-8fec-0ea25af41d31")
                .desiredResourceState(model)
                .build();


        when(eventBridgeClient.describeEventBus(any(DescribeEventBusRequest.class)))
                .thenThrow(ResourceNotFoundException.class);


        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }
    @Test
    public void handleRequest_DoesNotExistCCAPI() {

        final String testEventBusName = "eventBus1234";
        final ResourceModel model = ResourceModel.builder()
                .name(testEventBusName).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();


        when(eventBridgeClient.describeEventBus(any(DescribeEventBusRequest.class)))
                .thenThrow(ResourceNotFoundException.class);


        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }
}
