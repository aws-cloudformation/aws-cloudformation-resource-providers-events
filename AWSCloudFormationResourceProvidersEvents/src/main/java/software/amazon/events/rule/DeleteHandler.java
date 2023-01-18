package software.amazon.events.rule;

import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.awssdk.services.cloudwatchevents.model.DeleteRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.ListTargetsByRuleResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.concurrent.atomic.AtomicReference;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<CloudWatchEventsClient> proxyClient,
        final Logger logger) {

        this.logger = logger;
        AtomicReference<ListTargetsByRuleResponse> listTargetsResponse = new AtomicReference<>();

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)

            // STEP 1 [List Targets]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::ListTargets", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToListTargetsByRuleRequest)
                    .makeServiceCall((awsRequest, client) -> {
                        try {
                            listTargetsResponse.set(proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::listTargetsByRule));

                        } catch (final AwsServiceException e) {
                            // TODO Make sure this is correct
                            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
                        }

                        logger.log(String.format("%s successfully read.", "AWS::Events::Target"));
                        return null;
                    })
                    .progress()
            )

            // STEP 2 [Delete Targets]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::DeleteTargets", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(model -> Translator.translateToRemoveTargetsRequest(model, listTargetsResponse.get()))
                    .makeServiceCall((awsRequest, client) -> {

                        AwsResponse awsResponse;

                        try {
                            awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::removeTargets);

                        } catch (final AwsServiceException e) {
                            // TODO Make sure this is correct
                            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
                        }

                        logger.log(String.format("%s successfully deleted.", "AWS::Events::Target"));
                        return awsResponse;
                    })

                    // TODO: Make sure this doesn't need to be stabilized. If not, delete this.
                    .stabilize((awsRequest, awsResponse, client, model, context) -> {
                        final boolean stabilized = true;
                        logger.log(String.format("%s deletion has stabilized: %s", "AWS::Events::Target", stabilized));
                        return stabilized;
                    })
                    .progress()
            )

            // STEP 3 [Delete Rule]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::DeleteRule", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToDeleteRuleRequest)
                    .makeServiceCall((awsRequest, client) -> {

                        DeleteRuleResponse awsResponse;

                        try {
                            awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::deleteRule);

                        } catch (final AwsServiceException e) {
                            // TODO Make sure this is correct
                            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
                        }

                        logger.log(String.format("%s successfully deleted.", ResourceModel.TYPE_NAME));
                        return awsResponse;
                    })

                    // TODO: Make sure this doesn't need to be stabilized. If not, delete this.
                    .stabilize((awsRequest, awsResponse, client, model, context) -> {
                        final boolean stabilized = true;
                        logger.log(String.format("%s [%s] deletion has stabilized: %s", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier(), stabilized));
                        return stabilized;
                    })
                    .progress()
            )

            // STEP 4 [TODO: return the successful progress event without resource model]
            .then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }
}
