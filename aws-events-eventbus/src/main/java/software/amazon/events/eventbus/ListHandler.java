package software.amazon.events.eventbus;

import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.ListEventBusesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import com.google.common.annotations.VisibleForTesting;

public class ListHandler extends BaseHandlerStd {

    public ListHandler() {
        super();
    }

    @VisibleForTesting
    protected ListHandler(EventBridgeClient eventBridgeClient) {
        super(eventBridgeClient);
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<EventBridgeClient> proxyClient,
            final Logger logger) {


       logger.log(String.format("[StackId: %s, ClientRequestToken: %s] Calling List EventBus",
                request.getStackId(), request.getClientRequestToken()));

        ResourceModel model = request.getDesiredResourceState();
        return proxy.initiate("AWS-Events-EventBus::Read", proxyClient, model, callbackContext)
                .translateToServiceRequest(m -> Translator.translateToListRequest(request.getNextToken()))
                .makeServiceCall((listEventBusesRequest, client) -> {
                    ListEventBusesResponse listEventBusesResponse = client.injectCredentialsAndInvokeV2(listEventBusesRequest,
                            client.client()::listEventBuses);
                    return listEventBusesResponse;
                })
                .handleError(this::handleError)
                .done(((listEventBusesRequest, listEventBusesResponse, proxyClient1, resourceModel, callbackContext1) ->
                        ProgressEvent.<ResourceModel, CallbackContext>builder()
                                .resourceModels(Translator.translateFromListResponse(listEventBusesResponse))
                                .nextToken(listEventBusesResponse.nextToken())
                                .status(OperationStatus.SUCCESS)
                                .build()
                ));
    }
}
