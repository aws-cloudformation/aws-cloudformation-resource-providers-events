package software.amazon.events.rule;

import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.awssdk.services.cloudwatchevents.model.DeleteRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.ListTargetsByRuleResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.concurrent.atomic.AtomicReference;

public class DeleteHandler extends BaseHandlerStd {

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

                        listTargetsResponse.set(proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::listTargetsByRule));

                        logger.log(String.format("StackId: %s: %s [%s] successfully read.", request.getStackId(), "AWS::Events::Target", listTargetsResponse.get().targets().size()));
                        return null;
                    })
                    .handleError(this::handleError)
                    .progress()
            )

            // STEP 2 [Delete Targets]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::DeleteTargets", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(model -> Translator.translateToRemoveTargetsRequest(model, listTargetsResponse.get()))
                    .makeServiceCall((awsRequest, client) -> {

                        AwsResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::removeTargets);

                        logger.log(String.format("StackId: %s: %s [%s] successfully deleted.", request.getStackId(), "AWS::Events::Target", listTargetsResponse.get().targets().size()));
                        return awsResponse;
                    })

                    // TODO: Make sure this doesn't need to be stabilized. If not, delete this.
                    .stabilize((awsRequest, awsResponse, client, model, context) -> {
                        final boolean stabilized = true;
                        logger.log(String.format("StackId: %s: %s [%s] deletion has stabilized: %s", request.getStackId(), "AWS::Events::Target", listTargetsResponse.get().targets().size(), stabilized));
                        return stabilized;
                    })
                    .handleError(this::handleError)
                    .progress()
            )

            // STEP 3 [Delete Rule]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::DeleteRule", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToDeleteRuleRequest)
                    .makeServiceCall((awsRequest, client) -> {

                        DeleteRuleResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::deleteRule);

                        logger.log(String.format("StackId: %s: %s [%s] successfully deleted.", request.getStackId(), ResourceModel.TYPE_NAME, awsRequest.name()));
                        return awsResponse;
                    })

                    // TODO: Make sure this doesn't need to be stabilized. If not, delete this.
                    .stabilize((awsRequest, awsResponse, client, model, context) -> {
                        final boolean stabilized = true;
                        logger.log(String.format("StackId: %s: %s [%s] deletion has stabilized: %s", request.getStackId(), ResourceModel.TYPE_NAME, awsRequest.name(), stabilized));
                        return stabilized;
                    })
                    .handleError(this::handleError)
                    .progress()
            )

            // STEP 4 [TODO: return the successful progress event without resource model]
            .then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }
}
