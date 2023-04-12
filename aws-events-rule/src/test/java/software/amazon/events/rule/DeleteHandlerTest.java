package software.amazon.events.rule;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.awssdk.services.cloudwatchevents.model.*;
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
        final DeleteHandler handler = new DeleteHandler();

        // MODEL

        final ResourceModel model = ResourceModel.builder()
                .name("TestRule")
                .build();

        Set<Target> previousTargets = new HashSet<>();
        previousTargets.add(software.amazon.events.rule.Target.builder()
                .id("ToDeleteId")
                .arn("ToDeleteArn")
                .build());

        final ResourceModel previousModel = ResourceModel.builder()
                .name("TestRule")
                .description("TestDescription")
                .state("ENABLED")
                .targets(previousTargets)
                .build();

        // MOCK

        /*
        describeRule
        removeTargets
        deleteRule
         */

        final DescribeRuleResponse describeRuleResponse = DescribeRuleResponse.builder().build();

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
            .desiredResourceState(model)
            .previousResourceState(previousModel)
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
    public void handleRequest_DoesNotExist() {
        final DeleteHandler handler = new DeleteHandler();

        // MODEL

        final ResourceModel model = ResourceModel.builder()
                .name("TestRule")
                .build();

        Set<Target> previousTargets = new HashSet<>();
        previousTargets.add(software.amazon.events.rule.Target.builder()
                .id("ToDeleteId")
                .arn("ToDeleteArn")
                .build());

        final ResourceModel previousModel = ResourceModel.builder()
                .name("TestRule")
                .description("TestDescription")
                .state("ENABLED")
                .targets(previousTargets)
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
                .desiredResourceState(model)
                .previousResourceState(previousModel)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // ASSERT

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
//        assertThat(response.getResourceModel()).isNull();
//        assertThat(response.getResourceModels()).isNull();
//        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }
}
