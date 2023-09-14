package software.amazon.events.eventbus;

import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.CreateEventBusResponse;
import software.amazon.awssdk.services.eventbridge.model.PutPermissionResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static software.amazon.events.eventbus.Translator.getPolicy;

import java.util.Map;

import com.google.common.annotations.VisibleForTesting;


public class CreateHandler extends BaseHandlerStd {

    private Logger logger;



    public CreateHandler() {
        super();
    }

    @VisibleForTesting
    protected CreateHandler(EventBridgeClient eventBridgeClient) {
        super(eventBridgeClient);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<EventBridgeClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        logger.log(String.format("[StackId: %s, ClientRequestToken: %s] Calling Create EventBus",
                request.getStackId(), request.getClientRequestToken()));

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                // STEP 1 [create/stabilize progress chain - required for resource creation]
                .then(progress -> createEventBus(proxy, proxyClient, progress, request))
                .then(progress -> associatePolicy(proxy, proxyClient, progress))
                // STEP 2 [describe call/chain to return the resource model]
                .then(progress -> {
                    return ProgressEvent.defaultSuccessHandler(progress.getResourceModel());
                });
    }


    private ProgressEvent<ResourceModel, CallbackContext> createEventBus(AmazonWebServicesClientProxy proxy,
            ProxyClient<EventBridgeClient> proxyClient, ProgressEvent<ResourceModel, CallbackContext> progress,
            ResourceHandlerRequest<ResourceModel> request) {

        TagHelper tagHelper = new TagHelper();
        final Map<String, String> tagsToCreate = tagHelper.generateTagsForCreate(request.getDesiredResourceState(),
                request);


        ResourceModel model =  progress.getResourceModel();

        return proxy.initiate("AWS-Events-EventBus::Create", proxyClient, progress.getResourceModel(),
                        progress.getCallbackContext())
                .translateToServiceRequest(currentModel -> Translator.translateToCreateRequest(tagsToCreate, currentModel))
                .makeServiceCall((awsRequest, client) -> {
                    CreateEventBusResponse awsResponse =
                            client.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::createEventBus);

                    logger.log(String.format("%s with name %s successfully created.", ResourceModel.TYPE_NAME , model.getName()));
                    return awsResponse;
                })
                .stabilize((awsRequest, awsResponse, client, currentModel, context) -> stabilizeCreate(awsRequest, awsResponse,
                        proxyClient, currentModel, progress.getCallbackContext(), logger))
                .handleError(this::handleError)
                .done((awsRequest, awsResponse, client, _model, context) -> {
                    logger.log(String.format("Successfully created %s [%s]",ResourceModel.TYPE_NAME, model.getName()));
                    model.setArn(awsResponse.eventBusArn());
                    return ProgressEvent.progress(model, context);
                });
    }

    private ProgressEvent<ResourceModel, CallbackContext> associatePolicy(AmazonWebServicesClientProxy proxy, ProxyClient<EventBridgeClient> proxyClient, ProgressEvent<ResourceModel, CallbackContext> progress) {
        String policy = getPolicy(progress.getResourceModel());
        if (policy != null && !policy.isEmpty()) {
            return proxy.initiate("AWS-Events-EventBus::AssociatePolicy", proxyClient, progress.getResourceModel(),
                            progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToPutPermissionRequest)
                    .makeServiceCall((awsRequest, client) -> {
                        PutPermissionResponse awsResponse = client.injectCredentialsAndInvokeV2(awsRequest,
                                client.client()::putPermission);
                        return awsResponse;
                    })
                    .handleError(this::handleError)
                    .progress();

        } else {
            return ProgressEvent.progress(progress.getResourceModel(), progress.getCallbackContext());
        }
    };
}
