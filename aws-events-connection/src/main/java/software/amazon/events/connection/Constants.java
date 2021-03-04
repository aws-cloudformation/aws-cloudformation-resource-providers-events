package software.amazon.events.connection;

import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.delay.Constant;

import java.time.Duration;
import java.util.function.BiFunction;

public class Constants {
    private Constants() {}

    public static final BiFunction<ResourceModel, ProxyClient<EventBridgeClient>, ResourceModel> EMPTY_CALL =
            (model, proxyClient) -> model;

    public static final Constant BACK_OFF_DELAY = Constant.of()
            .timeout(Duration.ofSeconds(360L))
            .delay(Duration.ofSeconds(15L))
            .build();
}
