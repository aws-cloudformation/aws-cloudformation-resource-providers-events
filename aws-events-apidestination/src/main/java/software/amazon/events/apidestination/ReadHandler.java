package software.amazon.events.apidestination;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.DescribeApiDestinationRequest;
import software.amazon.awssdk.services.eventbridge.model.DescribeApiDestinationResponse;
import software.amazon.awssdk.services.eventbridge.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<EventBridgeClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();
        logger.log(String.format("Resource module: %s", model.toString()));

        return proxy.initiate("AWS-Events-ApiDestination::Read", proxyClient, model, callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall(this::readResource)
                .done(this::constructResourceModelFromResponse);
    }

    private ProgressEvent<ResourceModel, CallbackContext> constructResourceModelFromResponse(DescribeApiDestinationResponse awsResponse) {
        ResourceModel resourceModel = Translator.translateFromReadResponse(awsResponse);

        return ProgressEvent.defaultSuccessHandler(resourceModel);
    }

    private DescribeApiDestinationResponse readResource(DescribeApiDestinationRequest awsRequest, ProxyClient<EventBridgeClient> proxyClient) {
        DescribeApiDestinationResponse awsResponse;
        try {
            awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::describeApiDestination);
        } catch (final ResourceNotFoundException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, awsRequest.name(), e);
        } catch (final AwsServiceException e) {
            throw new CfnGeneralServiceException(software.amazon.events.apidestination.ResourceModel.TYPE_NAME, e);
        }
        logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }
}
