package software.amazon.events.rule;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
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
            eventMapperMap = MAPPER.readValue(eventPatternString, new TypeReference<Map<String, Object>>(){});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        
        // MODEL

        Set<Target> targets = new HashSet<>();

        targets.add(software.amazon.events.rule.Target.builder()
                .id("TestLambdaFunctionId")
                .arn("arn:aws:lambda:us-west-2:123456789123:function:TestLambdaFunctionId")
                .build());

        final ResourceModel model = ResourceModel.builder()
                .name("TestRule")
                .build();

        final ResourceModel resultModel = ResourceModel.builder()
                .name(model.getName())
                .description("TestDescription")
                .eventPattern(eventMapperMap)
                .state("ENABLED")
                .targets(targets)
                .build();

        // MOCK

        /*
        describeRule
        listTargetsByRule
         */

        Collection<software.amazon.awssdk.services.cloudwatchevents.model.Target> responseTargets = new ArrayList<>();
        for (software.amazon.events.rule.Target target : targets) {
            responseTargets.add(software.amazon.awssdk.services.cloudwatchevents.model.Target.builder()
                    .id(target.getId())
                    .arn(target.getArn())
                    .build());
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

        //RUN

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

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
            eventMapperMap = MAPPER.readValue(eventPatternString, new TypeReference<Map<String, Object>>(){});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // MODEL

        final ResourceModel model = ResourceModel.builder()
                .name("TestRule")
                .build();

        final ResourceModel resultModel = ResourceModel.builder()
                .name(model.getName())
                .description("TestDescription")
                .eventPattern(eventMapperMap)
                .state("ENABLED")
                .build();

        // MOCK

        /*
        describeRule
        listTargetsByRule
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

        //RUN

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

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
                .name("TestRule")
                .build();

        // MOCK

        /*
        describeRule
        listTargetsByRule
         */

        when(proxyClient.client().describeRule(any(DescribeRuleRequest.class)))
                .thenThrow(ResourceNotFoundException.class);

        //RUN

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // ASSERT

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    }

}
