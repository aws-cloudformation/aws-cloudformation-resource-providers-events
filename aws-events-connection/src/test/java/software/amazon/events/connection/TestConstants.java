package software.amazon.events.connection;

import software.amazon.awssdk.services.eventbridge.model.ConnectionBodyParameter;
import software.amazon.awssdk.services.eventbridge.model.ConnectionHeaderParameter;
import software.amazon.awssdk.services.eventbridge.model.ConnectionHttpParameters;
import software.amazon.awssdk.services.eventbridge.model.ConnectionOAuthClientResponseParameters;
import software.amazon.awssdk.services.eventbridge.model.ConnectionQueryStringParameter;

import java.time.Instant;
import java.util.Arrays;

public final class TestConstants {
    private TestConstants() {

    }

    public static final String CONNECTION_NAME = "TestConnection";
    public static final Instant CREATION_TIME = Instant.now();
    public static final Instant LAST_MODIFIED_TIME = Instant.now();
    public static final String USER_NAME = "BasicTest";
    public static final String API_KEY_NAME = "ApiKeyTest";
    public static final String AUTH_CLIENT_ID = "ID";

    public static final ConnectionOAuthClientResponseParameters sdkClientResponseParameters = ConnectionOAuthClientResponseParameters.builder()
            .clientID(AUTH_CLIENT_ID).build();

    public static final ConnectionHttpParameters invocationHttpParameters = ConnectionHttpParameters.builder()
            .headerParameters(ConnectionHeaderParameter.builder().key("Key").value("Value").build())
            .build();

    public static final HttpParameters modelInvocationHttpParameters = HttpParameters.builder()
            .headerParameters(Arrays.asList(Parameters.builder().key("Key").value("Value").build()))
            .build();

    public static final ConnectionHttpParameters oAuthHttpParameters = ConnectionHttpParameters.builder()
            .headerParameters(ConnectionHeaderParameter.builder().key("Key1").value("Value1").build())
            .bodyParameters(ConnectionBodyParameter.builder().key("Key2").value("Value2").build())
            .queryStringParameters(ConnectionQueryStringParameter.builder().key("Key3").value("Value3").build())
            .build();

    public static final HttpParameters modelOAuthHttpParameters = HttpParameters.builder()
            .headerParameters(Arrays.asList(Parameters.builder().key("Key1").value("Value1").build()))
            .bodyParameters(Arrays.asList(Parameters.builder().key("Key2").value("Value2").build()))
            .queryStringParameters(Arrays.asList(Parameters.builder().key("Key3").value("Value3").build()))
            .build();

    public static final AuthParameters authParametersBasicType = AuthParameters.builder()
            .basicAuthParameters(
                    BasicAuthParameters.builder()
                            .username(USER_NAME)
                            .build())
            .invocationHttpParameters(modelInvocationHttpParameters)
            .build();

    public static final AuthParameters authParametersApiKeyType = AuthParameters.builder()
            .apiKeyAuthParameters(
                    ApiKeyAuthParameters.builder()
                            .apiKeyName(API_KEY_NAME)
                            .build())
            .build();


    public static final AuthParameters authParametersOAuthType = AuthParameters.builder()
            .oAuthParameters(
                    OAuthParameters.builder()
                            .oAuthHttpParameters(modelOAuthHttpParameters)
                            .clientParameters(ClientParameters.builder().clientID(AUTH_CLIENT_ID).build())
                            .build())
            .build();

}
