package software.amazon.events.rule;

// TODO: replace all usage of SdkClient with your service client type, e.g; YourServiceClient
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {
  /*
  TODO: uncomment the following, replacing YourServiceClient with your service client name
  It is recommended to use static HTTP client so less memory is consumed
  e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/master/aws-logs-loggroup/src/main/java/software/amazon/logs/loggroup/ClientBuilder.java#L9
  */

  public static CloudWatchEventsClient getClient() {
    return CloudWatchEventsClient.builder()
              .httpClient(LambdaWrapper.HTTP_CLIENT)
              .build();
  }

}
