package software.amazon.events.apidestination;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.CreateApiDestinationRequest;
import software.amazon.awssdk.services.eventbridge.model.CreateApiDestinationResponse;
import software.amazon.awssdk.services.eventbridge.model.DescribeApiDestinationRequest;
import software.amazon.awssdk.services.eventbridge.model.DescribeApiDestinationResponse;
import software.amazon.awssdk.services.eventbridge.model.LimitExceededException;
import software.amazon.awssdk.services.eventbridge.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.eventbridge.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import static software.amazon.events.apidestination.TestConstants.API_DESTINATION_NAME;
import static software.amazon.events.apidestination.TestConstants.API_DESTINATION_ARN;
import static software.amazon.events.apidestination.TestConstants.CONNECTION_ARN;
import static software.amazon.events.apidestination.TestConstants.NOT_EXISTING_CONNECTION_ARN;
import static software.amazon.events.apidestination.TestConstants.ENDPOINT;
import static software.amazon.events.apidestination.TestConstants.INVOCATION_RATE_LIMIT;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<EventBridgeClient> proxyClient;

    @Mock
    EventBridgeClient eventBridgeClient;

    final CreateHandler handler = new CreateHandler();

    @BeforeEach
    public void setup() {
        // Creates a Mockito wrapper around this object, allowing us to replace the output of the invoke() method.
        proxy = Mockito.spy(new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis()));
        eventBridgeClient = mock(EventBridgeClient.class);
        proxyClient = MOCK_PROXY(proxy, eventBridgeClient);
    }

    @Test
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
        CreateApiDestinationResponse createResponse = CreateApiDestinationResponse.builder()
                .apiDestinationArn(API_DESTINATION_ARN)
                .apiDestinationState("ENABLED")
                .build();

        DescribeApiDestinationResponse describeResponse = DescribeApiDestinationResponse.builder()
                .name(API_DESTINATION_NAME)
                .connectionArn(CONNECTION_ARN)
                .httpMethod("GET")
                .invocationRateLimitPerSecond(INVOCATION_RATE_LIMIT)
                .invocationEndpoint(ENDPOINT)
                .build();

        when(eventBridgeClient.createApiDestination(any(CreateApiDestinationRequest.class)))
                .thenReturn(createResponse);

        when(eventBridgeClient.describeApiDestination(any(DescribeApiDestinationRequest.class)))
                .thenReturn(describeResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response.getResourceModel()).isEqualTo(model);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(eventBridgeClient, times(1)).createApiDestination(any(CreateApiDestinationRequest.class));
        // Handler invokes ReadHandler upon completion, which makes additional Describe call
        verify(eventBridgeClient, times(1)).describeApiDestination(any(DescribeApiDestinationRequest.class));

    }

    @Test
    public void handleRequest_SimpleSuccessWithoutName() {

        final ResourceModel model = ResourceModel.builder()
                .connectionArn(CONNECTION_ARN)
                .httpMethod("GET")
                .invocationRateLimitPerSecond(INVOCATION_RATE_LIMIT)
                .invocationEndpoint(ENDPOINT)
                .build();


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("id")
                .clientRequestToken("token")
                .build();

        // The expected responses
        CreateApiDestinationResponse createResponse = CreateApiDestinationResponse.builder()
                .apiDestinationArn(API_DESTINATION_ARN)
                .apiDestinationState("ENABLED")
                .build();

        DescribeApiDestinationResponse describeResponse = DescribeApiDestinationResponse.builder()
                .name("id-kVW2fZtz3CVH")
                .connectionArn(CONNECTION_ARN)
                .httpMethod("GET")
                .invocationRateLimitPerSecond(INVOCATION_RATE_LIMIT)
                .invocationEndpoint(ENDPOINT)
                .build();

        when(eventBridgeClient.createApiDestination(any(CreateApiDestinationRequest.class)))
                .thenReturn(createResponse);

        when(eventBridgeClient.describeApiDestination(any(DescribeApiDestinationRequest.class)))
                .thenReturn(describeResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response.getResourceModel()).isEqualTo(model);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(eventBridgeClient, times(1)).createApiDestination(any(CreateApiDestinationRequest.class));
        verify(eventBridgeClient, times(1)).describeApiDestination(any(DescribeApiDestinationRequest.class));
    }


    @Test
    public void handleRequest_InvalidRequest_ArnProvided() {
        final ResourceModel model = ResourceModel.builder()
                .name(API_DESTINATION_NAME)
                .arn("ARN is a readOnly property")
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(eventBridgeClient, never()).serviceName();
    }

    @Test
    public void handleRequest_AlreadyExistsFailure() {
        final ResourceModel model = ResourceModel.builder()
                .name("Already-Existing-ApiDestination")
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        doThrow(ResourceAlreadyExistsException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()
                );

        assertThrows(CfnAlreadyExistsException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(eventBridgeClient, times(1)).serviceName();
    }

    @Test
    public void handleRequest_ConnectionNotFoundFailure() {
        final ResourceModel model = ResourceModel.builder()
                .name(API_DESTINATION_NAME)
                .connectionArn(NOT_EXISTING_CONNECTION_ARN)
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
                .name(API_DESTINATION_NAME)
                .connectionArn(NOT_EXISTING_CONNECTION_ARN)
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
    public void handleRequest_GenericValidationError_CommunicatesErrorDetail() {
        final ResourceModel model = ResourceModel.builder()
                .name(API_DESTINATION_NAME)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final AwsServiceException genericValidationError = AwsServiceException.builder().awsErrorDetails(
                AwsErrorDetails.builder()
                        .errorMessage("Failed to create resource. Parameter foo is not valid.")
                        .errorCode(BaseHandlerStd.VALIDATION_ERROR_CODE)
                        .build()
        ).build();

        doThrow(genericValidationError)
                .when(proxy)
                .injectCredentialsAndInvokeV2(
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()
                );

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger))
                .isInstanceOf(CfnInvalidRequestException.class)
                .hasMessageContaining(genericValidationError.awsErrorDetails().errorMessage());
        verify(eventBridgeClient, times(1)).serviceName();
    }

    @Test
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
        verify(eventBridgeClient, times(1)).serviceName();
    }

}
