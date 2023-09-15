package software.amazon.events.eventbus;

import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.DeleteEventBusResponse;
import software.amazon.awssdk.services.eventbridge.model.DescribeEventBusResponse;
import software.amazon.awssdk.services.eventbridge.model.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import com.google.common.annotations.VisibleForTesting;


public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    public DeleteHandler() {
        super();
    }

    @VisibleForTesting
    protected DeleteHandler(EventBridgeClient eventBridgeClient) {
        super(eventBridgeClient);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<EventBridgeClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        logger.log(String.format("[StackId: %s, ClientRequestToken: %s] Calling Delete EventBus",
                request.getStackId(), request.getClientRequestToken()));

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)

                // STEP 1 [check if resource already exists with checkExistence]
                .checkExistence(request, progress -> checkIfEventBusExists(proxy, proxyClient, progress))
                // STEP 2.0 [delete/stabilize progress chain - required for resource deletion]
                .then(progress -> deleteEventBus(proxy, proxyClient, progress))
                // STEP 3  return the successful progress event without resource model
                .then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }


    protected ProgressEvent<ResourceModel, CallbackContext> checkIfEventBusExists(AmazonWebServicesClientProxy proxy, ProxyClient<EventBridgeClient> proxyClient, ProgressEvent<ResourceModel, CallbackContext> progress) {

        ResourceModel model = progress.getResourceModel();

        return proxy.initiate("AWS-Events-EventBus::Delete::PreDeletionCheck", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> {
                    DescribeEventBusResponse awsResponse = null;
                    logger.log(String.format("Get EventBus with name %s.", model.getName()));
                    awsResponse = client.injectCredentialsAndInvokeV2(awsRequest, client.client()::describeEventBus);
                    return awsResponse;
                })
                .handleError((awsRequest, exception, client, currentModel, context) -> {
                    // Contract Test: test_delete_delete requires a 2nd delete fails, but
                    // hard fail if the resource is not found and API request comes from CFN or CC API
                    // (self-service implementation does not fail for Not Found)
                    if (exception instanceof ResourceNotFoundException) {
                        logger.log(String.format("DeleteHandler: %s not found but okay to continue",
                                ResourceModel.TYPE_NAME));
                        return ProgressEvent.failed(currentModel, context, HandlerErrorCode.NotFound, String.format("EventBus with name %s not found.", model.getName()));
                    }
                    throw exception;
                })
                .progress();
    }

    private ProgressEvent<ResourceModel, CallbackContext> deleteEventBus(AmazonWebServicesClientProxy proxy, ProxyClient<EventBridgeClient> proxyClient, ProgressEvent<ResourceModel, CallbackContext> progress) {
        return  proxy.initiate("AWS-Events-EventBus::Delete", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToDeleteRequest)
                .makeServiceCall((awsRequest, client) -> {
                    DeleteEventBusResponse awsResponse = client.injectCredentialsAndInvokeV2(awsRequest, client.client()::deleteEventBus);

                    logger.log(String.format("%s with name %s successfully deleted.", ResourceModel.TYPE_NAME, progress.getResourceModel().getName()));
                    return awsResponse;
                })
                .handleError(this::handleError)
                .progress();
    }

}
