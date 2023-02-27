package software.amazon.events.rule;

import org.apache.commons.collections4.CollectionUtils;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.awssdk.services.cloudwatchevents.model.Target;
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

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)

            // STEP 1 [check if resource already exists]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::Update::PreUpdateCheck", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToDescribeRuleRequest)
                    .makeServiceCall((awsRequest, client) -> describeRule(awsRequest, client, logger, request.getStackId()))
                    .handleError(this::handleError)
                    .progress()
            )

            // STEP 2 [update the rule]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::Update::Rule", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(model -> Translator.translateToPutRuleRequest(model, request.getDesiredResourceTags()))
                    .makeServiceCall((awsRequest, client) -> putRule(awsRequest, client, logger, request.getStackId()))
                    .stabilize((awsRequest, awsResponse, client, model, context) -> stabilizePutRule(client, model, logger, request.getStackId()))
                    .handleError(this::handleError)
                    .progress()
            )

            // STEP 3 [get list of existing targets]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::Update::ListTargets", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToListTargetsByRuleRequest)
                    .makeServiceCall((awsRequest, client) -> listTargets(awsRequest, client, logger, request.getStackId()))
                    .handleError(this::handleError)
                    .done(awsResponse -> {
                        // Record the list of targets to be deleted.

                        // Create lists of ids
                        ArrayList<String> existingTargetIds = new ArrayList<>();
                        ArrayList<String> modelTargetIds = new ArrayList<>();

                        // Build the list of Targets ids that already exist
                        if (awsResponse.hasTargets()) {
                            for (Target target : awsResponse.targets()) {
                                existingTargetIds.add(target.id());
                            }
                        }

                        // Build the list of Targets ids that should exist after update
                        if (progress.getResourceModel().getTargets() != null) {
                            for (Target target : Translator.translateToPutTargetsRequest(progress.getResourceModel()).targets()) {
                                modelTargetIds.add(target.id());
                            }
                        }

                        // Subtract model target ids from existing target ids to get the list of Targets to delete
                        callbackContext.setTargetIdsToDelete(new ArrayList<>());
                        callbackContext.getTargetIdsToDelete().addAll(CollectionUtils.subtract(existingTargetIds, modelTargetIds));

                        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext);
                    })
            )

            // STEP 4 [delete extra targets]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::Update::DeleteTargets", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(model -> Translator.translateToRemoveTargetsRequest(model, callbackContext.getTargetIdsToDelete()))
                    .makeServiceCall((awsRequest, client) -> removeTargets(awsRequest, client, logger, request.getStackId(), callbackContext.getTargetIdsToDelete()))
                    .stabilize((awsRequest, awsResponse, client, model, context) -> stabilizeRemoveTargets(awsRequest, awsResponse, client, model, callbackContext, logger, request.getStackId(), callbackContext.getTargetIdsToDelete()))
                    .handleError(this::handleError)
                    .progress()
            )

            // STEP 5 [put targets]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::Update::Targets", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToPutTargetsRequest)
                    .makeServiceCall((awsRequest, client) -> putTargets(awsRequest, client, logger, request.getStackId()))
                    .stabilize((awsRequest, awsResponse, client, model, context) -> stabilizePutTargets(awsResponse, client, model, callbackContext, logger, request.getStackId()))
                    .handleError(this::handleError)
                    .progress()
            )

            // STEP 6 [describe call/chain to return the resource model]
            .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));
    }

}
