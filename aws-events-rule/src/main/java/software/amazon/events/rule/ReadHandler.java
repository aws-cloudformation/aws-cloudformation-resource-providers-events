package software.amazon.events.rule;

import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudWatchEventsClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel resourceModel = request.getDesiredResourceState();
        final CompositePID compositePID = new CompositePID(resourceModel, request.getAwsAccountId());

        return ProgressEvent.progress(resourceModel, callbackContext)

            // STEP 1 [read rule]
            .then(progress -> proxy.initiate("AWS-Events-Rule::ReadRule", proxyClient, request.getDesiredResourceState(), callbackContext)
                .translateToServiceRequest((model) -> Translator.translateToDescribeRuleRequest(compositePID))
                .makeServiceCall((awsRequest, client) -> describeRule(awsRequest, client, logger, request.getStackId()))
                .handleError(this::handleError)
                .done(awsResponse -> {
                    // Build the Rule part of the response
                    callbackContext.setResourceModelBuilder(Translator.translateFromDescribeRuleResponse(awsResponse));
                    return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext);
                })
            )

            // STEP 2 [list targets]
            .then(p -> proxy.initiate("AWS-Events-Rule::ListTargets", proxyClient, request.getDesiredResourceState(), callbackContext)
                .translateToServiceRequest((model) -> Translator.translateToListTargetsByRuleRequest(compositePID))
                .makeServiceCall((awsRequest, client) -> listTargets(awsRequest, client, logger, request.getStackId()))
                .handleError(this::handleError)
                .done(awsResponse -> {
                    // Add the list of Targets to the response
                    if (awsResponse.hasTargets()) {
                        callbackContext.getResourceModelBuilder().targets(Translator.translateFromListTargetsByRuleResponse(awsResponse));
                    }

                    final ResourceModel model = callbackContext.getResourceModelBuilder().build();
                    model.setId(compositePID.getPid());

                    return ProgressEvent.defaultSuccessHandler(model);
                })
            );
    }

}
