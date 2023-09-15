package software.amazon.events.eventbus;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetrySetting;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.EqualJitterBackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.cloudformation.LambdaWrapper;

import java.time.Duration;


public class ClientBuilder {
    private static EventBridgeClient eventBridgeClient;
    public static final String US_WEST_2 = "us-west-2";
    public static final String REGION = System.getenv("AWS_REGION") != null ? System.getenv("AWS_REGION") : US_WEST_2;

    private static final BackoffStrategy EVENTBUS_BACKOFF_THROTTLING_STRATEGY =
            EqualJitterBackoffStrategy.builder()
                    .baseDelay(Duration.ofMillis(2000)) //1st retry is ~2 sec
                    .maxBackoffTime(SdkDefaultRetrySetting.MAX_BACKOFF) //default is 20s
                    .build();

    private static final RetryPolicy EVENTBUS_RETRY_POLICY =
            RetryPolicy.builder()
                    .numRetries(4)
                    .retryCondition(RetryCondition.defaultRetryCondition())
                    .throttlingBackoffStrategy(EVENTBUS_BACKOFF_THROTTLING_STRATEGY)
                    .build();

    public static EventBridgeClient getClient() {
        return eventBridgeClient = EventBridgeClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .retryPolicy(EVENTBUS_RETRY_POLICY).build())
                .region(Region.of(REGION))
                .build();
    }
}
