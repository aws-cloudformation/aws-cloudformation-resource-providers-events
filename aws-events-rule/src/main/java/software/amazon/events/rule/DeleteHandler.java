package software.amazon.events.rule;

import org.apache.commons.lang3.ObjectUtils;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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

        if (resourceModel.getTargets() != null && resourceModel.getTargets().size() != 0) {
            callbackContext.setTargetIds(extractTargetIds(resourceModel.getTargets()));
        }
        else {
            callbackContext.setTargetIds(new ArrayList<>());
        }

        return ProgressEvent.progress(resourceModel, callbackContext)
            // STEP 1 [check if resource exists]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::ExistenceCheck", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest((model) -> Translator.translateToDescribeRuleRequest(compositePID))
                    .makeServiceCall((awsRequest, client) -> describeRule(awsRequest, client, logger, request.getStackId()))
                    .handleError(this::handleError)
                    .done(awsResponse -> {
                        progress.getResourceModel().setArn(awsResponse.arn());
                        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext);
                    })
            )

            // STEP 3 [get targets]
            .then(progress -> callbackContext.getTargetIds().size() != 0 ?
                    progress :
                // If there are no targets in the resource model, get them
                softFailAccessDenied(() -> proxy.initiate("AWS-Events-Rule::ListTargets", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest((model ) -> Translator.translateToListTargetsByRuleRequest(compositePID))
                    .makeServiceCall((awsRequest, client) -> listTargets(awsRequest, client, logger, request.getStackId()))
                    .handleError(this::handleError)
                    .done(awsResponse -> {
                        // Record the list of Targets
                        callbackContext.setTargetIds(extractTargetIds(Translator.translateFromListTargetsByRuleResponse(awsResponse)));
                        return ProgressEvent.progress(progress.getResourceModel(), callbackContext);
                    }), request.getDesiredResourceState(), callbackContext)
            )

            // STEP 3 [delete targets]
            .then(progress -> callbackContext.getTargetIds().size() == 0 ?
                        progress :
                proxy.initiate("AWS-Events-Rule::DeleteTargets", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(unused -> Translator.translateToRemoveTargetsRequest(compositePID, callbackContext.getTargetIds()))
                    .makeServiceCall((awsRequest, client) -> removeTargets(awsRequest, client, logger, request.getStackId()))
                    .stabilize((awsRequest, awsResponse, client, model, context) -> stabilizeRemoveTargets(awsResponse, client, compositePID, callbackContext, logger, request.getStackId()))
                    .handleError(this::handleError)
                    .done(awsResponse -> delayedProgress(progress, 30, 1))
            )

            // STEP 4 [delete rule]
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

    private static Collection<String> extractTargetIds(Set<Target> targets)
    {
        ArrayList<String> targetIds = new ArrayList<>();

        for (Target target : targets)
        {
            targetIds.add(target.getId());
        }

        return targetIds;
    }
}
