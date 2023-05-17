package software.amazon.events.rule;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.awssdk.services.cloudwatchevents.model.DeleteRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.DeleteRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.DescribeRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.DescribeRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.ListTargetsByRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.ListTargetsByRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.RemoveTargetsRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.RemoveTargetsResponse;
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
public class DeleteHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<CloudWatchEventsClient> proxyClient;

    @Mock
    CloudWatchEventsClient sdkClient;

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
    public void handleRequest_SimpleSuccess_ListTargets() {
        final DeleteHandler handler = new DeleteHandler();

        // MODEL
        final ResourceModel model = ResourceModel.builder()
                .arn(EVENT_RULE_ARN_DEFAULT_BUS)
                .build();

        Collection<software.amazon.awssdk.services.cloudwatchevents.model.Target> responseTargets = new ArrayList<>();
        responseTargets.add(software.amazon.awssdk.services.cloudwatchevents.model.Target.builder()
                .id("ToDeleteId")
                .arn("ToDeleteArn")
                .build());

        // MOCK
        /*
        describeRule
        removeTargets
        deleteRule
         */

        final DescribeRuleResponse describeRuleResponse = DescribeRuleResponse.builder()
                .arn(EVENT_RULE_ARN_DEFAULT_BUS)
                .build();

        final ListTargetsByRuleResponse listTargetsByRuleResponse = ListTargetsByRuleResponse.builder() // TODO
                .targets(responseTargets)
                .build();

        final RemoveTargetsResponse removeTargetsResponse = RemoveTargetsResponse.builder()
                .build();

        final DeleteRuleResponse deleteRuleResponse = DeleteRuleResponse.builder()
                .build();

        when(proxyClient.client().describeRule(any(DescribeRuleRequest.class)))
                .thenReturn(describeRuleResponse);

        when(proxyClient.client().listTargetsByRule(any(ListTargetsByRuleRequest.class)))
                .thenReturn(listTargetsByRuleResponse);

        when(proxyClient.client().removeTargets(any(RemoveTargetsRequest.class)))
                .thenReturn(removeTargetsResponse);

        when(proxyClient.client().deleteRule(any(DeleteRuleRequest.class)))
                .thenReturn(deleteRuleResponse);

        // RUN

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .awsAccountId(SOURCE_ACCOUNT_ID)
            .desiredResourceState(model)
            .build();

        CallbackContext context = new CallbackContext();
        ProgressEvent<ResourceModel, CallbackContext> response;

        response = handler.handleRequest(proxy, request, context, proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        response = handler.handleRequest(proxy, request, context, proxyClient, logger);

        // ASSERT

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final DeleteHandler handler = new DeleteHandler();

        Set<software.amazon.events.rule.Target> targets = new HashSet<>();

        targets.add(software.amazon.events.rule.Target.builder()
                .id("ToDeleteId")
                .arn("ToDeleteArn")
                .build());

        // MODEL
        final ResourceModel model = ResourceModel.builder()
                .arn(EVENT_RULE_ARN_DEFAULT_BUS)
                .targets(targets)
                .build();

        // MOCK
        /*
        describeRule
        removeTargets
        deleteRule
         */

        final DescribeRuleResponse describeRuleResponse = DescribeRuleResponse.builder()
                .arn(EVENT_RULE_ARN_DEFAULT_BUS)
                .build();

        final RemoveTargetsResponse removeTargetsResponse = RemoveTargetsResponse.builder()
                .build();

        final DeleteRuleResponse deleteRuleResponse = DeleteRuleResponse.builder()
                .build();

        when(proxyClient.client().describeRule(any(DescribeRuleRequest.class)))
                .thenReturn(describeRuleResponse);

        when(proxyClient.client().removeTargets(any(RemoveTargetsRequest.class)))
                .thenReturn(removeTargetsResponse);

        when(proxyClient.client().deleteRule(any(DeleteRuleRequest.class)))
                .thenReturn(deleteRuleResponse);

        // RUN

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(SOURCE_ACCOUNT_ID)
                .desiredResourceState(model)
                .stackId("STACK_ID")
                .build();

        CallbackContext context = new CallbackContext();
        ProgressEvent<ResourceModel, CallbackContext> response;

        response = handler.handleRequest(proxy, request, context, proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        response = handler.handleRequest(proxy, request, context, proxyClient, logger);

        // ASSERT

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_DoesNotExist_ListTargets() {
        final DeleteHandler handler = new DeleteHandler();

        // MODEL

        final ResourceModel model = ResourceModel.builder()
                .arn(EVENT_RULE_ARN_DEFAULT_BUS)
                .name("TestRule")
                .build();

        // MOCK

        /*
        describeRule
        removeTargets
        deleteRule
         */

        when(proxyClient.client().describeRule(any(DescribeRuleRequest.class)))
                .thenThrow(ResourceNotFoundException.class);

        // RUN

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(SOURCE_ACCOUNT_ID)
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // ASSERT

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void handleRequest_DoesNotExist() {
        final DeleteHandler handler = new DeleteHandler();

        Set<software.amazon.events.rule.Target> targets = new HashSet<>();

        targets.add(software.amazon.events.rule.Target.builder()
                .id("ToDeleteId")
                .arn("ToDeleteArn")
                .build());

        // MODEL
        final ResourceModel model = ResourceModel.builder()
                .arn(EVENT_RULE_ARN_DEFAULT_BUS)
                .targets(targets)
                .build();

        // MOCK

        /*
        describeRule
        removeTargets
        deleteRule
         */

        when(proxyClient.client().describeRule(any(DescribeRuleRequest.class)))
                .thenThrow(ResourceNotFoundException.class);

        // RUN

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(SOURCE_ACCOUNT_ID)
                .desiredResourceState(model)
                .stackId("STACK_ID")
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // ASSERT

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }
}
