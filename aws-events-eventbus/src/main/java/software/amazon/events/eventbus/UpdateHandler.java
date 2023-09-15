package software.amazon.events.eventbus;

import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.DescribeEventBusResponse;
import software.amazon.awssdk.services.eventbridge.model.PutPermissionResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static software.amazon.events.eventbus.Translator.getPolicy;

import java.util.Map;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    public UpdateHandler() {
        super();
    }

    @VisibleForTesting
    protected UpdateHandler(EventBridgeClient eventBridgeClient) {
        super(eventBridgeClient);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<EventBridgeClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        logger.log(String.format("[StackId: %s, ClientRequestToken: %s] Calling Update EventBus",
                request.getStackId(), request.getClientRequestToken()));

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)

                // STEP 1 [check if resource already exists]
                .then(progress -> getEventBus(proxy, proxyClient, progress, request))
                // STEP 2 [update tags]
                .then(progress -> updateTags(proxy, proxyClient, progress, request))
                // STEP 3 [second update/stabilize progress chain - update policy]
                .then(progress -> updatePolicy(proxy, proxyClient, progress, request))
                // STEP 4 [describe call/chain to return the resource model]
                .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));
    }


    protected ProgressEvent<ResourceModel, CallbackContext> getEventBus(AmazonWebServicesClientProxy proxy, ProxyClient<EventBridgeClient> proxyClient, ProgressEvent<ResourceModel, CallbackContext> progress,  ResourceHandlerRequest<ResourceModel> request) {

        CallbackContext callbackContext = progress.getCallbackContext();
        ResourceModel model = progress.getResourceModel();

        return proxy.initiate("AWS-Events-EventBus::Update::PreUpdateCheck", proxyClient, model, progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> {
                    DescribeEventBusResponse awsResponse =
                            client.injectCredentialsAndInvokeV2(awsRequest, client.client()::describeEventBus);

                    logger.log(String.format("%s with name %s has successfully been read.", ResourceModel.TYPE_NAME, model.getName()));
                    return awsResponse;
                })
                .handleError(this::handleError)
                .done(awsResponse -> {
                    progress.getResourceModel().setArn(awsResponse.arn());
                    return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext);
                });
    }


    protected ProgressEvent<ResourceModel, CallbackContext> updateTags(AmazonWebServicesClientProxy proxy, ProxyClient<EventBridgeClient> proxyClient, ProgressEvent<ResourceModel, CallbackContext> progress,  ResourceHandlerRequest<ResourceModel> request) {

        CallbackContext callbackContext = progress.getCallbackContext();
        ResourceModel model = progress.getResourceModel();

        final TagHelper tagHelper = new TagHelper();
        ProgressEvent<ResourceModel, CallbackContext> progressEvent = ProgressEvent.progress(model, callbackContext);

        if (tagHelper.shouldUpdateTags(request)) {
            final Map<String, String> previousTags = tagHelper.getPreviouslyAttachedTags(request);
            final Map<String, String> desiredTags = tagHelper.getNewDesiredTags(request);
            final Set<String> tagsToRemove = tagHelper.generateTagsToRemove(previousTags, desiredTags);
            final Map<String, String> tagsToAdd = tagHelper.generateTagsToAdd(previousTags, desiredTags);

            logger.log(String.format("update tags for %s with name %s been started.", ResourceModel.TYPE_NAME, model.getName()));

            if (tagsToRemove != null && !tagsToRemove.isEmpty()) {
                logger.log(String.format("removing tags for %s with name %s.", ResourceModel.TYPE_NAME, model.getName()));
                progressEvent = progressEvent.then(
                        progress1 -> tagHelper.untagResource(proxy, proxyClient, model, request,
                                callbackContext, tagsToRemove, logger)
                );
            }

            if (tagsToAdd != null && !tagsToAdd.isEmpty()) {
                logger.log(String.format("adding tags for %s with name %s.", ResourceModel.TYPE_NAME, model.getName()));
                progressEvent = progressEvent.then(
                        progressEvent1 -> tagHelper.tagResource(proxy, proxyClient, model, request,
                                callbackContext, tagsToAdd, logger)
                );
            }
        }
        return progressEvent;
    }

    protected ProgressEvent<ResourceModel, CallbackContext> updatePolicy(AmazonWebServicesClientProxy proxy, ProxyClient<EventBridgeClient> proxyClient, ProgressEvent<ResourceModel, CallbackContext> progress,  ResourceHandlerRequest<ResourceModel> request) {
        ResourceModel model = progress.getResourceModel();
        String policy = getPolicy(model);
        if (policy != null && !policy.isEmpty()) {
            return proxy.initiate("AWS-Events-EventBus::Update::updatePolicy",
                            proxyClient, model, progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToPutPermissionRequest)
                    .makeServiceCall((awsRequest, client) -> {
                        PutPermissionResponse awsResponse = client.injectCredentialsAndInvokeV2(awsRequest, client.client()::putPermission);
                        logger.log(String.format("%s with name %s has successfully been updated.", ResourceModel.TYPE_NAME, model.getName()));
                        return awsResponse;
                    })
                    .handleError(this::handleError)
                    .progress();
        } else {
            return ProgressEvent.progress(progress.getResourceModel(), progress.getCallbackContext());
        }
    }
}
