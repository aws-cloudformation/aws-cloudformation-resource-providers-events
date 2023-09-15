package software.amazon.events.eventbus;

import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.DescribeEventBusResponse;
import software.amazon.awssdk.services.eventbridge.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.eventbridge.model.ResourceNotFoundException;
import com.amazonaws.util.StringUtils;
import com.google.common.annotations.VisibleForTesting;

import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;


import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.stream.Collectors;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    public ReadHandler() {
        super();
    }

    @VisibleForTesting
    protected ReadHandler(EventBridgeClient eventBridgeClient) {
        super(eventBridgeClient);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<EventBridgeClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        logger.log(String.format("[StackId: %s, ClientRequestToken: %s] Calling Read EventBus",
                request.getStackId(), request.getClientRequestToken()));

        ResourceModel model = request.getDesiredResourceState();

        if (model == null || StringUtils.isNullOrEmpty(model.getName())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Primary Event Bus ID cannot be empty");
        }
        // STEP 1 [initialize a proxy context]
        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> getEventBus(proxy, proxyClient, progress, request))
                .then(progress -> listTags(proxy, proxyClient, progress));
    }


    protected ProgressEvent<ResourceModel, CallbackContext> getEventBus(AmazonWebServicesClientProxy proxy, ProxyClient<EventBridgeClient> proxyClient, ProgressEvent<ResourceModel, CallbackContext> progress,  ResourceHandlerRequest<ResourceModel> request) {

        CallbackContext callbackContext = progress.getCallbackContext();
        ResourceModel model = progress.getResourceModel();

        return proxy.initiate("AWS-Events-EventBus::Read", proxyClient, request.getDesiredResourceState(), callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> {
                    DescribeEventBusResponse awsResponse;
                    try {
                        awsResponse = client.injectCredentialsAndInvokeV2(awsRequest, client.client()::describeEventBus);
                    } catch (final ResourceNotFoundException e) {
                        callbackContext.setEventBusExists(false);
                        logger.log(String.format("%s with name %s not found.", ResourceModel.TYPE_NAME, model.getName()));
                        return null; // result not needed in done()
                    }
                    callbackContext.setEventBusExists(true);

                    logger.log(String.format("%s  with name %s has successfully been read.", ResourceModel.TYPE_NAME, model.getName()));
                    return awsResponse;
                })
                .handleError(this::handleError)
                .done(awsResponse -> {
                    if (callbackContext.isEventBusExists()) {
                        progress.getResourceModel().setArn(awsResponse.arn());
                        callbackContext.setResourceModelBuilder(Translator.translateFromReadResponse(awsResponse));

                        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext);
                    } else {
                        return ProgressEvent.failed(request.getDesiredResourceState(), callbackContext,
                                HandlerErrorCode.NotFound,
                                String.format("%s  with name %s not found", ResourceModel.TYPE_NAME, model.getName()));
                    }
                });
    }


    protected ProgressEvent<ResourceModel, CallbackContext> listTags(AmazonWebServicesClientProxy proxy, ProxyClient<EventBridgeClient> proxyClient, ProgressEvent<ResourceModel, CallbackContext> progress) {

        CallbackContext callbackContext = progress.getCallbackContext();
        ResourceModel model = progress.getResourceModel();

        return  proxy.initiate("AWS-Events-EventBus::ListTags", proxyClient, progress.getResourceModel(),
                        callbackContext)
                .translateToServiceRequest(Translator::translateToListTagsForResourceRequest)
                .makeServiceCall((awsRequest, client) -> {
                    ListTagsForResourceResponse awsResponse;

                    try {
                        awsResponse = client.injectCredentialsAndInvokeV2(awsRequest, client.client()::listTagsForResource);
                    } catch (final CfnAccessDeniedException e) {
                        logger.log(String.format("ListTags: %s not found but okay to continue", ResourceModel.TYPE_NAME));
                        return null;
                    }

                    logger.log(String.format("%s with name %s has successfully been read with ListTags.", ResourceModel.TYPE_NAME, model.getName()));
                    return awsResponse;
                })
                .handleError(this::handleError)
                .done(awsResponse -> {
                    if( awsResponse != null){
                        List<Tag> tags = awsResponse.tags().stream()
                                .filter(tag -> tag.key() != null)
                                .map(tag -> Tag.builder().key(tag.key()).value(tag.value()).build())
                                .collect(Collectors.toList());
                        logger.log(String.format("%s with name %s has successfully been read with %s numbers of ListTags.", ResourceModel.TYPE_NAME, model.getName(), tags.size()));
                        progress.getResourceModel().setTags(tags);

                        if (!tags.isEmpty()) {
                            callbackContext.getResourceModelBuilder().tags(tags);
                        }
                    }
                    return ProgressEvent.defaultSuccessHandler(callbackContext.getResourceModelBuilder().build());
                });
    }
}
