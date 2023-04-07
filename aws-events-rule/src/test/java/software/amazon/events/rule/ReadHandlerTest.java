package software.amazon.events.rule;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.awssdk.services.cloudwatchevents.model.DescribeRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.DescribeRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.ListTargetsByRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.ListTargetsByRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.PutTargetsRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.ResourceNotFoundException;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

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

    @Test
    public void handleRequest_SimpleSuccess() {
        final ReadHandler handler = new ReadHandler();

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

        Map<String, Object> eventMapperMap;
        try {
            eventMapperMap = MAPPER.readValue(eventPatternString, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // MODEL

        Set<Target> targets = new HashSet<>();

        targets.add(software.amazon.events.rule.Target.builder()
                .id("TestLambdaFunctionId")
                .arn("arn:aws:lambda:us-west-2:123456789123:function:TestLambdaFunctionId")
                .build());

        final ResourceModel expectedModel = ResourceModel.builder()
                .arn(EVENT_RULE_ARN_DEFAULT_BUS)
                .id(EVENT_RULE_NAME)
                .description("TestDescription")
                .eventPattern(eventMapperMap)
                .state("ENABLED")
                .targets(targets)
                .build();


        final DescribeRuleResponse describeRuleResponse = DescribeRuleResponse.builder()
                .arn(expectedModel.getArn())
                .description(expectedModel.getDescription())
                .eventPattern(eventPatternString)
                .state(expectedModel.getState())
                .build();
        // MOCK

        /*
         * describeRule
         * listTargetsByRule
         */

        Collection<software.amazon.awssdk.services.cloudwatchevents.model.Target> responseTargets = new ArrayList<>();
        for (software.amazon.events.rule.Target target : targets) {
            responseTargets.add(software.amazon.awssdk.services.cloudwatchevents.model.Target.builder()
                    .id(target.getId())
                    .arn(target.getArn())
                    .build());
        }

        final ListTargetsByRuleResponse listTargetsByRuleResponse = ListTargetsByRuleResponse.builder()
                .targets(responseTargets)
                .build();

        when(proxyClient.client().describeRule(any(DescribeRuleRequest.class)))
                .thenReturn(describeRuleResponse);

        when(proxyClient.client().listTargetsByRule(any(ListTargetsByRuleRequest.class)))
                .thenReturn(listTargetsByRuleResponse);

        // RUN

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(SOURCE_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder().arn(EVENT_RULE_ARN_DEFAULT_BUS).build())
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);

        // ASSERT
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_NoTargets() {
        final ReadHandler handler = new ReadHandler();

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

        Map<String, Object> eventMapperMap;
        try {
            eventMapperMap = MAPPER.readValue(eventPatternString, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // MODEL

        final ResourceModel model = ResourceModel.builder()
                .arn(EVENT_RULE_ARN_DEFAULT_BUS)
                .build();

        final ResourceModel resultModel = ResourceModel.builder()
                .id(EVENT_RULE_NAME)
                .name(model.getName())
                .description("TestDescription")
                .eventPattern(eventMapperMap)
                .state("ENABLED")
                .build();

        // MOCK

        /*
         * describeRule
         * listTargetsByRule
         */

        final DescribeRuleResponse describeRuleResponse = DescribeRuleResponse.builder()
                .name(resultModel.getName())
                .description(resultModel.getDescription())
                .eventPattern(eventPatternString)
                .state(resultModel.getState())
                .build();

        final ListTargetsByRuleResponse listTargetsByRuleResponse = ListTargetsByRuleResponse.builder()
                .build();

        when(proxyClient.client().describeRule(any(DescribeRuleRequest.class)))
                .thenReturn(describeRuleResponse);

        when(proxyClient.client().listTargetsByRule(any(ListTargetsByRuleRequest.class)))
                .thenReturn(listTargetsByRuleResponse);

        // RUN

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(SOURCE_ACCOUNT_ID)
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);

        // ASSERT
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(resultModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_NotFound() {
        final ReadHandler handler = new ReadHandler();

        // MODEL

        final ResourceModel model = ResourceModel.builder()
                .arn(EVENT_RULE_ARN_DEFAULT_BUS)
                .build();

        // MOCK

        /*
         * describeRule
         * listTargetsByRule
         */

        when(proxyClient.client().describeRule(any(DescribeRuleRequest.class)))
                .thenThrow(ResourceNotFoundException.class);

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
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    }

    @Test
    public void handleRequest_TranslatorTest() {
        final ReadHandler handler = new ReadHandler();

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

        Map<String, Object> eventMapperMap;
        try {
            eventMapperMap = MAPPER.readValue(eventPatternString, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // MODEL

        Set<software.amazon.events.rule.Target> targets = new HashSet<>();

        Set<String> securityGroups = new HashSet<>();
        Set<String> subnets = new HashSet<>();
        Map<String, String> headerParameters = new HashMap<>();
        Set<String> pathParameterValues = new HashSet<>();
        Map<String, String> queryStringParameters = new HashMap<>();
        Map<String, String> inputPathsMap = new HashMap<>();
        Set<software.amazon.events.rule.RunCommandTarget> runCommandTargets = new HashSet<>();
        Set<String> runCommandTargetsValues = new HashSet<>();
        Set<software.amazon.events.rule.SageMakerPipelineParameter> sageMakerPipelineParameters = new HashSet<>();
        Set<software.amazon.events.rule.Tag> tags = new HashSet<>();
        Set<software.amazon.events.rule.PlacementStrategy> placementStrategies = new HashSet<>();
        Set<software.amazon.events.rule.PlacementConstraint> placementConstraints = new HashSet<>();
        Set<software.amazon.events.rule.CapacityProviderStrategyItem> capacityProviderStrategy = new HashSet<>();

        securityGroups.add("SECURITY_GROUP");
        subnets.add("SUBNET");
        headerParameters.put("HEADER_PARAMETER_KEY", "HEADER_PARAMETER_VALUE");
        pathParameterValues.add("PATH_PARAMETER_VALUE");
        queryStringParameters.put("QUERY_STRING_PARAMETER_KEY", "QUERY_STRING_PARAMETER_VALUE");
        inputPathsMap.put("INPUT_PATH_KEY", "INPUT_PATH_VALUE");
        runCommandTargetsValues.add("RUN_COMMAND_TARGETS_VALUE");
        runCommandTargets.add(software.amazon.events.rule.RunCommandTarget.builder()
                .key("RUN_COMMAND_TARGET_KEY")
                .values(runCommandTargetsValues)
                .build());
        sageMakerPipelineParameters.add(SageMakerPipelineParameter.builder()
                .name("SAGEMAKER_PIPELINE_PARAMETER_NAME")
                .value("SAGEMAKER_PIPELINE_PARAMETER_VALUE")
                .build());
        tags.add(software.amazon.events.rule.Tag.builder()
                .key("TAG_KEY")
                .value("TAG_VALUE")
                .build());
        placementStrategies.add(software.amazon.events.rule.PlacementStrategy.builder()
                .field("PLACEMENT_STRATEGY_FIELD")
                .type("PLACEMENT_STRATEGY_TYPE")
                .build());
        placementConstraints.add(software.amazon.events.rule.PlacementConstraint.builder()
                .expression("PLACEMENT_CONSTRAINT_EXPRESSION")
                .type("PLACEMENT_CONSTRAINT_TYPE")
                .build());
        capacityProviderStrategy.add(software.amazon.events.rule.CapacityProviderStrategyItem.builder()
                .base(1)
                .capacityProvider("CAPACITY_PROVIDER_STRATEGY_CAPACITY")
                .weight(1)
                .build());

        targets.add(software.amazon.events.rule.Target.builder()
                .id("TestLambdaFunctionId")
                .arn("arn:aws:lambda:us-west-2:123456789123:function:TestLambdaFunctionId")
                .batchParameters(software.amazon.events.rule.BatchParameters.builder()
                        .arrayProperties(software.amazon.events.rule.BatchArrayProperties.builder()
                                .size(1)
                                .build())
                        .retryStrategy(software.amazon.events.rule.BatchRetryStrategy.builder()
                                .attempts(1)
                                .build())
                        .build())
                .deadLetterConfig(software.amazon.events.rule.DeadLetterConfig.builder()
                        .arn("ARN")
                        .build())
                .ecsParameters(software.amazon.events.rule.EcsParameters.builder()
                        .networkConfiguration(software.amazon.events.rule.NetworkConfiguration.builder()
                                .awsVpcConfiguration(software.amazon.events.rule.AwsVpcConfiguration.builder()
                                        .assignPublicIp("UNKNOWN_TO_SDK_VERSION")
                                        .securityGroups(securityGroups)
                                        .subnets(subnets)
                                        .build())
                                .build())
                        .group("GROUP")
                        .launchType("UNKNOWN_TO_SDK_VERSION")
                        .platformVersion("PLATFORM_VERSION")
                        .taskCount(1)
                        .taskDefinitionArn("TASK_DEFINITION_ARN")
                        .tagList(tags)
                        .placementStrategies(placementStrategies)
                        .placementConstraints(placementConstraints)
                        .capacityProviderStrategy(capacityProviderStrategy)
                        .build())
                .httpParameters(software.amazon.events.rule.HttpParameters.builder()
                        .headerParameters(headerParameters)
                        .pathParameterValues(pathParameterValues)
                        .queryStringParameters(queryStringParameters)
                        .build())
                .inputTransformer(software.amazon.events.rule.InputTransformer.builder()
                        .inputPathsMap(inputPathsMap)
                        .inputTemplate("INPUT_TEMPLATE")
                        .build())
                .kinesisParameters(software.amazon.events.rule.KinesisParameters.builder()
                        .partitionKeyPath("PARTITION_KEY_PATH")
                        .build())
                .redshiftDataParameters(software.amazon.events.rule.RedshiftDataParameters.builder()
                        .database("DATABASE")
                        .dbUser("DB_USER")
                        .secretManagerArn("SECRET_MANAGER_ARN")
                        .sql("SQL")
                        .statementName("STATEMENT_NAME")
                        .withEvent(true)
                        .build())
                .retryPolicy(software.amazon.events.rule.RetryPolicy.builder()
                        .maximumEventAgeInSeconds(1)
                        .maximumRetryAttempts(1)
                        .build())
                .runCommandParameters(software.amazon.events.rule.RunCommandParameters.builder()
                        .runCommandTargets(runCommandTargets)
                        .build())
                .sqsParameters(software.amazon.events.rule.SqsParameters.builder()
                        .messageGroupId("MESSAGE_GROUP_ID")
                        .build())
                .sageMakerPipelineParameters(software.amazon.events.rule.SageMakerPipelineParameters.builder()
                        .pipelineParameterList(sageMakerPipelineParameters)
                        .build())
                .build());

        final ResourceModel model = ResourceModel.builder()
                .arn(EVENT_RULE_ARN_DEFAULT_BUS)
                .build();

        final ResourceModel resultModel = ResourceModel.builder()
                .id(EVENT_RULE_NAME)
                .name(EVENT_RULE_NAME)
                .description("TestDescription")
                .eventPattern(eventMapperMap)
                .state("ENABLED")
                .targets(targets)
                .build();

        // MOCK

        /*
         * describeRule
         * listTargetsByRule
         */

        Collection<software.amazon.awssdk.services.cloudwatchevents.model.Target> responseTargets = new ArrayList<>();
        for (software.amazon.events.rule.Target target : targets) {
            responseTargets.add(convertTarget(target));
        }

        final DescribeRuleResponse describeRuleResponse = DescribeRuleResponse.builder()
                .name(resultModel.getName())
                .description(resultModel.getDescription())
                .eventPattern(eventPatternString)
                .state(resultModel.getState())
                .build();

        final ListTargetsByRuleResponse listTargetsByRuleResponse = ListTargetsByRuleResponse.builder()
                .targets(responseTargets)
                .build();

        when(proxyClient.client().describeRule(any(DescribeRuleRequest.class)))
                .thenReturn(describeRuleResponse);

        when(proxyClient.client().listTargetsByRule(any(ListTargetsByRuleRequest.class)))
                .thenReturn(listTargetsByRuleResponse);

        // RUN

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(SOURCE_ACCOUNT_ID)
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);

        // ASSERT

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(resultModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    /**
     * A hacky way to avoid rewriting logic to convert ResourceModel Targets to
     * AwsSdk Targets
     *
     * @param target A ResourceModel Target
     * @return An AwsSdk Targetreq
     */
    private software.amazon.awssdk.services.cloudwatchevents.model.Target convertTarget(
            software.amazon.events.rule.Target target) {
        HashSet<software.amazon.events.rule.Target> targets = new HashSet<>();
        targets.add(target);
        ResourceModel model = ResourceModel.builder()
                .targets(targets)
                .name(EVENT_RULE_NAME)
                .build();

        CompositePID compositePID = new CompositePID(model, SOURCE_ACCOUNT_ID);

        PutTargetsRequest putTargetsRequest = Translator.translateToPutTargetsRequest(model, compositePID);

        return putTargetsRequest.targets().get(0);
    }
}
