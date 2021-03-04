package software.amazon.events.connection;

import software.amazon.awssdk.services.eventbridge.model.ConnectionApiKeyAuthResponseParameters;
import software.amazon.awssdk.services.eventbridge.model.ConnectionAuthResponseParameters;
import software.amazon.awssdk.services.eventbridge.model.ConnectionBasicAuthResponseParameters;
import software.amazon.awssdk.services.eventbridge.model.ConnectionBodyParameter;
import software.amazon.awssdk.services.eventbridge.model.ConnectionHeaderParameter;
import software.amazon.awssdk.services.eventbridge.model.ConnectionHttpParameters;
import software.amazon.awssdk.services.eventbridge.model.ConnectionOAuthClientResponseParameters;
import software.amazon.awssdk.services.eventbridge.model.ConnectionOAuthResponseParameters;
import software.amazon.awssdk.services.eventbridge.model.ConnectionQueryStringParameter;
import software.amazon.awssdk.services.eventbridge.model.CreateConnectionApiKeyAuthRequestParameters;
import software.amazon.awssdk.services.eventbridge.model.CreateConnectionAuthRequestParameters;
import software.amazon.awssdk.services.eventbridge.model.CreateConnectionBasicAuthRequestParameters;
import software.amazon.awssdk.services.eventbridge.model.CreateConnectionOAuthClientRequestParameters;
import software.amazon.awssdk.services.eventbridge.model.CreateConnectionOAuthRequestParameters;
import software.amazon.awssdk.services.eventbridge.model.CreateConnectionRequest;
import software.amazon.awssdk.services.eventbridge.model.DeleteConnectionRequest;
import software.amazon.awssdk.services.eventbridge.model.DescribeConnectionRequest;
import software.amazon.awssdk.services.eventbridge.model.DescribeConnectionResponse;
import software.amazon.awssdk.services.eventbridge.model.ListConnectionsRequest;
import software.amazon.awssdk.services.eventbridge.model.ListConnectionsResponse;
import software.amazon.awssdk.services.eventbridge.model.UpdateConnectionApiKeyAuthRequestParameters;
import software.amazon.awssdk.services.eventbridge.model.UpdateConnectionAuthRequestParameters;
import software.amazon.awssdk.services.eventbridge.model.UpdateConnectionBasicAuthRequestParameters;
import software.amazon.awssdk.services.eventbridge.model.UpdateConnectionOAuthClientRequestParameters;
import software.amazon.awssdk.services.eventbridge.model.UpdateConnectionOAuthRequestParameters;
import software.amazon.awssdk.services.eventbridge.model.UpdateConnectionRequest;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a centralized placeholder for
 * - api request construction
 * - object translation to/from aws sdk
 * - resource model construction for read/list handlers
 */

public class Translator {

    /**
     * Request to create a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to create a resource
     */
    static CreateConnectionRequest translateToCreateRequest(final ResourceModel model) {
        return CreateConnectionRequest.builder()
                .name(model.getName())
                .description(model.getDescription())
                .authParameters(translateToSdkCreateAuthRequestParameter(model.getAuthParameters()))
                .authorizationType(model.getAuthorizationType())
                .build();
    }

    /**
     * Request to read a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to describe a resource
     */
    static DescribeConnectionRequest translateToReadRequest(final ResourceModel model) {
        return DescribeConnectionRequest.builder()
                .name(model.getName())
                .build();
    }

    /**
     * Translates resource object from sdk into a resource model
     *
     * @param awsResponse the aws service describe resource response
     * @return model resource model
     */
    static ResourceModel translateFromReadResponse(final DescribeConnectionResponse awsResponse) {
        software.amazon.events.connection.ResourceModel resourceModel = software.amazon.events.connection.ResourceModel.builder()
                .name(awsResponse.name())
                .arn(awsResponse.connectionArn())
                .description(awsResponse.description())
                .authorizationType(awsResponse.authorizationTypeAsString())
                .authParameters(translateFromSdkToModelAuthParameter(awsResponse.authParameters()))
                .secretArn(awsResponse.secretArn())
                .build();

        return resourceModel;

    }


    /**
     * Request to delete a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to delete a resource
     */
    static DeleteConnectionRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteConnectionRequest.builder()
                .name(model.getName())
                .build();
    }


    /**
     * Request to update properties of a previously created resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to modify a resource
     */
    static UpdateConnectionRequest translateToUpdateRequest(final ResourceModel model) {
        return UpdateConnectionRequest.builder()
                .name(model.getName())
                .description(model.getDescription())
                .authParameters(translateToSdkUpdateAuthRequestParameter(model.getAuthParameters()))
                .authorizationType(model.getAuthorizationType())
                .build();
    }

    /**
     * Request to list resources
     *
     * @param nextToken token passed to the aws service list resources request
     * @return awsRequest the aws service request to list resources within aws account
     */
    static ListConnectionsRequest translateToListRequest(final String nextToken) {
        return ListConnectionsRequest.builder()
                .limit(50)
                .nextToken(nextToken)
                .build();
    }

    /**
     * Translates resource objects from sdk into a resource model (primary identifier only)
     *
     * @param awsResponse the aws service describe resource response
     * @return list of resource models
     */
    static List<ResourceModel> translateFromListRequest(final ListConnectionsResponse awsResponse) {
        return streamOfOrEmpty(awsResponse.connections())
                .map(resource -> ResourceModel.builder()
                        .name(resource.name())
                        .build())
                .collect(Collectors.toList());
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }

    private static CreateConnectionAuthRequestParameters translateToSdkCreateAuthRequestParameter(final AuthParameters authParameters) {
        BasicAuthParameters modelBasicAuthParameters = authParameters.getBasicAuthParameters();
        ApiKeyAuthParameters modelApiKeyAuthParameters = authParameters.getApiKeyAuthParameters();
        OAuthParameters modelOAuthParameters = authParameters.getOAuthParameters();
        software.amazon.events.connection.ConnectionHttpParameters modelInvocationHttpParameters = authParameters.getInvocationHttpParameters();

        CreateConnectionBasicAuthRequestParameters sdkBasicAuthParameters = translateToSdkCreateBasicAuthRequestParameters(modelBasicAuthParameters);
        CreateConnectionApiKeyAuthRequestParameters sdkApiKeyAuthParameters = translateToSdkCreateApiKeyAuthRequestParameters(modelApiKeyAuthParameters);
        CreateConnectionOAuthRequestParameters sdkOAuthRequestParameters = translateToSdkCreateOAuthRequestParameters(modelOAuthParameters);
        ConnectionHttpParameters connectionHttpParameters = translateToSdkConnectionHttpParameters(modelInvocationHttpParameters);

        CreateConnectionAuthRequestParameters authRequestParameters =
                CreateConnectionAuthRequestParameters.builder()
                        .basicAuthParameters(sdkBasicAuthParameters)
                        .apiKeyAuthParameters(sdkApiKeyAuthParameters)
                        .oAuthParameters(sdkOAuthRequestParameters)
                        .invocationHttpParameters(connectionHttpParameters)
                        .build();

        return authRequestParameters;
    }

    private static UpdateConnectionAuthRequestParameters translateToSdkUpdateAuthRequestParameter(final AuthParameters authParameters) {
        BasicAuthParameters modelBasicAuthParameters = authParameters.getBasicAuthParameters();
        ApiKeyAuthParameters modelApiKeyAuthParameters = authParameters.getApiKeyAuthParameters();
        OAuthParameters modelOAuthParameters = authParameters.getOAuthParameters();
        software.amazon.events.connection.ConnectionHttpParameters modelInvocationHttpParameters = authParameters.getInvocationHttpParameters();

        UpdateConnectionBasicAuthRequestParameters sdkBasicAuthParameters = translateToSdkUpdateBasicAuthRequestParameters(modelBasicAuthParameters);
        UpdateConnectionApiKeyAuthRequestParameters sdkApiKeyAuthParameters = translateToSdkUpdateApiKeyAuthRequestParameters(modelApiKeyAuthParameters);
        UpdateConnectionOAuthRequestParameters sdkOAuthRequestParameters = translateToSdkUpdateOAuthRequestParameters(modelOAuthParameters);
        ConnectionHttpParameters connectionHttpParameters = translateToSdkConnectionHttpParameters(modelInvocationHttpParameters);

        UpdateConnectionAuthRequestParameters authRequestParameters =
                UpdateConnectionAuthRequestParameters.builder()
                        .basicAuthParameters(sdkBasicAuthParameters)
                        .apiKeyAuthParameters(sdkApiKeyAuthParameters)
                        .oAuthParameters(sdkOAuthRequestParameters)
                        .invocationHttpParameters(connectionHttpParameters)
                        .build();

        return authRequestParameters;
    }

    private static ConnectionHttpParameters translateToSdkConnectionHttpParameters(software.amazon.events.connection.ConnectionHttpParameters httpParameters) {
        if (httpParameters != null) {
            List<ConnectionHeaderParameter> headerParameters = ListConverter.toSdk(httpParameters.getHeaderParameters(), x -> ConnectionHeaderParameter.builder().key(x.getKey()).value(x.getValue()).isValueSecret(isSecret(x.getIsValueSecret())).build());
            List<ConnectionBodyParameter> bodyParameters = ListConverter.toSdk(httpParameters.getBodyParameters(), x -> ConnectionBodyParameter.builder().key(x.getKey()).value(x.getValue()).isValueSecret(isSecret(x.getIsValueSecret())).build());
            List<ConnectionQueryStringParameter> queryStringParameters = ListConverter.toSdk(httpParameters.getQueryStringParameters(), x -> ConnectionQueryStringParameter.builder().key(x.getKey()).value(x.getValue()).isValueSecret(isSecret(x.getIsValueSecret())).build());

            ConnectionHttpParameters connectionHttpParameters = ConnectionHttpParameters.builder()
                    .headerParameters(headerParameters)
                    .bodyParameters(bodyParameters)
                    .queryStringParameters(queryStringParameters)
                    .build();

            return connectionHttpParameters;
        }

        return null;
    }

    public static CreateConnectionBasicAuthRequestParameters translateToSdkCreateBasicAuthRequestParameters(final BasicAuthParameters params) {
        if (params != null) {
            CreateConnectionBasicAuthRequestParameters parameters = CreateConnectionBasicAuthRequestParameters.builder()
                    .username(params.getUsername())
                    .password(params.getPassword())
                    .build();

            return parameters;
        }
        return null;
    }

    private static CreateConnectionApiKeyAuthRequestParameters translateToSdkCreateApiKeyAuthRequestParameters(final ApiKeyAuthParameters params) {
        if (params != null) {
            CreateConnectionApiKeyAuthRequestParameters apiKeyParameters = CreateConnectionApiKeyAuthRequestParameters.builder()
                    .apiKeyName(params.getApiKeyName())
                    .apiKeyValue(params.getApiKeyValue())
                    .build();

            return apiKeyParameters;
        }

        return null;
    }

    private static CreateConnectionOAuthRequestParameters translateToSdkCreateOAuthRequestParameters(final OAuthParameters params) {
        if (params != null) {
            CreateConnectionOAuthRequestParameters parameters = CreateConnectionOAuthRequestParameters.builder()
                    .authorizationEndpoint(params.getAuthorizationEndpoint())
                    .httpMethod(params.getHttpMethod())
                    .clientParameters(translateToSdkCreateOAuthClientRequestParameter(params.getClientParameters()))
                    .oAuthHttpParameters(translateToSdkConnectionHttpParameters(params.getOAuthHttpParameters()))
                    .build();

            return parameters;
        }

        return null;
    }

    private static CreateConnectionOAuthClientRequestParameters translateToSdkCreateOAuthClientRequestParameter(final ClientParameters params) {
        if (params != null) {
            CreateConnectionOAuthClientRequestParameters parameters = CreateConnectionOAuthClientRequestParameters.builder()
                    .clientID(params.getClientID())
                    .clientSecret(params.getClientSecret())
                    .build();

            return parameters;
        }

        return null;
    }


    private static UpdateConnectionBasicAuthRequestParameters translateToSdkUpdateBasicAuthRequestParameters(final BasicAuthParameters params) {
        if (params != null) {
            UpdateConnectionBasicAuthRequestParameters parameters = UpdateConnectionBasicAuthRequestParameters.builder()
                    .username(params.getUsername())
                    .password(params.getPassword())
                    .build();

            return parameters;
        }
        return null;
    }

    private static UpdateConnectionApiKeyAuthRequestParameters translateToSdkUpdateApiKeyAuthRequestParameters(final ApiKeyAuthParameters params) {
        if (params != null) {
            UpdateConnectionApiKeyAuthRequestParameters apiKeyParameters = UpdateConnectionApiKeyAuthRequestParameters.builder()
                    .apiKeyName(params.getApiKeyName())
                    .apiKeyValue(params.getApiKeyValue())
                    .build();

            return apiKeyParameters;
        }

        return null;
    }

    private static UpdateConnectionOAuthRequestParameters translateToSdkUpdateOAuthRequestParameters(final OAuthParameters params) {
        if (params != null) {
            UpdateConnectionOAuthRequestParameters parameters = UpdateConnectionOAuthRequestParameters.builder()
                    .authorizationEndpoint(params.getAuthorizationEndpoint())
                    .httpMethod(params.getHttpMethod())
                    .clientParameters(translateToSdkUpdateOAuthClientRequestParameter(params.getClientParameters()))
                    .oAuthHttpParameters(translateToSdkConnectionHttpParameters(params.getOAuthHttpParameters()))
                    .build();

            return parameters;
        }

        return null;
    }

    private static UpdateConnectionOAuthClientRequestParameters translateToSdkUpdateOAuthClientRequestParameter(final ClientParameters params) {
        if (params != null) {
            UpdateConnectionOAuthClientRequestParameters parameters = UpdateConnectionOAuthClientRequestParameters.builder()
                    .clientID(params.getClientID())
                    .clientSecret(params.getClientSecret())
                    .build();

            return parameters;
        }

        return null;
    }

    private static final AuthParameters translateFromSdkToModelAuthParameter(ConnectionAuthResponseParameters params) {
        if (params != null) {
            AuthParameters authParameters = AuthParameters.builder()
                    .basicAuthParameters(translateFromSdkToModelBasicAuthParameter(params.basicAuthParameters()))
                    .apiKeyAuthParameters(translateFromSdkToModelApiKeyAuthParameter(params.apiKeyAuthParameters()))
                    .oAuthParameters(translateFromSdkToModelOAuthParameter(params.oAuthParameters()))
                    .invocationHttpParameters(translateFromSdkToModelHttpParameters(params.invocationHttpParameters()))
                    .build();

            return authParameters;
        }

        return null;
    }

    private static final BasicAuthParameters translateFromSdkToModelBasicAuthParameter(ConnectionBasicAuthResponseParameters params) {
        if (params != null) {
            BasicAuthParameters basicAuthParameters = BasicAuthParameters
                    .builder()
                    .username(params.username())
                    .build();
            return basicAuthParameters;
        }

        return null;
    }

    private static final ApiKeyAuthParameters translateFromSdkToModelApiKeyAuthParameter(ConnectionApiKeyAuthResponseParameters params) {
        if (params != null) {
            ApiKeyAuthParameters apiKeyAuthParameters = ApiKeyAuthParameters.builder()
                    .apiKeyName(params.apiKeyName())
                    .build();
            return apiKeyAuthParameters;
        }

        return null;
    }

    private static final OAuthParameters translateFromSdkToModelOAuthParameter(ConnectionOAuthResponseParameters params) {
        if (params != null) {
            OAuthParameters oAuthParameters = OAuthParameters.builder()
                    .httpMethod(params.httpMethodAsString())
                    .authorizationEndpoint(params.authorizationEndpoint())
                    .clientParameters(translateFromSdkToModelClientParameters(params.clientParameters()))
                    .oAuthHttpParameters(translateFromSdkToModelHttpParameters(params.oAuthHttpParameters()))
                    .build();
            return oAuthParameters;
        }

        return null;
    }

    private static ClientParameters translateFromSdkToModelClientParameters(final ConnectionOAuthClientResponseParameters params) {
        if (params != null) {
            ClientParameters parameters = ClientParameters.builder()
                    .clientID(params.clientID())
                    .build();

            return parameters;
        }

        return null;
    }

    private static software.amazon.events.connection.ConnectionHttpParameters translateFromSdkToModelHttpParameters(final ConnectionHttpParameters params) {
        if (params != null) {
            List<Parameter> headerParameters = ListConverter.toModel(params.headerParameters(), x -> Parameter.builder().key(x.key()).value(x.value()).isValueSecret(x.isValueSecret()).build());
            List<Parameter> bodyParameters = ListConverter.toModel(params.bodyParameters(), x -> Parameter.builder().key(x.key()).value(x.value()).isValueSecret(x.isValueSecret()).build());
            List<Parameter> queryStringParameters = ListConverter.toModel(params.queryStringParameters(), x -> Parameter.builder().key(x.key()).value(x.value()).isValueSecret(x.isValueSecret()).build());

            software.amazon.events.connection.ConnectionHttpParameters  httpParameters = software.amazon.events.connection.ConnectionHttpParameters.builder()
                    .headerParameters(headerParameters)
                    .bodyParameters(bodyParameters)
                    .queryStringParameters(queryStringParameters)
                    .build();

            return httpParameters;
        }

        return null;
    }

    private static Boolean isSecret( Boolean isValueSecret){
        //by default all values are secret
        return isValueSecret == null ? true : isValueSecret;
    }
}
