package software.amazon.events.apidestination;

import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.DeleteApiDestinationRequest;
import software.amazon.awssdk.services.eventbridge.model.DeleteApiDestinationResponse;
import software.amazon.cloudformation.proxy.*;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<EventBridgeClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();
        logger.log(String.format("Resource module: %s", model.toString()));

        return ProgressEvent.progress(model, callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-Events-ApiDestination::Delete", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToDeleteRequest)
                                .makeServiceCall(this::deleteResource)
                                .done(this::setResourceModelToNullAndReturnSuccess));
    }

    private ProgressEvent<ResourceModel, CallbackContext> setResourceModelToNullAndReturnSuccess(
            DeleteApiDestinationRequest deleteRequest,
            DeleteApiDestinationResponse deleteResponse,
            ProxyClient<EventBridgeClient> proxyClient,
            ResourceModel resourceModel,
            CallbackContext callbackContext) {
        return ProgressEvent.defaultSuccessHandler(null);
    }

    private DeleteApiDestinationResponse deleteResource(DeleteApiDestinationRequest awsRequest, ProxyClient<EventBridgeClient> proxyClient) {
        DeleteApiDestinationResponse awsResponse;

        awsResponse = translateAwsServiceException(awsRequest.name(),
                () -> proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::deleteApiDestination)
        );
        logger.log(String.format("%s successfully deleted.", ResourceModel.TYPE_NAME));

        return awsResponse;
    }

}
