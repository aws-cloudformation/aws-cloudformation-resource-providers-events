package software.amazon.events.apidestination;

public final class TestConstants {
    private TestConstants() {

    }

    public static final int INVOCATION_RATE_LIMIT = 30;

    public static final String CONNECTION_ARN = "arn:aws:events:us-east-1:0123456789012:connection/1";

    public static final String NOT_EXISTING_CONNECTION_ARN = "arn:aws:events:us-east-1:0123456789012:connection/non-existent";

    public static final String API_DESTINATION_NAME = "TestApiDestination";

    public static final String API_DESTINATION_ARN = "TestApiDestinationARN";

    public static final String API_DESTINATION_STATE = "ENABLED";

    public static final String ENDPOINT = "TestEndpoint";

}
