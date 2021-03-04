package software.amazon.events.connection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.ConnectionAuthResponseParameters;
import software.amazon.awssdk.services.eventbridge.model.ConnectionAuthorizationType;
import software.amazon.awssdk.services.eventbridge.model.ConnectionBasicAuthResponseParameters;
import software.amazon.awssdk.services.eventbridge.model.DescribeConnectionResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static software.amazon.events.connection.TestConstants.CONNECTION_NAME;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<EventBridgeClient> proxyClient;

    @Mock
    EventBridgeClient eventBridgeClient;

    final ReadHandler handler = new ReadHandler();

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
                .name(CONNECTION_NAME)
                .authorizationType(ConnectionAuthorizationType.BASIC.toString())
                .authParameters(TestConstants.authParametersBasicType)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        ConnectionAuthResponseParameters authResponseParameters = ConnectionAuthResponseParameters.builder()
                .basicAuthParameters(
                        ConnectionBasicAuthResponseParameters.builder()
                                .username(TestConstants.USER_NAME)
                                .build())
                .invocationHttpParameters(TestConstants.invocationHttpParameters)
                .build();

        DescribeConnectionResponse connectionName = DescribeConnectionResponse.builder()
                .name(CONNECTION_NAME)
                .authorizationType(ConnectionAuthorizationType.BASIC.toString())
                .authParameters(authResponseParameters)
                .build();
        doReturn(connectionName)
                .when(proxy)
                .injectCredentialsAndInvokeV2(
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()
                );

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(model);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_ConnectionNotExistFailure() {
        final ResourceModel model = ResourceModel.builder()
                .name(CONNECTION_NAME)
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
    }

    @Test
    public void handleRequest_OtherFailure() {
        final ResourceModel model = ResourceModel.builder()
                .name(CONNECTION_NAME)
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
    }
}
