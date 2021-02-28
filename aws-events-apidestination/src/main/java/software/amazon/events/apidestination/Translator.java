package software.amazon.events.apidestination;

import software.amazon.awssdk.services.eventbridge.model.CreateApiDestinationRequest;
import software.amazon.awssdk.services.eventbridge.model.DeleteApiDestinationRequest;
import software.amazon.awssdk.services.eventbridge.model.DescribeApiDestinationRequest;
import software.amazon.awssdk.services.eventbridge.model.DescribeApiDestinationResponse;
import software.amazon.awssdk.services.eventbridge.model.ListApiDestinationsRequest;
import software.amazon.awssdk.services.eventbridge.model.ListApiDestinationsResponse;
import software.amazon.awssdk.services.eventbridge.model.UpdateApiDestinationRequest;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

  /**
   * Request to create a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static CreateApiDestinationRequest translateToCreateRequest(final ResourceModel model) {
    return CreateApiDestinationRequest.builder()
            .name(model.getName())
            .description(model.getDescription())
            .httpMethod(model.getHttpMethod())
            .invocationEndpoint(model.getInvocationEndpoint())
            .connectionArn(model.getConnectionArn())
            .invocationRateLimitPerSecond(model.getInvocationRateLimitPerSecond())
            .build();
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static DescribeApiDestinationRequest translateToReadRequest(final ResourceModel model) {
    return DescribeApiDestinationRequest.builder()
            .name(model.getName())
            .build();
  }

  /**
   * Request to update properties of a previously created resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static UpdateApiDestinationRequest translateToUpdateRequest(final ResourceModel model) {

    return UpdateApiDestinationRequest.builder()
            .name(model.getName())
            .description(model.getDescription())
            .httpMethod(model.getHttpMethod())
            .invocationEndpoint(model.getInvocationEndpoint())
            .connectionArn(model.getConnectionArn())
            .invocationRateLimitPerSecond(model.getInvocationRateLimitPerSecond())
            .build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param awsResponse the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final DescribeApiDestinationResponse awsResponse) {
    ResourceModel resourceModel = ResourceModel.builder()
            .name(awsResponse.name())
            .arn(awsResponse.apiDestinationArn())
            .description(awsResponse.description())
            .connectionArn(awsResponse.connectionArn())
            .invocationEndpoint(awsResponse.invocationEndpoint())
            .invocationRateLimitPerSecond(awsResponse.invocationRateLimitPerSecond())
            .httpMethod(awsResponse.httpMethodAsString())
            .build();

    return resourceModel;
  }


  /**
   * Request to list resources
   * @param nextToken token passed to the aws service list resources request
   * @return awsRequest the aws service request to list resources within aws account
   */
  static ListApiDestinationsRequest translateToListRequest(final String nextToken) {
    return ListApiDestinationsRequest.builder()
            .nextToken(nextToken)
            .build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * @param awsResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromList(final ListApiDestinationsResponse awsResponse) {
    return streamOfOrEmpty(awsResponse.apiDestinations())
            .map(resource -> ResourceModel.builder()
                    .name(resource.name())
                    .connectionArn(resource.connectionArn())
                    .invocationRateLimitPerSecond(resource.invocationRateLimitPerSecond())
                    .build())
            .collect(Collectors.toList());
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static DeleteApiDestinationRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteApiDestinationRequest.builder()
            .name(model.getName())
            .build();
  }

  static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
            .map(Collection::stream)
            .orElseGet(Stream::empty);
  }

}
