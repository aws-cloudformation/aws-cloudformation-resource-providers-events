package software.amazon.events.rule;

import org.apache.commons.collections4.CollectionUtils;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;

public class UpdateHandler extends BaseHandlerStd {

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<CloudWatchEventsClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel resourceModel = request.getDesiredResourceState();
        final CompositePID compositePID = new CompositePID(resourceModel, request.getAwsAccountId());

        // Create lists of ids
        ArrayList<String> existingTargetIds = new ArrayList<>();
        ArrayList<String> modelTargetIds = new ArrayList<>();
        ArrayList<String> targetIdsToDelete = new ArrayList<>();

        // Build the list of Targets ids that already exist
        if (request.getPreviousResourceState().getTargets() != null) {
            for (software.amazon.events.rule.Target target : request.getPreviousResourceState().getTargets()) {
                existingTargetIds.add(target.getId());
            }
        }

        // Build the list of Targets ids that should exist after update
        if (request.getDesiredResourceState().getTargets() != null) {
            for (software.amazon.events.rule.Target target : request.getDesiredResourceState().getTargets()) {
                modelTargetIds.add(target.getId());
            }
        }

        // Subtract model target ids from existing target ids to get the list of Targets to delete
        targetIdsToDelete.addAll(CollectionUtils.subtract(existingTargetIds, modelTargetIds));

        return ProgressEvent.progress(resourceModel, callbackContext)

            // STEP 1 [check if resource already exists]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::Update::PreUpdateCheck", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest((model) -> Translator.translateToDescribeRuleRequest(compositePID))
                    .makeServiceCall((awsRequest, client) -> describeRule(awsRequest, client, logger, request.getStackId()))
                    .handleError(this::handleError)
                    .done(awsResponse -> {
                        progress.getResourceModel().setArn(awsResponse.arn());
                        progress.getResourceModel().setId(compositePID.getPid());
                        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext);
                    })
            )

            // STEP 2 [update the rule]putTargetsRequest
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::Update::Rule", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(model -> Translator.translateToPutRuleRequest(model, compositePID))
                    .makeServiceCall((awsRequest, client) -> putRule(awsRequest, client, logger, request.getStackId()))
                    .stabilize((awsRequest, awsResponse, client, model, context) -> stabilizePutRule(client, compositePID, logger, request.getStackId()))
                    .handleError(this::handleError)
                    .progress()
            )

            // STEP 3 [delete extra targets]
            .then(progress -> targetIdsToDelete.size() == 0 ?
                        progress :
                        proxy.initiate("AWS-Events-Rule::Update::DeleteTargets", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(model -> Translator.translateToRemoveTargetsRequest(compositePID, targetIdsToDelete))
                    .makeServiceCall((awsRequest, client) -> removeTargets(awsRequest, client, logger, request.getStackId()))
                    .stabilize((awsRequest, awsResponse, client, model, context) -> stabilizeRemoveTargets(awsResponse, client, compositePID, callbackContext, logger, request.getStackId()))
                    .handleError(this::handleError)
                    .done(awsResponse -> delayedProgress(progress, 30, 1))
            )

            // STEP 4 [put targets]
            .then(progress -> progress.getResourceModel().getTargets() == null || progress.getResourceModel().getTargets().size() == 0 ?
                        progress :
                        proxy.initiate("AWS-Events-Rule::Update::Targets", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest((model) -> Translator.translateToPutTargetsRequest(model, compositePID))
                    .makeServiceCall((awsRequest, client) -> putTargets(awsRequest, client, logger, request.getStackId()))
                    .stabilize((awsRequest, awsResponse, client, model, context) -> stabilizePutTargets(awsResponse, client, model, context, logger, request.getStackId(), compositePID))
                    .handleError(this::handleError)
                    .done(awsResponse -> delayedProgress(progress, 30, 2))
            )

            // STEP 6 [describe call/chain to return the resource model]
            .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));
    }

}
