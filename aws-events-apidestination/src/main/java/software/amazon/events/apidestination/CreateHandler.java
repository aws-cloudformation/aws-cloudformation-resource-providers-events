package software.amazon.events.apidestination;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.CreateApiDestinationRequest;
import software.amazon.awssdk.services.eventbridge.model.CreateApiDestinationResponse;
import software.amazon.cloudformation.exceptions.*;
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

        awsResponse = translateAwsServiceException(awsRequest.name(),
                () -> proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::createApiDestination)
        );
        logger.log(String.format("%s successfully created.", ResourceModel.TYPE_NAME));

        return awsResponse;
    }

}
