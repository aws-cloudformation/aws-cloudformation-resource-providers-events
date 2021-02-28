package software.amazon.events.apidestination;

import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {

  public static EventBridgeClient getClient() {
    return EventBridgeClient.builder()
            .httpClient(LambdaWrapper.HTTP_CLIENT)
            .build();
  }
}
