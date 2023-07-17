package software.amazon.events.rule;

import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<CloudWatchEventsClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        logger.log("List handler starting with model: " + request.getDesiredResourceState());
        logger.log("Stack ID: " + request.getStackId());

        ResourceModel model = request.getDesiredResourceState();

        return proxy.initiate("AWS-Events-Rule::List", proxyClient, model, callbackContext)
                .translateToServiceRequest(r -> Translator.translateToListRulesRequest(request.getNextToken()))
                .makeServiceCall((awsRequest, client) -> listRules(awsRequest, client, logger, request.getStackId()))
                .handleError(this::handleError)
                .done((awsRequest, awsResponse, client, resourceModel, context) -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModels(Translator.translateFromListRulesResponse(awsResponse))
                        .nextToken(awsResponse.nextToken())
                        .status(OperationStatus.SUCCESS)
                        .build());
    }
}
