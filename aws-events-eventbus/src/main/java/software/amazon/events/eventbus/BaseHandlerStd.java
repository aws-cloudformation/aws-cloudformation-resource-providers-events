package software.amazon.events.eventbus;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.*;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;



public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

    public static final String ERROR_CODE_THROTTLING_EXCEPTION = "ThrottlingException";
    private ProxyClient<EventBridgeClient> proxyClient;

    private final EventBridgeClient eventBridgeClient;

    public EventBridgeClient getEventBridgeClient() {
        return eventBridgeClient;
    }

    protected BaseHandlerStd() {
        this(ClientBuilder.getClient());
    }

    protected BaseHandlerStd(EventBridgeClient eventBridgeClient) {
        this.eventBridgeClient = eventBridgeClient;
    }


    @Override
    public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {
        proxyClient = proxy.newProxy(this::getEventBridgeClient);

        return handleRequest(
                proxy,
                request,
                callbackContext != null ? callbackContext : new CallbackContext(),
                proxyClient,
                logger
        );
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<EventBridgeClient> proxyClient,
            final Logger logger);


    public ProgressEvent<ResourceModel, CallbackContext> handleError(
            final EventBridgeRequest request,
            final Exception e, final ProxyClient<EventBridgeClient> proxyClient,
            final ResourceModel resourceModel, final CallbackContext callbackContext) {

        HandlerErrorCode errorCode = HandlerErrorCode.GeneralServiceException;
        if (e instanceof ConcurrentModificationException) {
            errorCode = HandlerErrorCode.ResourceConflict;
        } else if (e instanceof LimitExceededException) {
            errorCode = HandlerErrorCode.ServiceLimitExceeded;
        } else if (e instanceof InvalidEventPatternException) {
            errorCode = HandlerErrorCode.InvalidRequest;
        } else if (e instanceof InternalException) {
            errorCode = HandlerErrorCode.InternalFailure;
        } else if (e instanceof ResourceNotFoundException) {
            errorCode = HandlerErrorCode.NotFound;
        } else if(e instanceof ResourceAlreadyExistsException) {
            errorCode = HandlerErrorCode.AlreadyExists;
        } else if (e instanceof AwsServiceException && ERROR_CODE_THROTTLING_EXCEPTION.equals(getErrorCode(e))) {
            errorCode = HandlerErrorCode.Throttling;
        }

        return ProgressEvent.defaultFailureHandler(e, errorCode);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> propagationDelay(ProgressEvent<ResourceModel, CallbackContext> progress) {
        if (progress.getCallbackContext().isPropagationDelay()) {
            return ProgressEvent.progress(progress.getResourceModel(), progress.getCallbackContext());
        }
        progress.getCallbackContext().setPropagationDelay(true);

        return ProgressEvent.defaultInProgressHandler(progress.getCallbackContext(), 10, progress.getResourceModel());
    }

    public boolean stabilizeCreate(
            final CreateEventBusRequest request,
            final CreateEventBusResponse response, final ProxyClient<EventBridgeClient> proxyClient,
            final ResourceModel resourceModel, final CallbackContext callbackContext, Logger logger) {


        logger.log(String.format("Stabilize CreateHandler started for %s with name %s.",  ResourceModel.TYPE_NAME, resourceModel.getName()));

        try{
            DescribeEventBusRequest describeEventBusRequest = DescribeEventBusRequest.builder()
                    .name(request.name()).build();
            DescribeEventBusResponse describeEventBusResponse =
                    proxyClient.injectCredentialsAndInvokeV2(describeEventBusRequest, proxyClient.client()::describeEventBus);

            if (describeEventBusResponse!= null && describeEventBusResponse.name().equals(request.name())) {
                logger.log(String.format("CreateHandler for %s with name %s is stabilized.",  ResourceModel.TYPE_NAME, resourceModel.getName()));

                return true;
            }
        } catch(Exception e){
            logger.log(String.format("CreateHandler for %s with name %s is not stabilized.",  ResourceModel.TYPE_NAME, resourceModel.getName()));
            if (getErrorCode(e).equals("RequestLimitExceeded")) return false;
            logger.log(getErrorCode(e));

            throw e;
        }
        return false;
    }

    protected String getErrorCode(Exception e) {
        if (e instanceof AwsServiceException &&
                ((AwsServiceException) e).awsErrorDetails() != null) {
            return ((AwsServiceException) e).awsErrorDetails().errorCode();
        }
        return e.getMessage();
    }

}
