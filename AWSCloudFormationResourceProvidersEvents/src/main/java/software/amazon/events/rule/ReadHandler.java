package software.amazon.events.rule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.awssdk.services.cloudwatchevents.model.DescribeRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.ListTargetsByRuleResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;
    private static final ObjectMapper MAPPER = new ObjectMapper();


    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudWatchEventsClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();
        AtomicReference<ResourceModel.ResourceModelBuilder> finalResourceModel = new AtomicReference<>();

        return ProgressEvent.progress(model, callbackContext)

            // STEP 1 [Read Rule]
            .then(progress -> proxy.initiate("AWS-Events-Rule::ReadRule", proxyClient, request.getDesiredResourceState(), callbackContext)
                .translateToServiceRequest(Translator::translateToDescribeRuleRequest)
                .makeServiceCall((awsRequest, client) -> {

                    DescribeRuleResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::describeRule);

                    logger.log(String.format("StackId: %s: %s [%s] has successfully been read.", request.getStackId(), ResourceModel.TYPE_NAME, awsRequest.name()));
                    return awsResponse;
                })
                .handleError(this::handleError )
                .done(awsResponse -> {

                    finalResourceModel.set(Translator.translateFromReadResponse(awsResponse));

                    return ProgressEvent.progress(model, callbackContext);
                })
            )

            // STEP 2 [List Targets]
            .then(progress -> proxy.initiate("AWS-Events-Rule::ListTargets", proxyClient, request.getDesiredResourceState(), callbackContext)
                .translateToServiceRequest(Translator::translateToListTargetsByRuleRequest)
                .makeServiceCall((awsRequest, client) -> {
                    ListTargetsByRuleResponse awsResponse = null;
                    awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::listTargetsByRule);

                    logger.log(String.format("StackId: %s: %s [%s] has successfully been read.", request.getStackId(), "AWS::Events::Target", awsResponse.targets().size()));
                    return awsResponse;
                })

                .handleError(this::handleError)

                // Add the list of Targets to the Response
                .done(awsResponse -> {

                    if (awsResponse.hasTargets()) {
                        finalResourceModel.get().targets(Translator.translateFromResponseToTargets(awsResponse));
                    }

                    return ProgressEvent.defaultSuccessHandler(finalResourceModel.get().build());
                })
            );
    }
}
