package software.amazon.events.eventbus;

import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.CreateEventBusResponse;
import software.amazon.awssdk.services.eventbridge.model.DescribeEventBusResponse;
import software.amazon.cloudformation.Action;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.DelayFactory;
import software.amazon.cloudformation.proxy.HandlerRequest;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.RequestData;
import software.amazon.cloudformation.proxy.WaitStrategy;
import software.amazon.cloudformation.resource.Serializer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.mockito.Mock;
import org.mockito.Mockito;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.core.type.TypeReference;


public class AbstractTestBase {
    protected static final Credentials MOCK_CREDENTIALS;
    protected static final LoggerProxy logger;

    final protected static String TEST_EVENT_BUS_NAME = "eventbus1234";
    final protected static  String TEST_ARN = "arn";
    final protected static String TEST_POLICY = "dummyPolicy1";
    final protected static String TEST_POLICY2 = "dummyPolicy2";
    final protected static String TEST_KEY1 = "testKey1";
    final protected static String TEST_VALUE1 = "testValue1";

    final protected static String TEST_KEY2 = "testKey2";
    final protected static String TEST_VALUE2 = "testValue2";

    final protected static String TEST_KEY3 = "testKey2";
    final protected static String TEST_VALUE3 = "testValue2";

    final protected static String TEST_UPDATE_VALUE = "testValue1update";

    final protected static List<Tag> TEST_TAGS1 = new ArrayList<Tag>(){{
        add(Tag.builder().key(TEST_KEY1).value(TEST_UPDATE_VALUE).build());
        add(Tag.builder().key(TEST_KEY2).value(TEST_VALUE2).build());
    }};

    final protected static List<Tag> TEST_TAGS2 = new ArrayList<Tag>(){{
        add(Tag.builder().key(TEST_KEY1).value(TEST_VALUE1).build());
        add(Tag.builder().key(TEST_KEY3).value(TEST_VALUE3).build());
    }};
    @Mock
    protected EventBridgeClient eventBridgeClient = mock(EventBridgeClient.class);

    static {
        MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
        logger = new LoggerProxy();
    }

    protected final Serializer serializer = new Serializer();

    protected final HandlerWrapper handlerWrapper = new HandlerWrapper() {{
        this.mockClientProxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, DelayFactory.CONSTANT_DEFAULT_DELAY_FACTORY, WaitStrategy.scheduleForCallbackStrategy())
        {
            @SuppressWarnings("unchecked")
            @Override
            public <ClientT> ProxyClient<ClientT> newProxy(@Nonnull Supplier<ClientT> client) {
                return super.newProxy(() -> (ClientT)eventBridgeClient);
            }
        };
    }};

    protected final Context context = Mockito.mock(Context.class);
    protected final LambdaLogger lambdaLogger = mock(LambdaLogger.class);

    protected AbstractTestBase() {
        when(context.getLogger()).thenReturn(lambdaLogger);
    }


    static boolean checkCredentialsOverride(Optional<AwsRequestOverrideConfiguration> overrideConfiguration) {
        return overrideConfiguration.map(cfg -> cfg.credentialsProvider().map(credentialsProvider -> {
                            final AwsSessionCredentials creds = (AwsSessionCredentials) credentialsProvider.resolveCredentials();
                            return creds.accessKeyId().equals(MOCK_CREDENTIALS.getAccessKeyId()) &&
                                    creds.secretAccessKey().equals(MOCK_CREDENTIALS.getSecretAccessKey()) &&
                                    creds.sessionToken().equals(MOCK_CREDENTIALS.getSessionToken());
                        })
                        .orElse(false))
                .orElse(false);
    }

    static AwsServiceException createThrottleException() {
        return AwsServiceException.builder().awsErrorDetails(
                AwsErrorDetails.builder()
                        .errorCode("ThrottlingException")
                        .sdkHttpResponse(
                                SdkHttpResponse.builder()
                                        .statusCode(HttpStatusCode.THROTTLING)
                                        .build()
                        )
                        .build()
        ).build();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> makeCall(
            ResourceModel model,
            ResourceModel previousModel,
            Action action,
            boolean isCCAPI) throws Exception
    {
        return makeCall(model, previousModel, new CallbackContext(), action, isCCAPI);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> makeCall(
            ResourceModel model,
            ResourceModel previousModel,
            CallbackContext callbackContext,
            Action action,
            boolean isCCAPI) throws Exception
    {
        HandlerRequest<ResourceModel, CallbackContext, TypeConfigurationModel> request = prepareRequest(
                model,
                previousModel,
                callbackContext,
                AwsSessionCredentials.create(
                        MOCK_CREDENTIALS.getAccessKeyId(),
                        MOCK_CREDENTIALS.getSecretAccessKey(),
                        MOCK_CREDENTIALS.getSessionToken()),
                action,
                Region.US_EAST_2,
                isCCAPI
        );
        final InputStream stream = prepareStream(request);
        final ByteArrayOutputStream responseStream = new ByteArrayOutputStream(1024*4);
        handlerWrapper.handleRequest(stream, responseStream, context);

        return serializer.deserialize(
                responseStream.toString(StandardCharsets.UTF_8.name()),
                new TypeReference<ProgressEvent<ResourceModel, CallbackContext>>() {}
        );
    }

    protected HandlerRequest<ResourceModel, CallbackContext, TypeConfigurationModel> prepareRequest(
            ResourceModel model,
            ResourceModel previousModel,
            CallbackContext callbackContext,
            AwsSessionCredentials credentials,
            Action action,
            Region region,
            boolean isCCAPI)
    {
        HandlerRequest<ResourceModel, CallbackContext, TypeConfigurationModel> request = new HandlerRequest<>();
        request.setAction(action);
        request.setAwsAccountId("1234567891234");
        request.setBearerToken("dwezxdfgfgh");
        request.setNextToken(null);
        request.setRegion(region.id());
        request.setResourceType("AWS::IAM::ManagedPolicy");
        request.setStackId(isCCAPI ? null : UUID.randomUUID().toString());
        request.setCallbackContext(callbackContext);
        RequestData<ResourceModel, TypeConfigurationModel> data = new RequestData<>();
        data.setResourceProperties(model);
        data.setPreviousResourceProperties(previousModel);
        data.setCallerCredentials(new Credentials(credentials.accessKeyId(), credentials.secretAccessKey(), credentials.sessionToken()));
        request.setRequestData(data);
        return request;
    }

    protected InputStream prepareStream(HandlerRequest<ResourceModel, CallbackContext, TypeConfigurationModel> request)
            throws Exception {

        ByteArrayOutputStream out = new ByteArrayOutputStream(2048);
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);

        String value = serializer.serialize(request);
        writer.write(value);
        writer.flush();
        writer.close();

        return new ByteArrayInputStream(out.toByteArray());
    }


    static ResourceModel buildResourceModelWithAllProperties() {

        return ResourceModel.builder()
                .name(TEST_EVENT_BUS_NAME).policy(TEST_POLICY)
                .tags(TEST_TAGS1).build();
    }

    static CreateEventBusResponse buildCreateEventBusResponse() {
        return CreateEventBusResponse.builder()
                .eventBusArn(TEST_ARN)
                .build();
    }

    static DescribeEventBusResponse buildDescribeEventBusResponse() {
        return DescribeEventBusResponse.builder()
                .name(TEST_EVENT_BUS_NAME)
                .arn(TEST_ARN)
                .policy(TEST_POLICY)
                .build();
    }

    static ResourceModel buildNewResourceModel() {
        return ResourceModel.builder()
                .name(TEST_EVENT_BUS_NAME)
                .arn(TEST_ARN)
                .policy(TEST_POLICY)
                .tags(TEST_TAGS1)
                .build();
    }

    static ResourceModel buildPreviousResourceModel() {
        return ResourceModel.builder()
                .name(TEST_EVENT_BUS_NAME)
                .arn(TEST_ARN)
                .policy(TEST_POLICY2)
                .tags(TEST_TAGS2)
                .build();
    }

}
