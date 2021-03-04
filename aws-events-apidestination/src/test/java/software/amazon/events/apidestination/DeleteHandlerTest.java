
package software.amazon.events.apidestination;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.DeleteApiDestinationRequest;
import software.amazon.awssdk.services.eventbridge.model.DeleteApiDestinationResponse;
import software.amazon.awssdk.services.eventbridge.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.events.apidestination.TestConstants.API_DESTINATION_NAME;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<EventBridgeClient> proxyClient;

    @Mock
    EventBridgeClient eventBridgeClient;

    final DeleteHandler handler = new DeleteHandler();

    @BeforeEach
    public void setup() {
        proxy = Mockito.spy(new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis()));
        eventBridgeClient = mock(EventBridgeClient.class);
        proxyClient = MOCK_PROXY(proxy, eventBridgeClient);
    }

    @AfterEach
    public void tear_down() {
        verify(eventBridgeClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(eventBridgeClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceModel model = ResourceModel.builder()
                .name(API_DESTINATION_NAME)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(eventBridgeClient.deleteApiDestination(any(DeleteApiDestinationRequest.class)))
                .thenReturn(DeleteApiDestinationResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(eventBridgeClient, times(1)).deleteApiDestination(any(DeleteApiDestinationRequest.class));
    }

    @Test
    public void handleRequest_ApiDestinationNotExistFailure() {
        final ResourceModel model = ResourceModel.builder()
                .name(API_DESTINATION_NAME)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(eventBridgeClient.deleteApiDestination(any(DeleteApiDestinationRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(eventBridgeClient, times(1)).deleteApiDestination(any(DeleteApiDestinationRequest.class));
    }

    @Test
    public void handleRequest_OtherFailure() {
        final ResourceModel model = ResourceModel.builder()
                .name(API_DESTINATION_NAME)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(eventBridgeClient.deleteApiDestination(any(DeleteApiDestinationRequest.class)))
                .thenThrow(AwsServiceException.builder().build());

        assertThrows(CfnGeneralServiceException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(eventBridgeClient, times(1)).deleteApiDestination(any(DeleteApiDestinationRequest.class));
    }
}
