package software.amazon.events.connection;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.ConcurrentModificationException;
import software.amazon.awssdk.services.eventbridge.model.ConnectionState;
import software.amazon.awssdk.services.eventbridge.model.DescribeConnectionResponse;
import software.amazon.awssdk.services.eventbridge.model.LimitExceededException;
import software.amazon.awssdk.services.eventbridge.model.ResourceNotFoundException;
import software.amazon.awssdk.services.eventbridge.model.UpdateConnectionRequest;
import software.amazon.awssdk.services.eventbridge.model.UpdateConnectionResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.function.Function;

import static software.amazon.events.connection.Constants.BACK_OFF_DELAY;
import static software.amazon.events.connection.Constants.EMPTY_CALL;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<EventBridgeClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        Utils.verifyAuthorizationTypeParameterMapping(request.getDesiredResourceState());

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-Events-Connection::Update", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(model -> Translator.translateToUpdateRequest(model))
                                .makeServiceCall(this::updateResource)
                                .progress())
                .then(progress ->
                        proxy.initiate("AWS-Events-Connection::PostUpdateStabilize", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Function.identity())
                                .backoffDelay(BACK_OFF_DELAY)
                                .makeServiceCall(EMPTY_CALL)
                                .stabilize((awsRequest, response, proxyInvocation, model, context) -> isStabilized(proxyClient, model))
                                .progress())
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private UpdateConnectionResponse updateResource(UpdateConnectionRequest awsRequest, ProxyClient<EventBridgeClient> proxyClient) {
        try {
            UpdateConnectionResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::updateConnection);
            logger.log(String.format("%s successfully called UpdateConnection. Still need to stabilize.", ResourceModel.TYPE_NAME));
            return awsResponse;
        } catch (final ResourceNotFoundException e) {
            throw new CfnNotFoundException(e);
        } catch (final ConcurrentModificationException e) {
            throw new CfnResourceConflictException(e);
        } catch (final LimitExceededException e) {
            throw new CfnServiceLimitExceededException(e);
        } catch (final AwsServiceException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }
    }

    private Boolean isStabilized(ProxyClient<EventBridgeClient> proxyClient, ResourceModel model) {
        DescribeConnectionResponse describeConnectionResponse = Utils.readResource(Translator.translateToReadRequest(model), proxyClient);
        final ConnectionState state = describeConnectionResponse.connectionState();

        boolean stabilized;
        switch (state) {
            case AUTHORIZED:
                stabilized =  true;
                break;
            case UPDATING:
            case AUTHORIZING:
            case DEAUTHORIZING:
                stabilized = false;
                break;
            default:
                throw new CfnGeneralServiceException(String.format("Couldn't stabilize %s [%s] due to connection state: %s",
                        ResourceModel.TYPE_NAME, model.getPrimaryIdentifier(), describeConnectionResponse.stateReason()));
        }

        logger.log(String.format("%s [%s] update has stabilized: %s. Connection state: %s", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier(), stabilized, state.toString()));
        return stabilized;
    }
}
