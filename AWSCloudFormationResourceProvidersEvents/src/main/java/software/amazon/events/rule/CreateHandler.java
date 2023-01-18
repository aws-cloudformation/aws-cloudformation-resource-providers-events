package software.amazon.events.rule;

import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.awssdk.services.cloudwatchevents.model.*;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.Target;

import java.util.HashSet;

public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

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
                        throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, progress.getResourceModel().getName());
                    }
                    catch (ResourceNotFoundException e) {
                        // All goood...
                    }

                    logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                    return null;
                })
                .progress()
            )

            // STEP 2 [create/stabilize progress chain - required for resource creation]
            .then(progress ->
                proxy.initiate("AWS-Events-Rule::CreateRule", proxyClient,progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(model -> Translator.translateToPutRuleRequest(model, request.getDesiredResourceTags()))
                    .makeServiceCall((awsRequest, client) -> {

                        PutRuleResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::putRule);

                        logger.log(String.format("%s successfully created.", ResourceModel.TYPE_NAME));
                        return awsResponse;
                    })

                    // TODO
                    //.handleError()
                    //throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);

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

                        logger.log(String.format("%s [%s] has been stabilized.", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier()));
                        return stabilized;
                    })
                    .progress()
                )

            .then(progress ->
                proxy.initiate("AWS-Events-Rule::CreateTargets", proxyClient,progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToPutTargetsRequest)
                    .makeServiceCall(((awsRequest, client) -> {
                        PutTargetsResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::putTargets);

                        logger.log(String.format("%s successfully created.", "AWS::Events::Target"));
                        return awsResponse;
                    }))

                    // TODO
                     //.handleError()
                    //throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);

                    .stabilize((awsRequest, awsResponse, client, model, context) -> {

                        boolean stabilized;

                        try {
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
                        catch (ResourceNotFoundException e) {
                            stabilized = false;
                        }

                        logger.log(String.format("%s have been stabilized.", "AWS::Events::Target"));
                        return stabilized;
                    })
                    .progress()
                )

            // STEP 3 [TODO: describe call/chain to return the resource model]
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}
