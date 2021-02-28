package software.amazon.events.connection;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.ConnectionAuthorizationType;
import software.amazon.awssdk.services.eventbridge.model.DescribeConnectionRequest;
import software.amazon.awssdk.services.eventbridge.model.DescribeConnectionResponse;
import software.amazon.awssdk.services.eventbridge.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.ProxyClient;

public class Utils {

    private Utils() { }

    public static DescribeConnectionResponse readResource(DescribeConnectionRequest awsRequest, ProxyClient<EventBridgeClient> proxyClient) {
        DescribeConnectionResponse awsResponse;
        try {
            awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::describeConnection);
        } catch (final ResourceNotFoundException e) {
            // connection does not exist
            throw new CfnNotFoundException(e);
        } catch (final AwsServiceException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }

        return awsResponse;
    }

    public static void verifyAuthorizationTypeParameterMapping(ResourceModel model){
        final String type = model.getAuthorizationType();
        final AuthParameters parameters = model.getAuthParameters();

        switch (ConnectionAuthorizationType.valueOf(type)) {
            case BASIC:
                if (parameters.getBasicAuthParameters() == null) {
                    throw new CfnInvalidRequestException("BasicAuthParameters not provided for BASIC AuthorizationType.");
                }
                break;
            case API_KEY:
                if (parameters.getApiKeyAuthParameters() == null) {
                    throw new CfnInvalidRequestException("ApiKeyAuthParameters not provided for API_KEY AuthorizationType.");
                }
                break;
            case OAUTH_CLIENT_CREDENTIALS:
                if (parameters.getOAuthParameters() == null) {
                    throw new CfnInvalidRequestException("OAuthParameters not provided for OAUTH_CLIENT_CREDENTIALS AuthorizationType.");
                }
                break;
            default:
                throw new CfnGeneralServiceException(String.format("Couldn't create %s due to invalid authorization type: %s",
                        ResourceModel.TYPE_NAME, type));
        }

    }
}
