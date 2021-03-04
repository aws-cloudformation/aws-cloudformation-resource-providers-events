package software.amazon.events.connection;

import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.eventbridge.model.ConnectionState;
import software.amazon.awssdk.services.eventbridge.model.CreateConnectionRequest;
import software.amazon.awssdk.services.eventbridge.model.CreateConnectionResponse;
import software.amazon.awssdk.services.eventbridge.model.DescribeConnectionResponse;
import software.amazon.awssdk.services.eventbridge.model.LimitExceededException;
import software.amazon.awssdk.services.eventbridge.model.ResourceAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;
import java.util.function.Function;

import static software.amazon.events.connection.Constants.BACK_OFF_DELAY;
import static software.amazon.events.connection.Constants.EMPTY_CALL;


public class CreateHandler extends BaseHandlerStd {
    private static final int MAX_CONNECTION_NAME_LENGTH = 64;
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

        Utils.verifyAuthorizationTypeParameterMapping(model);

        if (StringUtils.isNullOrEmpty(model.getName())) {
            model.setName(
                    IdentifierUtils.generateResourceIdentifier(
                            request.getLogicalResourceIdentifier(), request.getClientRequestToken(), MAX_CONNECTION_NAME_LENGTH));
        }

        return ProgressEvent.progress(model, callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-Events-Connection::Create", proxyClient, progress.getResourceModel(), progress.getCallbackContext())

                                .translateToServiceRequest(Translator::translateToCreateRequest)
                                .makeServiceCall(this::createResource)
                                .progress()
                )
                .then(progress ->
                        // If your resource requires some form of stabilization (e.g. service does not provide strong consistency), you will need to ensure that your code
                        // accounts for any potential issues, so that a subsequent read/update requests will not cause any conflicts (e.g. NotFoundException/InvalidRequestException)
                        proxy.initiate("AWS-Events-Connection::PostCreateStabilize", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Function.identity())
                                .backoffDelay(BACK_OFF_DELAY)
                                .makeServiceCall(EMPTY_CALL)
                                .stabilize((awsRequest, response, proxyInvocation, resourceModel, context) -> isStabilized(proxyClient, resourceModel))
                                .progress())
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));

    }

    private CreateConnectionResponse createResource(CreateConnectionRequest awsRequest, ProxyClient<EventBridgeClient> proxyClient) {
        try {
            CreateConnectionResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::createConnection);
            logger.log(String.format("%s successfully called CreateConnection. Still need to stabilize.", ResourceModel.TYPE_NAME));
            return awsResponse;
        } catch (final ResourceAlreadyExistsException e) {
            throw new CfnAlreadyExistsException(e);
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
                stabilized = true;
                break;
            case CREATING:
            case AUTHORIZING:
            case DEAUTHORIZING:
                stabilized = false;
                break;
            default:
                throw new CfnGeneralServiceException(String.format("Couldn't stabilize %s [%s] due to connection state: %s",
                        ResourceModel.TYPE_NAME, model.getPrimaryIdentifier(), describeConnectionResponse.stateReason()));
        }

        logger.log(String.format("%s [%s] create has stabilized: %s. Connection state: %s", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier(), stabilized, state.toString()));
        return stabilized;
    }


}
