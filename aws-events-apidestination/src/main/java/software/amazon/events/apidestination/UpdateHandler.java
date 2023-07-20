package software.amazon.events.apidestination;

import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.UpdateApiDestinationRequest;
import software.amazon.awssdk.services.eventbridge.model.UpdateApiDestinationResponse;
import software.amazon.cloudformation.exceptions.CfnNotUpdatableException;
import software.amazon.cloudformation.proxy.*;

import java.util.Optional;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<EventBridgeClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();
        logger.log(String.format("Resource module: %s", model.toString()));

        verifyNonUpdatableFields(model, request.getPreviousResourceState());

        return ProgressEvent.progress(model, callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-Events-ApiDestination::Update", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToUpdateRequest)
                                .makeServiceCall(this::updateResource)
                                .progress()
                )
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private UpdateApiDestinationResponse updateResource(UpdateApiDestinationRequest awsRequest, ProxyClient<EventBridgeClient> proxyClient) {
        UpdateApiDestinationResponse awsResponse;

        awsResponse = translateAwsServiceException(awsRequest.name(),
                () -> proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::updateApiDestination)
        );
        logger.log(String.format("%s successfully updated.", ResourceModel.TYPE_NAME));

        return awsResponse;
    }

    private void verifyNonUpdatableFields(ResourceModel currModel, ResourceModel prevModel) {
        if (prevModel != null) {
            if (!Optional.ofNullable(currModel.getName()).equals(Optional.ofNullable(prevModel.getName()))) {
                throw new CfnNotUpdatableException(ResourceModel.TYPE_NAME, "Name");
            }

            if (!Optional.ofNullable(currModel.getArn()).equals(Optional.ofNullable(prevModel.getArn()))) {
                throw new CfnNotUpdatableException(ResourceModel.TYPE_NAME, "Arn");
            }

        }
    }
}
