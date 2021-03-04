package software.amazon.events.connection;

import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.ListConnectionsRequest;
import software.amazon.awssdk.services.eventbridge.model.ListConnectionsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<EventBridgeClient> proxyClient,
            final Logger logger) {


        final ListConnectionsRequest awsRequest = Translator.translateToListRequest(request.getNextToken());
        ListConnectionsResponse awsResponse = proxy.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::listConnections);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(Translator.translateFromListRequest(awsResponse))
                .nextToken(awsResponse.nextToken())
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
