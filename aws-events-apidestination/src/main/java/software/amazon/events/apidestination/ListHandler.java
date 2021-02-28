package software.amazon.events.apidestination;

import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.ListApiDestinationsRequest;
import software.amazon.awssdk.services.eventbridge.model.ListApiDestinationsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ListHandler extends BaseHandlerStd {

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<EventBridgeClient> proxyClient,
            final Logger logger) {

        final ListApiDestinationsRequest awsRequest = Translator.translateToListRequest(request.getNextToken());
        ListApiDestinationsResponse awsResponse = proxy.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::listApiDestinations);

        String nextToken = awsResponse.nextToken();
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(Translator.translateFromList(awsResponse))
                .nextToken(nextToken)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
