package software.amazon.events.connection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.ConcurrentModificationException;
import software.amazon.awssdk.services.eventbridge.model.ConnectionApiKeyAuthResponseParameters;
import software.amazon.awssdk.services.eventbridge.model.ConnectionAuthResponseParameters;
import software.amazon.awssdk.services.eventbridge.model.ConnectionAuthorizationType;
import software.amazon.awssdk.services.eventbridge.model.ConnectionBasicAuthResponseParameters;
import software.amazon.awssdk.services.eventbridge.model.ConnectionOAuthResponseParameters;
import software.amazon.awssdk.services.eventbridge.model.ConnectionState;
import software.amazon.awssdk.services.eventbridge.model.DescribeConnectionRequest;
import software.amazon.awssdk.services.eventbridge.model.DescribeConnectionResponse;
import software.amazon.awssdk.services.eventbridge.model.LimitExceededException;
import software.amazon.awssdk.services.eventbridge.model.ResourceNotFoundException;
import software.amazon.awssdk.services.eventbridge.model.UpdateConnectionRequest;
import software.amazon.awssdk.services.eventbridge.model.UpdateConnectionResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import static software.amazon.events.connection.TestConstants.CONNECTION_NAME;
import static software.amazon.events.connection.TestConstants.CREATION_TIME;
import static software.amazon.events.connection.TestConstants.LAST_MODIFIED_TIME;
import static software.amazon.events.connection.TestConstants.USER_NAME;
import static software.amazon.events.connection.TestConstants.API_KEY_NAME;

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

    @Test
    public void handleRequest_SimpleSuccessWithBasicAuthType() {
        final ResourceModel model = ResourceModel.builder()
                .name(CONNECTION_NAME)
                .authorizationType(ConnectionAuthorizationType.BASIC.toString())
                .authParameters(TestConstants.authParametersBasicType)
                .build();


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        // The expected responses
        UpdateConnectionResponse updateResponse = UpdateConnectionResponse.builder()
                .connectionState(ConnectionState.AUTHORIZED)
                .creationTime(CREATION_TIME)
                .lastModifiedTime(LAST_MODIFIED_TIME)
                .build();


        // construct Basic response parameters
        ConnectionBasicAuthResponseParameters params = ConnectionBasicAuthResponseParameters.builder().username(USER_NAME).build();
        ConnectionAuthResponseParameters authResponseParameters = ConnectionAuthResponseParameters.builder()
                .basicAuthParameters(params)
                .invocationHttpParameters(TestConstants.invocationHttpParameters)
                .build();

        DescribeConnectionResponse describeResponse = DescribeConnectionResponse.builder()
                .name(CONNECTION_NAME)
                .connectionState(ConnectionState.AUTHORIZED)
                .authorizationType(ConnectionAuthorizationType.BASIC)
                .authParameters(authResponseParameters)
                .build();


        when(eventBridgeClient.updateConnection(any(UpdateConnectionRequest.class)))
                .thenReturn(updateResponse);

        when(eventBridgeClient.describeConnection(any(DescribeConnectionRequest.class)))
                .thenReturn(describeResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);


        verifyParameters(response, model, request);
        verify(eventBridgeClient, times(1)).updateConnection(any(UpdateConnectionRequest.class));
        // 1 for stabilization and 1 from for ReadHandler invocation upon completion
        verify(eventBridgeClient, times(2)).describeConnection(any(DescribeConnectionRequest.class));

    }

    @Test
    public void handleRequest_SimpleSuccessWithApiKeyAuthType() {
        final ResourceModel model = ResourceModel.builder()
                .name(CONNECTION_NAME)
                .authorizationType(ConnectionAuthorizationType.API_KEY.toString())
                .authParameters(TestConstants.authParametersApiKeyType)
                .build();


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        // The expected responses
        UpdateConnectionResponse updateResponse = UpdateConnectionResponse.builder()
                .connectionState(ConnectionState.AUTHORIZED)
                .creationTime(CREATION_TIME)
                .lastModifiedTime(LAST_MODIFIED_TIME)
                .build();


        // construct ApiKey response parameters
        ConnectionApiKeyAuthResponseParameters params = ConnectionApiKeyAuthResponseParameters.builder().apiKeyName(API_KEY_NAME).build();
        ConnectionAuthResponseParameters authResponseParameters = ConnectionAuthResponseParameters.builder().apiKeyAuthParameters(params).build();

        DescribeConnectionResponse describeResponse = DescribeConnectionResponse.builder()
                .name(CONNECTION_NAME)
                .connectionState(ConnectionState.AUTHORIZED)
                .authorizationType(ConnectionAuthorizationType.API_KEY)
                .authParameters(authResponseParameters)
                .build();


        when(eventBridgeClient.updateConnection(any(UpdateConnectionRequest.class)))
                .thenReturn(updateResponse);

        when(eventBridgeClient.describeConnection(any(DescribeConnectionRequest.class)))
                .thenReturn(describeResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);


        verifyParameters(response, model, request);
        verify(eventBridgeClient, times(1)).updateConnection(any(UpdateConnectionRequest.class));
        // 1 for stabilization and 1 from for ReadHandler invocation upon completion
        verify(eventBridgeClient, times(2)).describeConnection(any(DescribeConnectionRequest.class));

    }

    @Test
    public void handleRequest_SuccessWithStabilizationWithOauthType() {
        final ResourceModel model = ResourceModel.builder()
                .name(CONNECTION_NAME)
                .authorizationType(ConnectionAuthorizationType.OAUTH_CLIENT_CREDENTIALS.toString())
                .authParameters(TestConstants.authParametersOAuthType)
                .build();


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("id")
                .clientRequestToken("token")
                .build();

        // Expected responses
        UpdateConnectionResponse updateResponse = UpdateConnectionResponse.builder()
                .connectionState(ConnectionState.AUTHORIZED)
                .creationTime(CREATION_TIME)
                .lastModifiedTime(LAST_MODIFIED_TIME)
                .build();


        // Describe responses for stabilization
        DescribeConnectionResponse describeResponse1 = DescribeConnectionResponse.builder()
                .connectionState(ConnectionState.AUTHORIZING)
                .build();

        DescribeConnectionResponse describeResponse2 = DescribeConnectionResponse.builder()
                .connectionState(ConnectionState.UPDATING)
                .build();

        DescribeConnectionResponse describeResponse3 = DescribeConnectionResponse.builder()
                .name(CONNECTION_NAME)
                .connectionState(ConnectionState.AUTHORIZING)
                .build();


        // construct OAuth response parameters
        ConnectionOAuthResponseParameters params = ConnectionOAuthResponseParameters.builder()
                .oAuthHttpParameters(TestConstants.oAuthHttpParameters)
                .clientParameters(TestConstants.sdkClientResponseParameters)
                .build();

        ConnectionAuthResponseParameters authResponseParameters = ConnectionAuthResponseParameters.builder().oAuthParameters(params).build();

        DescribeConnectionResponse describeResponse4 = DescribeConnectionResponse.builder()
                .name(CONNECTION_NAME)
                .connectionState(ConnectionState.AUTHORIZED)
                .authorizationType(ConnectionAuthorizationType.OAUTH_CLIENT_CREDENTIALS)
                .authParameters(authResponseParameters)
                .build();


        when(eventBridgeClient.updateConnection(any(UpdateConnectionRequest.class)))
                .thenReturn(updateResponse);

        when(eventBridgeClient.describeConnection(any(DescribeConnectionRequest.class)))
                .thenReturn(describeResponse1)
                .thenReturn(describeResponse2)
                .thenReturn(describeResponse3)
                .thenReturn(describeResponse4);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verifyParameters(response, model, request);
        verify(eventBridgeClient, times(1)).updateConnection(any(UpdateConnectionRequest.class));
        // 4 for stabilization and 1 from for ReadHandler invocation upon completion
        verify(eventBridgeClient, times(5)).describeConnection(any(DescribeConnectionRequest.class));

    }

    @Test
    public void handleRequest_FailureWithStabilization() {
        final ResourceModel model = ResourceModel.builder()
                .name(CONNECTION_NAME)
                .authorizationType(ConnectionAuthorizationType.BASIC.toString())
                .authParameters(TestConstants.authParametersBasicType)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("id")
                .clientRequestToken("token")
                .build();

        // Describe responses for stabilization
        DescribeConnectionResponse describeResponse1 = DescribeConnectionResponse.builder()
                .connectionState(ConnectionState.AUTHORIZING)
                .build();

        DescribeConnectionResponse describeResponse2 = DescribeConnectionResponse.builder()
                .connectionState(ConnectionState.DEAUTHORIZED)
                .build();


        when(eventBridgeClient.updateConnection(any(UpdateConnectionRequest.class)))
                .thenReturn(UpdateConnectionResponse.builder().build());

        when(eventBridgeClient.describeConnection(any(DescribeConnectionRequest.class)))
                .thenReturn(describeResponse1)
                .thenReturn(describeResponse2);

        assertThrows(CfnGeneralServiceException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(eventBridgeClient, times(1)).updateConnection(any(UpdateConnectionRequest.class));
        verify(eventBridgeClient, times(2)).describeConnection(any(DescribeConnectionRequest.class));

    }

    @Test
    public void handleRequest_InvalidRequest_BasicTypeParameter_NotProvided() {
        final ResourceModel model = ResourceModel.builder()
                .name(CONNECTION_NAME)
                .authorizationType(ConnectionAuthorizationType.BASIC.toString())
                .authParameters(TestConstants.authParametersApiKeyType)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(eventBridgeClient, never()).serviceName();
    }

    @Test
    public void handleRequest_InvalidRequest_ApiKeyTypeParameter_NotProvided() {
        final ResourceModel model = ResourceModel.builder()
                .name(CONNECTION_NAME)
                .authorizationType(ConnectionAuthorizationType.API_KEY.toString())
                .authParameters(TestConstants.authParametersOAuthType)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(eventBridgeClient, never()).serviceName();
    }

    @Test
    public void handleRequest_InvalidRequest_OauthClientCredentials_NotProvided() {
        final ResourceModel model = ResourceModel.builder()
                .name(CONNECTION_NAME)
                .authorizationType(ConnectionAuthorizationType.API_KEY.toString())
                .authParameters(TestConstants.authParametersBasicType)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(eventBridgeClient, never()).serviceName();
    }


    @Test
    public void handleRequest_ConcurrentModificationFailure() {
        final ResourceModel model = ResourceModel.builder()
                .name(CONNECTION_NAME)
                .authorizationType(ConnectionAuthorizationType.BASIC.toString())
                .authParameters(TestConstants.authParametersBasicType)
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

    @Test
    public void handleRequest_ConnectionNotFoundFailure() {
        final ResourceModel model = ResourceModel.builder()
                .name(CONNECTION_NAME)
                .authorizationType(ConnectionAuthorizationType.BASIC.toString())
                .authParameters(TestConstants.authParametersBasicType)
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
        verify(eventBridgeClient, times(1)).serviceName();
    }

    @Test
    public void handleRequest_LimitExceededFailure() {
        final ResourceModel model = ResourceModel.builder()
                .name(CONNECTION_NAME)
                .authorizationType(ConnectionAuthorizationType.BASIC.toString())
                .authParameters(TestConstants.authParametersBasicType)
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

    @Test
    public void handleRequest_OtherFailure() {
        final ResourceModel model = ResourceModel.builder()
                .name(CONNECTION_NAME)
                .authorizationType(ConnectionAuthorizationType.BASIC.toString())
                .authParameters(TestConstants.authParametersBasicType)
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
        verify(eventBridgeClient, times(1)).serviceName();
    }


    private void verifyParameters(ProgressEvent<ResourceModel, CallbackContext> response, ResourceModel model, ResourceHandlerRequest<ResourceModel> request) {
        assertThat(response.getResourceModel()).isEqualTo(model);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModel().getDescription()).isEqualTo(model.getDescription());
        assertThat(response.getResourceModel().getAuthorizationType()).isEqualTo(model.getAuthorizationType());
        assertThat(response.getResourceModel().getName()).isEqualTo(model.getName());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

}
