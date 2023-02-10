package software.amazon.events.rule;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.awssdk.services.cloudwatchevents.model.DescribeRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.PutTargetsResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.PutTargetsResultEntry;
import software.amazon.awssdk.services.cloudwatchevents.model.ResourceNotFoundException;
import software.amazon.awssdk.services.cloudwatchevents.model.PutRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.ListTargetsByRuleResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.Target;

import java.util.HashSet;

public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    public static final int MAX_RETRIES_ON_PUT_TARGETS = 5;

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
                proxy.initiate("AWS-Events-Rule::Create::PreExistanceCheck", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToDescribeRuleRequest)
                .makeServiceCall((awsRequest, client) -> {
                    try {
                        proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::describeRule);
                        callbackContext.setRuleExists(true);
                    }
                    catch (ResourceNotFoundException e) {
                        // All goood...
                        logger.log(String.format("StackId: %s: %s [%s] does not yet exist.", request.getStackId(), ResourceModel.TYPE_NAME, awsRequest.name()));
                        callbackContext.setRuleExists(false);
                    }

                    return DescribeRuleResponse.builder().build();
                })
                .done(awsResponse -> {

                    if (!callbackContext.isRuleExists()) {
                        return ProgressEvent.progress(progress.getResourceModel(), progress.getCallbackContext());
                    } else {
                        return ProgressEvent.failed(progress.getResourceModel(), null, HandlerErrorCode.AlreadyExists,
                                String.format("%s already exists", progress.getResourceModel().getName()));
                    }
                })
            )

            // STEP 2 [create/stabilize progress chain - required for resource creation]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::CreateRule", proxyClient,progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(model -> Translator.translateToPutRuleRequest(model, request.getDesiredResourceTags()))
                    .makeServiceCall((awsRequest, client) -> {

                        PutRuleResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::putRule);
                        progress.getResourceModel().setArn(awsResponse.ruleArn());

                        logger.log(String.format("StackId: %s: %s [%s] successfully created.", request.getStackId(), ResourceModel.TYPE_NAME, awsRequest.name()));
                        return awsResponse;
                    })
                    .stabilize((awsRequest, awsResponse, client, model, context) -> {

                        boolean stabilized;

                        try {
                            proxyClient.injectCredentialsAndInvokeV2(
                                    Translator.translateToDescribeRuleRequest(model),
                                    proxyClient.client()::describeRule);

                            stabilized = true;
                        }
                        catch (ResourceNotFoundException e) {
                            stabilized = false;
                        }

                        logger.log(String.format("StackId: %s: %s [%s] has been stabilized: %s", request.getStackId(), ResourceModel.TYPE_NAME, awsRequest.name(), stabilized));
                        return stabilized;
                    })
                    .handleError(this::handleError)
                    .progress()
                )

            .then(progress ->
                proxy.initiate("AWS-Events-Rule::CreateTargets", proxyClient,progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToPutTargetsRequest)
                    .makeServiceCall(((awsRequest, client) -> {
                        PutTargetsResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::putTargets);

                        logger.log(String.format("StackId: %s: %s [%s] successfully created.", request.getStackId(), "AWS::Events::Target", awsRequest.targets().size()));
                        return awsResponse;
                    }))
                    .stabilize((awsRequest, awsResponse, client, model, context) -> {

                        boolean stabilized;

                        if (awsResponse.hasFailedEntries() && awsResponse.failedEntryCount() > 0) {
                            if (callbackContext.getRetryAttemptsForPutTargets() < MAX_RETRIES_ON_PUT_TARGETS) {
                                logger.log(String.format("PutTargets has %s failed entries. Retrying...", awsResponse.failedEntryCount()));

                                for (PutTargetsResultEntry failedEntry : awsResponse.failedEntries()) {
                                    logger.log(failedEntry.errorMessage());
                                }

                                callbackContext.setRetryAttemptsForPutTargets(callbackContext.getRetryAttemptsForPutTargets() + 1);
                                awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::putTargets);
                                stabilized = false;
                            } else {
                                throw AwsServiceException.builder()
                                        .awsErrorDetails(AwsErrorDetails.builder().errorCode("FailedEntries").build())
                                        .build();
                            }
                        }
                        else {
                            logger.log("Checking target stabilization.");

                            ListTargetsByRuleResponse listTargetsResponse =  proxyClient.injectCredentialsAndInvokeV2(
                                    Translator.translateToListTargetsByRuleRequest(model),
                                    proxyClient.client()::listTargetsByRule);

                            HashSet<String> targetIds = new HashSet<>();
                            for (Target target : listTargetsResponse.targets()) {
                                targetIds.add(target.id());
                            }

                            stabilized = true;
                            for (Target target : awsRequest.targets()) {
                                if (!targetIds.contains(target.id())) {
                                    stabilized = false;
                                    break;
                                }
                            }
                        }

                        logger.log(String.format("StackId: %s: %s [%s] have been stabilized: %s", request.getStackId(), "AWS::Events::Target", awsRequest.targets().size(), stabilized));
                        return stabilized;
                    })
                    .handleError(this::handleError)
                    .progress()
                )

            // STEP 3 [TODO: describe call/chain to return the resource model]
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}
