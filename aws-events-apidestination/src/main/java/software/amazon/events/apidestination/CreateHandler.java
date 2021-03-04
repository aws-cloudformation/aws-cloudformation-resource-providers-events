package software.amazon.events.apidestination;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.CreateApiDestinationRequest;
import software.amazon.awssdk.services.eventbridge.model.CreateApiDestinationResponse;
import software.amazon.awssdk.services.eventbridge.model.LimitExceededException;
import software.amazon.awssdk.services.eventbridge.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.eventbridge.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

public class CreateHandler extends BaseHandlerStd {

    private static final int MAX_API_DESTINATION_NAME_LENGTH = 64;
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

        verifyNonCreatableFields(model);

        if (StringUtils.isNullOrEmpty(model.getName())) {
            model.setName(
                    IdentifierUtils.generateResourceIdentifier(
                            request.getLogicalResourceIdentifier(), request.getClientRequestToken(), MAX_API_DESTINATION_NAME_LENGTH));
        }

        return ProgressEvent.progress(model, callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-Events-ApiDestination::Create", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToCreateRequest)
                                .makeServiceCall(this::createResource)
                                .progress()
                )
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));


    }

    private void verifyNonCreatableFields(ResourceModel resourceModel) {
        if (resourceModel.getArn() != null) {
            throw new CfnInvalidRequestException(ResourceModel.TYPE_NAME);
        }
    }

    private CreateApiDestinationResponse createResource(CreateApiDestinationRequest awsRequest, ProxyClient<EventBridgeClient> proxyClient) {
        CreateApiDestinationResponse awsResponse;
        try {
            awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::createApiDestination);
        } catch (final ResourceAlreadyExistsException e) {
            // ApiDestination with the same name already exist in the customer account
            throw new CfnAlreadyExistsException(e);
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

        logger.log(String.format("%s successfully created.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }

}
