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
        final CompositePID compositePID = new CompositePID(resourceModel, request.getAwsAccountId());


        return ProgressEvent.progress(resourceModel, callbackContext)
            // STEP 1 [list targets]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::ListTargets", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest((model ) -> Translator.translateToListTargetsByRuleRequest(compositePID))
                    .makeServiceCall((awsRequest, client) -> listTargets(awsRequest, client, logger, request.getStackId()))
                    .handleError(this::handleError)
                    .done(awsResponse -> {
                        // Record the list of Targets
                        callbackContext.setListTargetsByRuleResponse(awsResponse);
                        return ProgressEvent.progress(progress.getResourceModel(), callbackContext);
                    })
            )

            // STEP 2 [delete targets]
            .then(progress -> !callbackContext.getListTargetsByRuleResponse().hasTargets() || callbackContext.getListTargetsByRuleResponse().targets().isEmpty() ?
                        progress :
                        proxy.initiate("AWS-Events-Rule::DeleteTargets", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(model -> Translator.translateToRemoveTargetsRequest(compositePID, callbackContext.getListTargetsByRuleResponse()))
                    .makeServiceCall((awsRequest, client) -> removeTargets(awsRequest, client, logger, request.getStackId(), awsRequest.ids()))
                    .stabilize((awsRequest, awsResponse, client, model, context) -> stabilizeRemoveTargets(awsResponse, client, compositePID, callbackContext, logger, request.getStackId(), awsRequest.ids()))
                    .handleError(this::handleError)
                    .progress() // TODO 30
            )

            // STEP 3 [delete rule]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::DeleteRule", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest((model) -> Translator.translateToDeleteRuleRequest(compositePID))
                    .makeServiceCall((awsRequest, client) -> deleteRule(awsRequest, client, logger, request.getStackId()))
                    .handleError(this::handleError)
                    .progress() // TODO 30
            )

            // STEP 4 [return the successful progress event without resource model]
            .then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }

}
