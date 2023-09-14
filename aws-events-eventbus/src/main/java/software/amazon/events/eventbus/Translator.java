package software.amazon.events.eventbus;

import software.amazon.awssdk.services.eventbridge.model.CreateEventBusRequest;
import software.amazon.awssdk.services.eventbridge.model.DeleteEventBusRequest;
import software.amazon.awssdk.services.eventbridge.model.DescribeEventBusRequest;
import software.amazon.awssdk.services.eventbridge.model.DescribeEventBusResponse;
import software.amazon.awssdk.services.eventbridge.model.ListEventBusesRequest;
import software.amazon.awssdk.services.eventbridge.model.ListEventBusesResponse;
import software.amazon.awssdk.services.eventbridge.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.eventbridge.model.PutPermissionRequest;
import software.amazon.awssdk.services.eventbridge.model.TagResourceRequest;
import software.amazon.awssdk.services.eventbridge.model.UntagResourceRequest;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class is a centralized placeholder for
 * - api request construction
 * - object translation to/from aws sdk
 * - resource model construction for read/list handlers
 */

public class Translator {

    static private final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Request to create a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to create a resource
     */
    static CreateEventBusRequest translateToCreateRequest(Map<String, String> tags, final ResourceModel model) {
        CreateEventBusRequest.Builder builder = CreateEventBusRequest.builder().name(model.getName());
        if (model.getEventSourceName() != null) {
            builder = builder.eventSourceName(model.getEventSourceName());
        }

        if (tags != null && !tags.isEmpty()) {
            builder = builder.tags(TagHelper.convertToSet(tags));
        }

        return builder.build();
    }

    static PutPermissionRequest translateToPutPermissionRequest(final ResourceModel model) {
        PutPermissionRequest.Builder builder = PutPermissionRequest.builder().eventBusName(model.getName())
                .policy(getPolicy(model));
        return builder.build();
    }

    /**
     * Request to read a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to describe a resource
     */
    static DescribeEventBusRequest translateToReadRequest(final ResourceModel model) {
        return DescribeEventBusRequest.builder().name(model.getName()).build();
    }

    static ListTagsForResourceRequest translateToListTagsForResourceRequest(final ResourceModel model) {
        return ListTagsForResourceRequest.builder()
                .resourceARN(model.getArn()).build();
    }

    /**
     * Translates resource object from sdk into a resource model
     *
     * @param awsResponse the aws service describe resource response
     * @return model resource model
     */
    static ResourceModel.ResourceModelBuilder translateFromReadResponse(final DescribeEventBusResponse awsResponse) {
        return ResourceModel.builder()
                .arn(awsResponse.arn())
                .name(awsResponse.name())
                .policy(awsResponse.policy());
    }

    /**
     * Request to delete a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to delete a resource
     */
    static DeleteEventBusRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteEventBusRequest.builder()
                .name(model.getName()).build();
    }

    /**
     * Request to list resources
     *
     * @param nextToken token passed to the aws service list resources request
     * @return awsRequest the aws service request to list resources within aws account
     */
    static ListEventBusesRequest translateToListRequest(final String nextToken) {
        return ListEventBusesRequest.builder().nextToken(nextToken).build();
    }

    /**
     * Translates resource objects from sdk into a resource model (primary identifier only)
     *
     * @param awsResponse the aws service describe resource response
     * @return list of resource models
     */
    static List<ResourceModel> translateFromListResponse(final ListEventBusesResponse awsResponse) {
        return streamOfOrEmpty(awsResponse.eventBuses())
                .map(resource -> ResourceModel.builder()
                        .name(resource.name())
                        .arn(resource.arn())
                        .policy(resource.policy())
                        .build())
                .collect(Collectors.toList());
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }

    /**
     * Request to add tags to a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to create a resource
     */
    static TagResourceRequest tagResourceRequest(final ResourceModel model, final Map<String, String> addedTags) {
        return TagResourceRequest.builder()
                .resourceARN(model.getArn()).tags(TagHelper.convertToSet(addedTags)).build();
    }

    /**
     * Request to add tags to a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to create a resource
     */
    static UntagResourceRequest untagResourceRequest(final ResourceModel model, final Set<String> removedTags) {
        return UntagResourceRequest.builder().resourceARN(model.getArn()).tagKeys(removedTags).build();
    }

    static public String getPolicy(ResourceModel model) {
        if (model.getPolicy() instanceof Map){
            try {
                return MAPPER.writeValueAsString(model.getPolicy());
            } catch (JsonProcessingException e) {
                throw new CfnInvalidRequestException(e);
            }
        }
        return (String) model.getPolicy();
    }
}
