package software.amazon.events.rule;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.awssdk.services.cloudwatchevents.model.DescribeRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.DescribeRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.ListTargetsByRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.ListTargetsByRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.PutRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.PutRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.PutTargetsRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.PutTargetsResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.PutTargetsResultEntry;
import software.amazon.awssdk.services.cloudwatchevents.model.RemoveTargetsRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.RemoveTargetsResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.Target;
import software.amazon.awssdk.services.cloudwatchevents.model.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.cloudwatchevents.model.BatchParameters;
import software.amazon.awssdk.services.cloudwatchevents.model.BatchArrayProperties;
import software.amazon.awssdk.services.cloudwatchevents.model.BatchRetryStrategy;
import software.amazon.awssdk.services.cloudwatchevents.model.DeadLetterConfig;
import software.amazon.awssdk.services.cloudwatchevents.model.EcsParameters;
import software.amazon.awssdk.services.cloudwatchevents.model.NetworkConfiguration;
import software.amazon.awssdk.services.cloudwatchevents.model.AwsVpcConfiguration;
import software.amazon.awssdk.services.cloudwatchevents.model.HttpParameters;
import software.amazon.awssdk.services.cloudwatchevents.model.InputTransformer;
import software.amazon.awssdk.services.cloudwatchevents.model.KinesisParameters;
import software.amazon.awssdk.services.cloudwatchevents.model.RedshiftDataParameters;
import software.amazon.awssdk.services.cloudwatchevents.model.RetryPolicy;
import software.amazon.awssdk.services.cloudwatchevents.model.RunCommandParameters;
import software.amazon.awssdk.services.cloudwatchevents.model.SqsParameters;
import software.amazon.awssdk.services.cloudwatchevents.model.RunCommandTarget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

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
        final UpdateHandler handler = new UpdateHandler();

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
            eventMapperMap = MAPPER.readValue(eventPatternString, new TypeReference<Map<String, Object>>(){});
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
                .name("TestRule")
                .description("TestDescription")
                .eventPattern(eventMapperMap)
                .state("ENABLED")
                .targets(targets)
                .build();

        // MOCK

        /*
        describeRule
        putRule
        listTargetsByRule
        removeTargets
        putTargets
         */

        Collection<Target> responseTargets1 = new ArrayList<>();
        for (software.amazon.events.rule.Target target :targets) {
            responseTargets1.add(convertTarget(target));
        }
        responseTargets1.add(Target.builder()
                .id("ToDeleteId")
                .arn("ToDeleteArn")
                .build());

        Collection<Target> responseTargets2 = new ArrayList<>();
        for (software.amazon.events.rule.Target target :targets) {
            responseTargets2.add(convertTarget(target));
        }

        final DescribeRuleResponse describeRuleResponse = DescribeRuleResponse.builder()
                .name(model.getName())
                .description(model.getDescription())
                .eventPattern(eventPatternString)
                .state(model.getState())
                .build();

        final PutRuleResponse putRuleResponse = PutRuleResponse.builder()
                .ruleArn("arn")
                .build();

        final ListTargetsByRuleResponse listTargetsByRuleResponse1 = ListTargetsByRuleResponse.builder()
                .targets(responseTargets1)
                .build();

        final ListTargetsByRuleResponse listTargetsByRuleResponse2 = ListTargetsByRuleResponse.builder()
                .targets(responseTargets2)
                .build();

        final RemoveTargetsResponse removeTargetsResponse = RemoveTargetsResponse.builder()
                .build();

        final PutTargetsResponse putTargetsResponse = PutTargetsResponse.builder()
                .build();

        when(proxyClient.client().describeRule(any(DescribeRuleRequest.class)))
                .thenReturn(describeRuleResponse);

        when(proxyClient.client().putRule(any(PutRuleRequest.class)))
                .thenReturn(putRuleResponse);

        when(proxyClient.client().listTargetsByRule(any(ListTargetsByRuleRequest.class)))
                .thenReturn(listTargetsByRuleResponse1)
                .thenReturn(listTargetsByRuleResponse2);

        when(proxyClient.client().removeTargets(any(RemoveTargetsRequest.class)))
                .thenReturn(removeTargetsResponse);

        when(proxyClient.client().putTargets(any(PutTargetsRequest.class)))
                .thenReturn(putTargetsResponse);

        // RUN

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

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
    public void handleRequest_NoTargets() {
        final UpdateHandler handler = new UpdateHandler();

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
            eventMapperMap = MAPPER.readValue(eventPatternString, new TypeReference<Map<String, Object>>(){});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // MODEL

        final ResourceModel model = ResourceModel.builder()
                .name("TestRule")
                .description("TestDescription")
                .eventPattern(eventMapperMap)
                .state("ENABLED")
                .build();

        // MOCK

        /*
        describeRule
        putRule
        listTargetsByRule
        removeTargets
        putTargets
         */

        final DescribeRuleResponse describeRuleResponse = DescribeRuleResponse.builder()
                .name(model.getName())
                .description(model.getDescription())
                .eventPattern(eventPatternString)
                .state(model.getState())
                .build();

        final PutRuleResponse putRuleResponse = PutRuleResponse.builder()
                .ruleArn("arn")
                .build();

        final ListTargetsByRuleResponse listTargetsByRuleResponse = ListTargetsByRuleResponse.builder()
                .build();

        final RemoveTargetsResponse removeTargetsResponse = RemoveTargetsResponse.builder()
                .build();

        final PutTargetsResponse putTargetsResponse = PutTargetsResponse.builder()
                .build();

        when(proxyClient.client().describeRule(any(DescribeRuleRequest.class)))
                .thenReturn(describeRuleResponse);

        when(proxyClient.client().putRule(any(PutRuleRequest.class)))
                .thenReturn(putRuleResponse);

        when(proxyClient.client().listTargetsByRule(any(ListTargetsByRuleRequest.class)))
                .thenReturn(listTargetsByRuleResponse);

//        when(proxyClient.client().removeTargets(any(RemoveTargetsRequest.class)))
//                .thenReturn(removeTargetsResponse);
//
//        when(proxyClient.client().putTargets(any(PutTargetsRequest.class)))
//                .thenReturn(putTargetsResponse);

        // RUN

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

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
    public void handleRequest_CreateTargetsFail() {
        final UpdateHandler handler = new UpdateHandler();

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
            eventMapperMap = MAPPER.readValue(eventPatternString, new TypeReference<Map<String, Object>>(){});
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
                .name("TestRule")
                .description("TestDescription")
                .eventPattern(eventMapperMap)
                .state("ENABLED")
                .targets(targets)
                .build();

        // MOCK

        /*
        describeRule
        putRule
        listTargetsByRule
        removeTargets
        putTargets
         */

        Collection<Target> responseTargets = new ArrayList<>();
        for (software.amazon.events.rule.Target target :targets) {
            responseTargets.add(convertTarget(target));
        }

        final DescribeRuleResponse describeRuleResponse = DescribeRuleResponse.builder()
                .name(model.getName())
                .description(model.getDescription())
                .eventPattern(eventPatternString)
                .state(model.getState())
                .build();

        final PutRuleResponse putRuleResponse = PutRuleResponse.builder()
                .ruleArn("arn")
                .build();

        final ListTargetsByRuleResponse listTargetsByRuleResponse = ListTargetsByRuleResponse.builder()
                .targets(responseTargets)
                .build();

        final Collection<PutTargetsResultEntry> PutTargetsResultEntries = new ArrayList<>();
        for (software.amazon.events.rule.Target target : model.getTargets()) {
            PutTargetsResultEntries.add(PutTargetsResultEntry.builder()
                    .targetId(target.getId())
                    .build());
        }

        final PutTargetsResponse putTargetsResponse = PutTargetsResponse.builder()
                .failedEntries(PutTargetsResultEntries)
                .build();

        when(proxyClient.client().describeRule(any(DescribeRuleRequest.class)))
                .thenReturn(describeRuleResponse);

        when(proxyClient.client().putRule(any(PutRuleRequest.class)))
                .thenReturn(putRuleResponse);

        when(proxyClient.client().listTargetsByRule(any(ListTargetsByRuleRequest.class)))
                .thenReturn(listTargetsByRuleResponse);

        when(proxyClient.client().putTargets(any(PutTargetsRequest.class)))
                .thenReturn(putTargetsResponse);

        // RUN

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // ASSERT

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }

    @Test
    public void handleRequest_TranslatorTest() {
        final UpdateHandler handler = new UpdateHandler();

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
            eventMapperMap = MAPPER.readValue(eventPatternString, new TypeReference<Map<String, Object>>(){});
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
                .build());

        final ResourceModel model = ResourceModel.builder()
                .name("TestRule")
                .description("TestDescription")
                .eventPattern(eventMapperMap)
                .state("ENABLED")
                .targets(targets)
                .build();

        // MOCK

        /*
        describeRule
        putRule
        listTargetsByRule
        removeTargets
        putTargets

        describeRule
        listTargetsByRule
         */

        Collection<Target> responseTargets1 = new ArrayList<>();
        for (software.amazon.events.rule.Target target :targets) {
            responseTargets1.add(convertTarget(target));
        }
        responseTargets1.add(Target.builder()
                .id("ToDeleteId")
                .arn("ToDeleteArn")
                .build());

        Collection<Target> responseTargets2 = new ArrayList<>();
        for (software.amazon.events.rule.Target target :targets) {
            responseTargets2.add(convertTarget(target));
        }

        final DescribeRuleResponse describeRuleResponse = DescribeRuleResponse.builder()
                .name(model.getName())
                .description(model.getDescription())
                .eventPattern(eventPatternString)
                .state(model.getState())
                .build();

        final PutRuleResponse putRuleResponse = PutRuleResponse.builder()
                .ruleArn("arn")
                .build();

        final ListTargetsByRuleResponse listTargetsByRuleResponse1 = ListTargetsByRuleResponse.builder()
                .targets(responseTargets1)
                .build();

        final ListTargetsByRuleResponse listTargetsByRuleResponse2 = ListTargetsByRuleResponse.builder()
                .targets(responseTargets2)
                .build();

        final RemoveTargetsResponse removeTargetsResponse = RemoveTargetsResponse.builder()
                .build();

        final PutTargetsResponse putTargetsResponse = PutTargetsResponse.builder()
                .build();

        when(proxyClient.client().describeRule(any(DescribeRuleRequest.class)))
                .thenReturn(describeRuleResponse);

        when(proxyClient.client().putRule(any(PutRuleRequest.class)))
                .thenReturn(putRuleResponse);

        when(proxyClient.client().listTargetsByRule(any(ListTargetsByRuleRequest.class)))
                .thenReturn(listTargetsByRuleResponse1)
                .thenReturn(listTargetsByRuleResponse2)
                .thenReturn(listTargetsByRuleResponse1);

        when(proxyClient.client().removeTargets(any(RemoveTargetsRequest.class)))
                .thenReturn(removeTargetsResponse);

        when(proxyClient.client().putTargets(any(PutTargetsRequest.class)))
                .thenReturn(putTargetsResponse);

        // RUN

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

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
    public void handleRequest_TranslatorTestNulls() {
        final UpdateHandler handler = new UpdateHandler();

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
            eventMapperMap = MAPPER.readValue(eventPatternString, new TypeReference<Map<String, Object>>(){});
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

        targets.add(software.amazon.events.rule.Target.builder()
                .id("TestLambdaFunctionId")
                .arn("arn:aws:lambda:us-west-2:123456789123:function:TestLambdaFunctionId")
                .batchParameters(software.amazon.events.rule.BatchParameters.builder()
                        .build())
                .deadLetterConfig(software.amazon.events.rule.DeadLetterConfig.builder()
                        .arn("ARN")
                        .build())
                .ecsParameters(software.amazon.events.rule.EcsParameters.builder()
                        .group("GROUP")
                        .launchType("EC2")
                        .platformVersion("PLATFORM_VERSION")
                        .taskCount(1)
                        .taskDefinitionArn("TASK_DEFINITION_ARN")
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
                .sqsParameters(software.amazon.events.rule.SqsParameters.builder()
                        .messageGroupId("MESSAGE_GROUP_ID")
                        .build())
                .build());

        final ResourceModel model = ResourceModel.builder()
                .name("TestRule")
                .description("TestDescription")
                .eventPattern(eventMapperMap)
                .state("ENABLED")
                .targets(targets)
                .build();

        // MOCK

        /*
        describeRule
        putRule
        listTargetsByRule
        removeTargets
        putTargets

        describeRule
        listTargetsByRule
         */

        Collection<Target> responseTargets1 = new ArrayList<>();
        for (software.amazon.events.rule.Target target :targets) {
            responseTargets1.add(convertTarget(target));
        }
        responseTargets1.add(Target.builder()
                .id("ToDeleteId")
                .arn("ToDeleteArn")
                .build());

        Collection<Target> responseTargets2 = new ArrayList<>();
        for (software.amazon.events.rule.Target target :targets) {
            responseTargets2.add(convertTarget(target));
        }

        final DescribeRuleResponse describeRuleResponse = DescribeRuleResponse.builder()
                .name(model.getName())
                .description(model.getDescription())
                .eventPattern(eventPatternString)
                .state(model.getState())
                .build();

        final PutRuleResponse putRuleResponse = PutRuleResponse.builder()
                .ruleArn("arn")
                .build();

        final ListTargetsByRuleResponse listTargetsByRuleResponse1 = ListTargetsByRuleResponse.builder()
                .targets(responseTargets1)
                .build();

        final ListTargetsByRuleResponse listTargetsByRuleResponse2 = ListTargetsByRuleResponse.builder()
                .targets(responseTargets2)
                .build();

        final RemoveTargetsResponse removeTargetsResponse = RemoveTargetsResponse.builder()
                .build();

        final PutTargetsResponse putTargetsResponse = PutTargetsResponse.builder()
                .build();

        when(proxyClient.client().describeRule(any(DescribeRuleRequest.class)))
                .thenReturn(describeRuleResponse);

        when(proxyClient.client().putRule(any(PutRuleRequest.class)))
                .thenReturn(putRuleResponse);

        when(proxyClient.client().listTargetsByRule(any(ListTargetsByRuleRequest.class)))
                .thenReturn(listTargetsByRuleResponse1)
                .thenReturn(listTargetsByRuleResponse2)
                .thenReturn(listTargetsByRuleResponse1);

        when(proxyClient.client().removeTargets(any(RemoveTargetsRequest.class)))
                .thenReturn(removeTargetsResponse);

        when(proxyClient.client().putTargets(any(PutTargetsRequest.class)))
                .thenReturn(putTargetsResponse);

        // RUN

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // ASSERT

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }


    private Target convertTarget(software.amazon.events.rule.Target target) {
        Target.Builder targetBuilder = Target.builder();

        if (target.getBatchParameters() != null) {
            BatchParameters.Builder batchParameters = BatchParameters.builder();

            if (target.getBatchParameters().getArrayProperties() != null) {
                batchParameters.arrayProperties(BatchArrayProperties.builder()
                        .size(target.getBatchParameters().getArrayProperties().getSize())
                        .build());
            }

            if (target.getBatchParameters().getRetryStrategy() != null) {
                batchParameters.retryStrategy(BatchRetryStrategy.builder()
                        .attempts(target.getBatchParameters().getRetryStrategy().getAttempts())
                        .build());
            }

            targetBuilder.batchParameters(batchParameters
                    .jobDefinition(target.getBatchParameters().getJobDefinition())
                    .jobName(target.getBatchParameters().getJobName())
                    .build());
        }

        if (target.getDeadLetterConfig() != null) {
            targetBuilder.deadLetterConfig(DeadLetterConfig.builder()
                    .arn(target.getDeadLetterConfig().getArn())
                    .build());
        }

        if (target.getEcsParameters() != null) {
            EcsParameters.Builder ecsParameters = EcsParameters.builder();

            if (target.getEcsParameters().getNetworkConfiguration() != null &&
                    target.getEcsParameters().getNetworkConfiguration().getAwsVpcConfiguration() != null) {
                ecsParameters.networkConfiguration(NetworkConfiguration.builder()
                        .awsvpcConfiguration(AwsVpcConfiguration.builder()
                                .assignPublicIp(target.getEcsParameters().getNetworkConfiguration().getAwsVpcConfiguration().getAssignPublicIp())
                                .securityGroups(target.getEcsParameters().getNetworkConfiguration().getAwsVpcConfiguration().getSecurityGroups())
                                .subnets(target.getEcsParameters().getNetworkConfiguration().getAwsVpcConfiguration().getSubnets())
                                .build())
                        .build());
            }

            targetBuilder.ecsParameters(ecsParameters
                    //.capacityProviderStrategy()
                    //.enableECSManagedTags()
                    //.enableExecuteCommand()
                    .group(target.getEcsParameters().getGroup())
                    .launchType(target.getEcsParameters().getLaunchType())
                    //.placementConstraints()
                    //.placementStrategy()
                    .platformVersion(target.getEcsParameters().getPlatformVersion())
                    //.propagateTags()
                    //.referenceId()
                    //.tags()
                    .taskCount(target.getEcsParameters().getTaskCount())
                    .taskDefinitionArn(target.getEcsParameters().getTaskDefinitionArn())
                    .build());
        }

        if (target.getHttpParameters() != null) {
            targetBuilder.httpParameters(HttpParameters.builder()
                    .headerParameters(target.getHttpParameters().getHeaderParameters())
                    .pathParameterValues(target.getHttpParameters().getPathParameterValues())
                    .queryStringParameters(target.getHttpParameters().getQueryStringParameters())
                    .build());
        }

        if (target.getInputTransformer() != null) {
            targetBuilder.inputTransformer(InputTransformer.builder()
                    .inputPathsMap(target.getInputTransformer().getInputPathsMap())
                    .inputTemplate(target.getInputTransformer().getInputTemplate())
                    .build());
        }

        if (target.getKinesisParameters() != null) {
            targetBuilder.kinesisParameters(KinesisParameters.builder()
                    .partitionKeyPath(target.getKinesisParameters().getPartitionKeyPath())
                    .build());
        }

        if (target.getRedshiftDataParameters() != null) {
            targetBuilder.redshiftDataParameters(RedshiftDataParameters.builder()
                    .database(target.getRedshiftDataParameters().getDatabase())
                    .dbUser(target.getRedshiftDataParameters().getDbUser())
                    .secretManagerArn(target.getRedshiftDataParameters().getSecretManagerArn())
                    .sql(target.getRedshiftDataParameters().getSql())
                    .statementName(target.getRedshiftDataParameters().getStatementName())
                    .withEvent(target.getRedshiftDataParameters().getWithEvent())
                    .build());
        }

        if (target.getRetryPolicy() != null) {
            targetBuilder.retryPolicy(RetryPolicy.builder()
                    .maximumEventAgeInSeconds(target.getRetryPolicy().getMaximumEventAgeInSeconds())
                    .maximumRetryAttempts(target.getRetryPolicy().getMaximumRetryAttempts())
                    .build());
        }

        if (target.getRunCommandParameters() != null && target.getRunCommandParameters().getRunCommandTargets() != null) {
            ArrayList<RunCommandTarget> runCommandTargets = new ArrayList<>();

            for (software.amazon.events.rule.RunCommandTarget value : target.getRunCommandParameters().getRunCommandTargets()) {
                runCommandTargets.add(RunCommandTarget.builder()
                        .key(value.getKey())
                        .values(value.getValues())
                        .build()
                );
            }

            targetBuilder.runCommandParameters(RunCommandParameters.builder()
                    .runCommandTargets(runCommandTargets)
                    .build());
        }

        if (target.getSqsParameters() != null) {
            targetBuilder.sqsParameters(SqsParameters.builder()
                    .messageGroupId(target.getSqsParameters().getMessageGroupId())
                    .build());
        }

        return targetBuilder
                .arn(target.getArn())
                .id(target.getId())
                .input(target.getInput())
                .inputPath(target.getInputPath())
                .roleArn(target.getRoleArn())
                //.sageMakerPipelineParameters(target.setSageMakerPipelineParameters())
                .build();
    }
}
