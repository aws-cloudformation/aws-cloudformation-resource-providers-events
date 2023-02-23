package software.amazon.events.rule;

import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.awssdk.services.cloudwatchevents.model.*;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class CreateHandler extends BaseHandlerStd {

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
                proxy.initiate("AWS-Events-Rule::Create::PreExistenceCheck", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToDescribeRuleRequest)
                .makeServiceCall((awsRequest, client) -> {

                    // Determine whether the rule exists
                    try {
                        describeRule(awsRequest, client, logger, request.getStackId());
                        callbackContext.setRuleExists(true);
                    }
                    catch (ResourceNotFoundException e) {
                        logger.log(String.format("StackId: %s: %s [%s] does not yet exist.", request.getStackId(), ResourceModel.TYPE_NAME, awsRequest.name()));
                        callbackContext.setRuleExists(false);
                    }

                    return DescribeRuleResponse.builder().build();
                })
                .done(awsResponse -> {

                    if (callbackContext.isRuleExists()) {
                        // If the rule already exists, return failure
                        return ProgressEvent.failed(progress.getResourceModel(), null, HandlerErrorCode.AlreadyExists,
                                String.format("%s already exists", progress.getResourceModel().getName()));
                    } else {
                        // If the rule does not yet exist, continue
                        return ProgressEvent.progress(progress.getResourceModel(), progress.getCallbackContext());
                    }
                })
            )

            // STEP 2 [create/stabilize rule]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::CreateRule", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(model -> Translator.translateToPutRuleRequest(model, request.getDesiredResourceTags()))
                    .makeServiceCall((awsRequest, client) -> putRule(awsRequest, client, logger, request.getStackId()))
                    .stabilize((awsRequest, awsResponse, client, model, context) -> stabilizePutRule(client, model, logger, request.getStackId()))
                    .handleError(this::handleError)
                    .progress()
                )

            // STEP 3 [create/stabilize targets]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::CreateTargets", proxyClient,progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToPutTargetsRequest)
                    .makeServiceCall((awsRequest, client) -> putTargets(awsRequest, client, logger, request.getStackId()))
                    .stabilize((awsRequest, awsResponse, client, model, context) -> stabilizePutTargets(awsResponse, client, model, callbackContext, logger, request.getStackId()))
                    .handleError(this::handleError)
                    .progress()
                )

            // STEP 4 [describe call/chain to return the resource model]
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

}
