package software.amazon.events.rule;

import org.apache.commons.collections4.CollectionUtils;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.awssdk.services.cloudwatchevents.model.ListTargetsByRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.Target;

import java.util.ArrayList;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

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

                        try {
                            proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::describeRule);
                        }
                        catch (ResourceNotFoundException e) {
                            throw new software.amazon.cloudformation.exceptions.ResourceNotFoundException(ResourceModel.TYPE_NAME, progress.getResourceModel().getName());
                        }

                        logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                        return null;
                    })
                    .progress()
            )

            // STEP 2 [Update the Rule]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::Update::Rule", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(model -> Translator.translateToPutRuleRequest(model, request.getDesiredResourceTags()))
                    .makeServiceCall((awsRequest, client) -> {

                        AwsResponse awsResponse;

                        try {
                            // Update Rule itself
                            awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::putRule);

                        } catch (final AwsServiceException e) {
                            // TODO Make sure this is correct
                            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
                        }

                        logger.log(String.format("%s has successfully been updated.", ResourceModel.TYPE_NAME));
                        return awsResponse;
                    })
                    // TODO: Make sure this doesn't need to be stabilized. If not, delete this.
                    .stabilize((awsRequest, awsResponse, client, model, context) -> {

                        final boolean stabilized = true;

                        logger.log(String.format("%s [%s] update has stabilized: %s", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier(), stabilized));
                        return stabilized;
                    })
                    .progress()
            )

            // STEP 3 [Get list of existing Targets]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::Update::ListTargets", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToListTargetsByRuleRequest)
                    .makeServiceCall((awsRequest, client) -> {

                        try {

                            // List existing targets
                            ListTargetsByRuleResponse existingTargetsResponse = proxyClient.injectCredentialsAndInvokeV2(
                                    awsRequest,
                                    proxyClient.client()::listTargetsByRule);

                            // Create lists of ids
                            ArrayList<String> existingTargetIds = new ArrayList<>();
                            ArrayList<String> modelTargetIds = new ArrayList<>();

                            // Build the list of Targets ids that already exist
                            for (Target target : existingTargetsResponse.targets()) {
                                existingTargetIds.add(target.id());
                            }

                            // Build the list of Targets ids that should exist after update
                            for (Target target : Translator.translateToPutTargetsRequest(progress.getResourceModel()).targets()) {
                                modelTargetIds.add(target.id());
                            }

                            // Subtract model target ids from existing target ids to get the list of Targets to delete
                            targetIdsToDelete.addAll(CollectionUtils.subtract(existingTargetIds, modelTargetIds));

                        } catch (final AwsServiceException e) {
                            // TODO Make sure this is correct
                            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
                        }

                        logger.log(String.format("%s has successfully been read.", "AWS::Events::Target"));
                        return null;
                    })
                    .progress()
            )

            // STEP 4 [Delete Targets]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::Update::DeleteTargets", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(model -> Translator.translateToRemoveTargetsRequest(model, targetIdsToDelete))
                    .makeServiceCall((awsRequest, client) -> {

                        try {
                            // Delete targets that should not exist after update
                            if (targetIdsToDelete.size() > 0) {
                                proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::removeTargets);
                            }
                        } catch (final AwsServiceException e) {
                            // TODO Make sure this is correct
                            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
                        }

                        logger.log(String.format("%s has successfully been deleted.", "AWS::Events::Target"));
                        return null;
                    })

                    // TODO: Make sure this doesn't need to be stabilized. If not, delete this.
                    .stabilize((awsRequest, awsResponse, client, model, context) -> {
                        final boolean stabilized = true;

                        logger.log(String.format("%s delete has stabilized: %s", "AWS::Events::Target", stabilized));
                        return stabilized;
                    })
                    .progress()
            )

            // STEP 5 [Create/Update Targets]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::Update::Targets", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToPutTargetsRequest)
                    .makeServiceCall((awsRequest, client) -> {

                        try {
                            // Create resulting list of target ids
                            proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::putTargets);

                        } catch (final AwsServiceException e) {
                            // TODO Make sure this is correct
                        }

                        logger.log(String.format("%s has successfully been updated.", "AWS::Events::Target"));
                        return null;
                    })

                    // TODO: Make sure this doesn't need to be stabilized. If not, delete this.
                    .stabilize((awsRequest, awsResponse, client, model, context) -> {
                        final boolean stabilized = true;

                        logger.log(String.format("%s update has stabilized: %s", "AWS::Events::Target", stabilized));
                        return stabilized;
                    })
                    .progress()
            )

            // STEP 6 [TODO: describe call/chain to return the resource model]
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}
