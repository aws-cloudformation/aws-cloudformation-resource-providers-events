package software.amazon.events.rule;

import com.amazonaws.AmazonServiceException;
import com.fasterxml.jackson.databind.ser.Serializers;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.awssdk.services.cloudwatchevents.model.DescribeRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

public class CreateHandler extends BaseHandlerStd {
    private final int EVENT_RULE_NAME_MAX_LENGTH = 64;

    private String generateEventRuleName(final ResourceHandlerRequest<ResourceModel> request) {
        return IdentifierUtils.generateResourceIdentifier(
            request.getStackId(),
            request.getLogicalResourceIdentifier(),
            request.getClientRequestToken(),
            EVENT_RULE_NAME_MAX_LENGTH
        );
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<CloudWatchEventsClient> proxyClient,
        final Logger logger) {

        this.logger = logger;
        final ResourceModel resourceModel = request.getDesiredResourceState();

        // Auto generate name if not present
        if (resourceModel.getName() == null) {
            resourceModel.setName(generateEventRuleName(request));
        }

        final CompositePID compositePID = new CompositePID(resourceModel, request.getAwsAccountId());
        resourceModel.setId(compositePID.getPid());

        return ProgressEvent.progress(resourceModel, callbackContext)

            // STEP 1 [check if resource already exists]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::Create::PreExistenceCheck", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest((model) -> Translator.translateToDescribeRuleRequest(compositePID))
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
                    .translateToServiceRequest(model -> Translator.translateToPutRuleRequest(model, compositePID))
                    .makeServiceCall((awsRequest, client) -> putRule(awsRequest, client, logger, request.getStackId()))
                    .stabilize((awsRequest, awsResponse, client, model, context) -> stabilizePutRule(client, compositePID, logger, request.getStackId()))
                    .handleError(this::handleError)
                    .done(awsResponse -> {
                        progress.getResourceModel().setArn(awsResponse.ruleArn());
                        callbackContext.setRuleCreated(true);

                        return delayedProgress(progress, 30, 1);
                    })
                )

            // STEP 3 [create/stabilize targets]
            .then(progress -> progress.getResourceModel().getTargets() == null ?
                            progress :
                            proxy.initiate("AWS-Events-Rule::CreateTargets", proxyClient,progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest((model) -> Translator.translateToPutTargetsRequest(model, compositePID))
                    .makeServiceCall((awsRequest, client) -> putTargets(awsRequest, client, logger, request.getStackId()))
                    .stabilize((awsRequest, awsResponse, client, model, context) -> stabilizePutTargets(awsResponse, client, model, context, logger, request.getStackId(), compositePID))
                    .handleError((req, e, proxyC, model, context) -> {

                        if (BaseHandlerStd.ERROR_CODE_THROTTLING_EXCEPTION.equals(BaseHandlerStd.getErrorCode(e)))
                        {
                            return ProgressEvent.defaultInProgressHandler(context, 5, model);
                        }
                        return handleError(req, e, proxyC, model, context);
                    })
                    .done(awsResponse -> delayedProgress(progress, 30, 2))
                )

            // STEP 4 [describe call/chain to return the resource model]
            .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));
    }

}
