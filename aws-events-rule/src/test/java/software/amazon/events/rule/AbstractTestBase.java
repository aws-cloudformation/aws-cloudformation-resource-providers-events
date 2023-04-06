package software.amazon.events.rule;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;

public class AbstractTestBase {
  protected static final String SOURCE_ACCOUNT_ID = "123456789012";
  protected static final String CROSS_ACCOUNT_ID = "123456789013";
  protected static final String EVENT_RULE_NAME = "EventRuleName";
  protected static final String DEFAULT_EVENT_BUS_NAME = "default";
  protected static final String CUSTOM_EVENT_BUS_NAME = "CustomBusName";

  protected static final String SAME_ACCOUNT_DEFAULT_BUS_ARN = "arn:aws:events:us-east-1:" + SOURCE_ACCOUNT_ID + ":event-bus/" + DEFAULT_EVENT_BUS_NAME;
  protected static final String EVENT_RULE_ARN_DEFAULT_BUS = "arn:aws:events:us-east-1:" + SOURCE_ACCOUNT_ID + ":rule/" + EVENT_RULE_NAME;
  protected static final String SAME_ACCOUNT_PID_DEFAULT_BUS = EVENT_RULE_NAME;

  protected static final String SAME_ACCOUNT_CUSTOM_EVENT_BUS_ARN = "arn:aws:events:us-east-1:" + SOURCE_ACCOUNT_ID + ":event-bus/" + CUSTOM_EVENT_BUS_NAME;
  protected static final String EVENT_RULE_ARN_CUSTOM_BUS = "arn:aws:events:us-east-1:" + SOURCE_ACCOUNT_ID + ":rule/" + CUSTOM_EVENT_BUS_NAME + "/" + EVENT_RULE_NAME;
  protected static final String SAME_ACCOUNT_PID_CUSTOM_BUS = CUSTOM_EVENT_BUS_NAME + "|" + EVENT_RULE_NAME;

  protected static final String CROSS_ACCOUNT_CUSTOM_EVENT_BUS_ARN = "arn:aws:events:us-east-1:" + CROSS_ACCOUNT_ID + ":event-bus/" + CUSTOM_EVENT_BUS_NAME;
  protected static final String CROSS_ACCOUNT_EVENT_RULE_ARN_CUSTOM_BUS = "arn:aws:events:us-east-1:" + CROSS_ACCOUNT_ID + ":rule/" + CUSTOM_EVENT_BUS_NAME + "/" + EVENT_RULE_NAME;
  protected static final String CROSS_ACCOUNT_CUSTOM_PID_CUSTOM_BUS = "arn:aws:events:us-east-1:" + CROSS_ACCOUNT_ID + ":event-bus/" + CUSTOM_EVENT_BUS_NAME + "|" + EVENT_RULE_NAME;

  protected static final String MOCK_STACK_ID = "arn:aws:cloudformation:us-east-1:123456789012:stack/mystack/f449b250-b969-11e0-a185-5081d0136786";
  protected static final String MOCK_LOGICAL_ID = "MyEventRule";
  protected static final String MOCK_CLIENT_TOKEN = "f119b160-b969-11e0-a185-5081d0131234";
  protected static final String MOCK_EVENT_RULE_NAME = "CustomEventRuleName";
  protected static final String MOCK_CUSTOM_EVENT_BUS_NAME = "CustomEventBus";
  protected static final String MOCK_CROSS_ACCOUNT_EVENT_BUS_ARN = "arn:aws:events:us-east-1:123456789013:event-bus/custom.event.bus.crossaccount";

  protected static final Credentials MOCK_CREDENTIALS;
  protected static final LoggerProxy logger;

  static {
    MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
    logger = new LoggerProxy();
  }
  static ProxyClient<CloudWatchEventsClient> MOCK_PROXY(
    final AmazonWebServicesClientProxy proxy,
    final CloudWatchEventsClient sdkClient) {
    return new ProxyClient<CloudWatchEventsClient>() {
      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseT
      injectCredentialsAndInvokeV2(RequestT request, Function<RequestT, ResponseT> requestFunction) {
        return proxy.injectCredentialsAndInvokeV2(request, requestFunction);
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse>
      CompletableFuture<ResponseT>
      injectCredentialsAndInvokeV2Async(RequestT request, Function<RequestT, CompletableFuture<ResponseT>> requestFunction) {
        throw new UnsupportedOperationException();
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse, IterableT extends SdkIterable<ResponseT>>
      IterableT
      injectCredentialsAndInvokeIterableV2(RequestT request, Function<RequestT, IterableT> requestFunction) {
        return proxy.injectCredentialsAndInvokeIterableV2(request, requestFunction);
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseInputStream<ResponseT>
      injectCredentialsAndInvokeV2InputStream(RequestT requestT, Function<RequestT, ResponseInputStream<ResponseT>> function) {
        throw new UnsupportedOperationException();
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseBytes<ResponseT>
      injectCredentialsAndInvokeV2Bytes(RequestT requestT, Function<RequestT, ResponseBytes<ResponseT>> function) {
        throw new UnsupportedOperationException();
      }

      @Override
      public CloudWatchEventsClient client() {
        return sdkClient;
      }
    };
  }
}
