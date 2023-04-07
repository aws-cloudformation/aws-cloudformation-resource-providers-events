package software.amazon.events.rule;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.awssdk.services.cloudwatchevents.model.*;
import software.amazon.awssdk.services.cloudwatchevents.model.Target;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.events.rule.ResourceModel.ResourceModelBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<CloudWatchEventsClient> proxyClient;

    @Mock
    CloudWatchEventsClient sdkClient;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(CloudWatchEventsClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @AfterEach
    public void tear_down() {
        verify(sdkClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(sdkClient);
    }

    @ParameterizedTest
    @ValueSource(strings = {EVENT_RULE_ARN_DEFAULT_BUS, EVENT_RULE_ARN_CUSTOM_BUS, CROSS_ACCOUNT_EVENT_RULE_ARN_CUSTOM_BUS})
    public void handleRequest_SimpleSuccess(final String arn) {
        final CreateHandler handler = new CreateHandler();

        String eventPatternString = String.join("",
                "{",
                "  \"source\": [",
                "    \"aws.s3\"",
                "  ],",
                "  \"detail-type\": [",
                "    \"Object created\"",
                "  ],",
                "  \"detail\": {",
                "    \"bucket\": {",
                "      \"name\": [",
                "        \"testcdkstack-bucket43879c71-r2j3dsw4wp4z\"",
                "      ]",
                "    }",
                "  }",
                "}");

        Map<String, Object> eventMapperMap = null;
        try {
            eventMapperMap = MAPPER.readValue(eventPatternString, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // MODEL
        Set<software.amazon.events.rule.Target> targets = new HashSet<>();

        targets.add(software.amazon.events.rule.Target.builder()
                .id("TestLambdaFunctionId")
                .arn("arn:aws:lambda:us-west-2:123456789123:function:TestLambdaFunctionId")
                .build());

        // Final Models
        final ResourceModel model = ResourceModel.builder()
            .name(EVENT_RULE_NAME)
            .description("TestDescription")
            .eventPattern(eventMapperMap)
            .state("ENABLED")
            .targets(targets)
            .build();


        // MOCK
        /*
         * describeRule
         * putRule
         * describeRule
         * putTargets
         */

        Collection<Target> responseTargets = new ArrayList<>();
        for (software.amazon.events.rule.Target target : model.getTargets()) {
            responseTargets.add(Target.builder()
                    .id(target.getId())
                    .arn(target.getArn())
                    .build());
        }

        final DescribeRuleResponse describeRuleResponse = DescribeRuleResponse.builder()
                .name(model.getName())
                .description(model.getDescription())
                .eventPattern(eventPatternString)
                .state(model.getState())
                .arn(arn)
                .build();

        final PutRuleResponse putRuleResponse = PutRuleResponse.builder()
                .ruleArn(arn)
                .build();

        final PutTargetsResponse putTargetsResponse = PutTargetsResponse.builder()
                .build();

        when(proxyClient.client().describeRule(any(DescribeRuleRequest.class)))
                .thenThrow(ResourceNotFoundException.class)
                .thenThrow(ResourceNotFoundException.class)
                .thenReturn(describeRuleResponse);

        when(proxyClient.client().putRule(any(PutRuleRequest.class)))
                .thenReturn(putRuleResponse);

        when(proxyClient.client().putTargets(any(PutTargetsRequest.class)))
                .thenReturn(putTargetsResponse);

        // RUN
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(SOURCE_ACCOUNT_ID)
                .stackId(MOCK_STACK_ID)
                .clientRequestToken(MOCK_CLIENT_TOKEN)
                .logicalResourceIdentifier(MOCK_LOGICAL_ID)
                .desiredResourceState(model)
                .build();
        request.getDesiredResourceState().setArn(arn);

        CallbackContext context = new CallbackContext();
        ProgressEvent<ResourceModel, CallbackContext> response;

        response = handler.handleRequest(proxy, request, context, proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        response = handler.handleRequest(proxy, request, context, proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        response = handler.handleRequest(proxy, request, context, proxyClient, logger);

        // ASSERT
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_ScheduleExpression() {
        final CreateHandler handler = new CreateHandler();

        // MODEL

        Set<software.amazon.events.rule.Target> targets = new HashSet<>();

        targets.add(software.amazon.events.rule.Target.builder()
                .id("TestLambdaFunctionId")
                .arn("arn:aws:lambda:us-west-2:123456789123:function:TestLambdaFunctionId")
                .build());

        final ResourceModel model = ResourceModel.builder()
                .name(EVENT_RULE_NAME)
                .description("TestDescription")
                .scheduleExpression("rate(1 day)")
                .state("ENABLED")
                .targets(targets)
                .build();

        // MOCK

        /*
         * describeRule
         * putRule
         * describeRule
         * putTargets
         */

        Collection<Target> responseTargets = new ArrayList<>();
        for (software.amazon.events.rule.Target target : model.getTargets()) {
            responseTargets.add(Target.builder()
                    .id(target.getId())
                    .arn(target.getArn())
                    .build());
        }

        final DescribeRuleResponse describeRuleResponse = DescribeRuleResponse.builder()
                .name(model.getName())
                .description(model.getDescription())
                .scheduleExpression(model.getScheduleExpression())
                .state(model.getState())
                .arn(EVENT_RULE_ARN_DEFAULT_BUS)
                .build();

        final PutRuleResponse putRuleResponse = PutRuleResponse.builder()
                .ruleArn(EVENT_RULE_ARN_DEFAULT_BUS)
                .build();

        final PutTargetsResponse putTargetsResponse = PutTargetsResponse.builder()
                .build();

        when(proxyClient.client().describeRule(any(DescribeRuleRequest.class)))
                .thenThrow(ResourceNotFoundException.class)
                .thenThrow(ResourceNotFoundException.class)
                .thenReturn(describeRuleResponse);

        when(proxyClient.client().putRule(any(PutRuleRequest.class)))
                .thenReturn(putRuleResponse);

        when(proxyClient.client().putTargets(any(PutTargetsRequest.class)))
                .thenReturn(putTargetsResponse);

        // RUN
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(SOURCE_ACCOUNT_ID)
                .desiredResourceState(model)
                .build();
        request.getDesiredResourceState().setArn(EVENT_RULE_ARN_DEFAULT_BUS);

        CallbackContext context = new CallbackContext();
        ProgressEvent<ResourceModel, CallbackContext> response;

        response = handler.handleRequest(proxy, request, context, proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        response = handler.handleRequest(proxy, request, context, proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        response = handler.handleRequest(proxy, request, context, proxyClient, logger);

        // ASSERT
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_CreateRuleFail() {
        final CreateHandler handler = new CreateHandler();

        String eventPatternString = String.join("",
                "{",
                "  \"source\": [",
                "    \"aws.s3\"",
                "  ],",
                "  \"detail-type\": [",
                "    \"Object created\"",
                "  ],",
                "  \"detail\": {",
                "    \"bucket\": {",
                "      \"name\": [",
                "        \"testcdkstack-bucket43879c71-r2j3dsw4wp4z\"",
                "      ]",
                "    }",
                "  }",
                "}");

        Map<String, Object> eventMapperMap = null;
        try {
            eventMapperMap = MAPPER.readValue(eventPatternString, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // MODEL

        Set<software.amazon.events.rule.Target> targets = new HashSet<>();

        targets.add(software.amazon.events.rule.Target.builder()
                .id("TestLambdaFunctionId")
                .arn("arn:aws:lambda:us-west-2:123456789123:function:TestLambdaFunctionId")
                .build());

        final ResourceModel model = ResourceModel.builder()
                .name(EVENT_RULE_NAME)
                .description("TestDescription")
                .eventPattern(eventMapperMap)
                .state("ENABLED")
                .targets(targets)
                .build();

        // MOCK

        /*
         * describeRule
         * putRule
         * describeRule
         * putTargets
         * listTargetsByRule
         */

        final DescribeRuleResponse describeRuleResponse = DescribeRuleResponse.builder()
                .name(model.getName())
                .description(model.getDescription())
                .eventPattern(eventPatternString)
                .state(model.getState())
                .build();

        when(proxyClient.client().describeRule(any(DescribeRuleRequest.class)))
                .thenReturn(describeRuleResponse);

        // RUN

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(SOURCE_ACCOUNT_ID)
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);

        // ASSERT
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isEqualTo(EVENT_RULE_NAME + " already exists");
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AlreadyExists);
    }
}
