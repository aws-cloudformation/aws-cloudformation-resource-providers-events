package software.amazon.events.rule;

import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<CloudWatchEventsClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel resourceModel = request.getDesiredResourceState();
        final ResourceModel previousModel = request.getPreviousResourceState();
        final CompositePID compositePID = new CompositePID(resourceModel, request.getAwsAccountId());

        logger.log("Targets: " + previousModel.getTargets().toString());

        return ProgressEvent.progress(resourceModel, callbackContext)
            // STEP 1 [check if resource exists]
            .then(progress ->
            proxy.initiate("AWS-Events-Rule::Update::PreUpdateCheck", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest((model) -> Translator.translateToDescribeRuleRequest(compositePID))
                .makeServiceCall((awsRequest, client) -> describeRule(awsRequest, client, logger, request.getStackId()))
                .handleError(this::handleError)
                .done(awsResponse -> {
                    progress.getResourceModel().setArn(awsResponse.arn());
                    return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext);
                })
            )

            // STEP 2 [delete targets]
            .then(progress -> previousModel.getTargets() == null || previousModel.getTargets().size() == 0 ?
                        progress :
                        proxy.initiate("AWS-Events-Rule::DeleteTargets", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(unused -> Translator.translateToRemoveTargetsRequest(compositePID, previousModel))
                    .makeServiceCall((awsRequest, client) -> removeTargets(awsRequest, client, logger, request.getStackId()))
                    .stabilize((awsRequest, awsResponse, client, model, context) -> stabilizeRemoveTargets(awsResponse, client, compositePID, callbackContext, logger, request.getStackId()))
                    .handleError(this::handleError)
                    .done(awsResponse -> delayedProgress(progress, 30, 1))
            )

            // STEP 3 [delete rule]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::DeleteRule", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest((model) -> Translator.translateToDeleteRuleRequest(compositePID))
                    .makeServiceCall((awsRequest, client) -> deleteRule(awsRequest, client, logger, request.getStackId()))
                    .handleError(this::handleError)
                    .done(awsResponse -> delayedProgress(progress, 30, 1))
            )

            // STEP 4 [return the successful progress event without resource model]
            .then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }

}
