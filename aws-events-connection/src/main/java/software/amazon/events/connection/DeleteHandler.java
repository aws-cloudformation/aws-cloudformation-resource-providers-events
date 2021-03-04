package software.amazon.events.connection;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.ConcurrentModificationException;
import software.amazon.awssdk.services.eventbridge.model.DeleteConnectionRequest;
import software.amazon.awssdk.services.eventbridge.model.DeleteConnectionResponse;
import software.amazon.awssdk.services.eventbridge.model.DescribeConnectionResponse;
import software.amazon.awssdk.services.eventbridge.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static software.amazon.events.connection.Constants.BACK_OFF_DELAY;

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
                        proxy.initiate("AWS-Events-Connection::Delete", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToDeleteRequest)
                                .backoffDelay(BACK_OFF_DELAY)
                                .makeServiceCall(this::deleteResource)
                                .stabilize(this::stabilizedOnDelete)
                                .done(this::setResourceModelToNullAndReturnSuccess));
    }

    private ProgressEvent<ResourceModel, CallbackContext> setResourceModelToNullAndReturnSuccess(
            DeleteConnectionRequest deleteRequest,
            DeleteConnectionResponse deleteResponse,
            ProxyClient<EventBridgeClient> proxyClient,
            ResourceModel resourceModel,
            CallbackContext callbackContext) {
        return ProgressEvent.defaultSuccessHandler(null);
    }

    private DeleteConnectionResponse deleteResource(DeleteConnectionRequest awsRequest, ProxyClient<EventBridgeClient> proxyClient) {
        try {
            DeleteConnectionResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::deleteConnection);
            logger.log(String.format("%s successfully called DeleteConnection. Still need to stabilize.", ResourceModel.TYPE_NAME));
            return awsResponse;
        } catch (final ConcurrentModificationException e) {
            throw new CfnResourceConflictException(e);
        } catch (final ResourceNotFoundException e) {
            throw new CfnNotFoundException(e);
        } catch (final AwsServiceException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }
    }

    private boolean stabilizedOnDelete(
            final DeleteConnectionRequest deleteRequest,
            final DeleteConnectionResponse deleteConnectionResponse,
            final ProxyClient<EventBridgeClient> proxyClient,
            final ResourceModel model,
            final CallbackContext callbackContext) {

        DescribeConnectionResponse describeConnectionResponse;
        boolean stabilized;
        try {
            describeConnectionResponse = proxyClient.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(model),
                    proxyClient.client()::describeConnection);
            switch (describeConnectionResponse.connectionState()) {
                case DELETING:
                    stabilized = false;
                    break;
                default:
                    throw new CfnGeneralServiceException(String.format("Couldn't stabilize %s [%s] due to connection state: %s",
                            ResourceModel.TYPE_NAME, model.getPrimaryIdentifier(), describeConnectionResponse.stateReason()));
            }
        } catch (ResourceNotFoundException e) {
            stabilized = true;
        }
        logger.log(String.format("%s [%s] deletion has stabilized: %s", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier(), stabilized));
        return stabilized;
    }

}
