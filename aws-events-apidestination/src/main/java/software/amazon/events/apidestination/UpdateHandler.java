package software.amazon.events.apidestination;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.ConcurrentModificationException;
import software.amazon.awssdk.services.eventbridge.model.LimitExceededException;
import software.amazon.awssdk.services.eventbridge.model.ResourceNotFoundException;
import software.amazon.awssdk.services.eventbridge.model.UpdateApiDestinationRequest;
import software.amazon.awssdk.services.eventbridge.model.UpdateApiDestinationResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotUpdatableException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

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
        try {
            awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::updateApiDestination);
        } catch (final ConcurrentModificationException e) {
            // There is a concurrent modification on a connection
            throw new CfnResourceConflictException(e);
        } catch (final ResourceNotFoundException e) {
            // Provided connection arn does not exist
            throw new CfnNotFoundException(e);
        } catch (final LimitExceededException e) {
            // Resource limit exceeded
            throw new CfnServiceLimitExceededException(ResourceModel.TYPE_NAME, awsRequest.name(), e);
        } catch (final AwsServiceException e) {
            // general exception
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }

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
