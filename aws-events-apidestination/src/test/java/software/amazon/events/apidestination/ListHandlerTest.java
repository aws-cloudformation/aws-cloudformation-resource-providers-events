package software.amazon.events.apidestination;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.ApiDestination;
import software.amazon.awssdk.services.eventbridge.model.ListApiDestinationsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<EventBridgeClient> proxyClient;

    @Mock
    private EventBridgeClient eventBridgeClient;


    @BeforeEach
    public void setup() {
        // Creates a Mockito wrapper around this object, allowing us to replace the output of the invoke() method.
        proxy = Mockito.spy(new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis()));
        eventBridgeClient = mock(EventBridgeClient.class);
        proxyClient = MOCK_PROXY(proxy, eventBridgeClient);
    }

    @AfterEach
    public void tear_down() {
        verifyNoMoreInteractions(eventBridgeClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ListHandler handler = new ListHandler();

        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final List<ApiDestination> apiDestinations = new ArrayList<>();
        apiDestinations.add(ApiDestination.builder()
                .name("destination-1")
                .build());
        apiDestinations.add(ApiDestination.builder()
                .name("destination-2")
                .build());

        final ListApiDestinationsResponse listResponse = ListApiDestinationsResponse.builder()
                .apiDestinations(apiDestinations)
                .nextToken("next-token")
                .build();

        doReturn(listResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()
                );

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // Assert that the returned resource models are correct
        assertThat(response.getResourceModels().get(0).getName()).isEqualTo("destination-1");
        assertThat(response.getResourceModels().get(1).getName()).isEqualTo("destination-2");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
