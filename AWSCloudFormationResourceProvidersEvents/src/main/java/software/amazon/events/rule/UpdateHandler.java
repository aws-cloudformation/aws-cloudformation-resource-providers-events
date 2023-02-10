package software.amazon.events.rule;

import org.apache.commons.collections4.CollectionUtils;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.awssdk.services.cloudwatchevents.model.ListTargetsByRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.PutRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.PutTargetsRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.PutTargetsResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.Target;

import java.util.ArrayList;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    public static final int MAX_RETRIES_ON_PUT_TARGETS = 5;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<CloudWatchEventsClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        ArrayList<String> targetIdsToDelete = new ArrayList<>();

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)

            // STEP 1 [check if resource already exists]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::Update::PreUpdateCheck", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToDescribeRuleRequest)
                    .makeServiceCall((awsRequest, client) -> {

                        proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::describeRule);

                        logger.log(String.format("StackId: %s: %s [%s] has successfully been read.", request.getStackId(), ResourceModel.TYPE_NAME, awsRequest.name()));
                        return null;
                    })
                    .handleError(this::handleError)
                    .progress()
            )

            // STEP 2 [Update the Rule]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::Update::Rule", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(model -> Translator.translateToPutRuleRequest(model, request.getDesiredResourceTags()))
                    .makeServiceCall((awsRequest, client) -> {

                        AwsResponse awsResponse;

                        // Update Rule itself
                        awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::putRule);

                        logger.log(String.format("StackId: %s: %s [%s] has successfully been updated.", request.getStackId(), ResourceModel.TYPE_NAME, awsRequest.name()));
                        return awsResponse;
                    })
                    // TODO: Make sure this doesn't need to be stabilized. If not, delete this.
                    .stabilize((awsRequest, awsResponse, client, model, context) -> {

                        final boolean stabilized = true;

                        logger.log(String.format("StackId: %s: %s [%s] update has stabilized: %s", request.getStackId(), ResourceModel.TYPE_NAME, awsRequest.name(), stabilized));
                        return stabilized;
                    })
                    .handleError(this::handleError)
                    .progress()
            )

            // STEP 3 [Get list of existing Targets]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::Update::ListTargets", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToListTargetsByRuleRequest)
                    .makeServiceCall((awsRequest, client) -> {

                        // List existing targets
                        ListTargetsByRuleResponse existingTargetsResponse = proxyClient.injectCredentialsAndInvokeV2(
                                awsRequest,
                                proxyClient.client()::listTargetsByRule);

                        // Create lists of ids
                        ArrayList<String> existingTargetIds = new ArrayList<>();
                        ArrayList<String> modelTargetIds = new ArrayList<>();

                        // Build the list of Targets ids that already exist
                        if (existingTargetsResponse.hasTargets()) {
                            for (Target target : existingTargetsResponse.targets()) {
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
                        targetIdsToDelete.addAll(CollectionUtils.subtract(existingTargetIds, modelTargetIds));

                        logger.log(String.format("StackId: %s: %s [%s] has successfully been read.", request.getStackId(), "AWS::Events::Target", existingTargetsResponse.targets().size()));
                        return null;
                    })
                    .handleError(this::handleError)
                    .progress()
            )

            // STEP 4 [Delete Targets]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::Update::DeleteTargets", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(model -> Translator.translateToRemoveTargetsRequest(model, targetIdsToDelete))
                    .makeServiceCall((awsRequest, client) -> {

                        // Delete targets that should not exist after update
                        if (targetIdsToDelete.size() > 0) {
                            proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::removeTargets);
                        }
                        logger.log(String.format("StackId: %s: %s [%s] has successfully been deleted.", request.getStackId(), "AWS::Events::Target", targetIdsToDelete));
                        return null;
                    })

                    // TODO: Make sure this doesn't need to be stabilized. If not, delete this.
                    .stabilize((awsRequest, awsResponse, client, model, context) -> {
                        final boolean stabilized = true;

                        logger.log(String.format("StackId: %s: %s [%s] delete has stabilized: %s", request.getStackId(), "AWS::Events::Target", targetIdsToDelete, stabilized));
                        return stabilized;
                    })
                    .handleError(this::handleError)
                    .progress()
            )

            // STEP 5 [Create/Update Targets]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::Update::Targets", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::dummmyTranslator)
                    .makeServiceCall((awsRequest, client) -> {
                        PutTargetsResponse awsResponse = null;

                        if (progress.getResourceModel().getTargets() != null) {
                            awsRequest = Translator.translateToPutTargetsRequest(progress.getResourceModel());

                            // Create resulting list of target ids
                            awsResponse = proxyClient.injectCredentialsAndInvokeV2((PutTargetsRequest) awsRequest, proxyClient.client()::putTargets);

                            logger.log(String.format("StackId: %s: %s [%s] has successfully been updated.", request.getStackId(), "AWS::Events::Target", ((PutTargetsRequest) awsRequest).targets().size()));
                        }
                        return awsResponse;
                    })

                    // TODO: Make sure this doesn't need to be stabilized. If not, delete this.
                    .stabilize((awsRequest, awsResponse, client, model, context) -> {
                        boolean stabilized;

                        if (progress.getResourceModel().getTargets() != null) {

                            if (awsResponse.hasFailedEntries() && awsResponse.failedEntryCount() > 0) {
                                if (callbackContext.getRetryAttemptsForPutTargets() < MAX_RETRIES_ON_PUT_TARGETS) {
                                    callbackContext.setRetryAttemptsForPutTargets(callbackContext.getRetryAttemptsForPutTargets() + 1);
                                    awsRequest = Translator.translateToPutTargetsRequest(progress.getResourceModel());
                                    awsResponse = proxyClient.injectCredentialsAndInvokeV2((PutTargetsRequest) awsRequest, proxyClient.client()::putTargets);

                                    stabilized = false;
                                } else {
                                    throw AwsServiceException.builder()
                                            .awsErrorDetails(AwsErrorDetails.builder().errorCode("FailedEntries").build())
                                            .build();
                                }
                            }
                            else {
                                stabilized = true;
                            }

                            logger.log(String.format("StackId: %s: %s [%s] update has stabilized: %s", request.getStackId(), "AWS::Events::Target", progress.getResourceModel().getTargets().size(), stabilized));
                        }
                        else {
                            stabilized = true;
                        }

                        return stabilized;
                    })
                    .handleError(this::handleError)
                    .progress()
            )

            // STEP 6 [TODO: describe call/chain to return the resource model]
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}
