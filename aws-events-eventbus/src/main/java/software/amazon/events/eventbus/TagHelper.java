package software.amazon.events.eventbus;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.Tag;
import software.amazon.awssdk.services.eventbridge.model.TagResourceResponse;
import software.amazon.awssdk.services.eventbridge.model.UntagResourceResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

public class TagHelper {
    private static final String ERROR_MESSAGE_DUPLICATE_TAG_KEYS = "Provided tags contain duplicate key(s)";
    /**
     * convertToMap
     * <p>
     * Converts a collection of Tag objects to a tag-name -> tag-value map.
     * <p>
     * Note: Tag objects with null tag values will not be included in the output
     * map.
     *
     * @param tags Collection of tags to convert
     * @return Converted Map of tags
     */
    public static Map<String, String> convertToMap(final Collection<Tag> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            return Collections.emptyMap();
        }

        return Optional.ofNullable(tags).orElse(ImmutableList.of())
                .stream()
                .collect(Collectors.toMap(Tag::key, Tag::value,
                        (oldValue, newValue) -> { throw new RuntimeException(ERROR_MESSAGE_DUPLICATE_TAG_KEYS); }));
    }

    /**
     * convertToSet
     * <p>
     * Converts a tag map to a set of Tag objects.
     * <p>
     * Note: Like convertToMap, convertToSet filters out value-less tag entries.
     *
     * @param tagMap Map of tags to convert
     * @return Set of Tag objects
     */
    public static Set<Tag> convertToSet(final Map<String, String> tagMap) {
        if (MapUtils.isEmpty(tagMap)) {
            return Collections.emptySet();
        }
        return tagMap.entrySet().stream()
                .filter(tag -> tag.getValue() != null)
                .map(tag -> Tag.builder()
                        .key(tag.getKey())
                        .value(tag.getValue())
                        .build())
                .collect(Collectors.toSet());
    }

    /**
     * generateTagsForCreate
     * <p>
     * Generate tags to put into resource creation request.
     * This includes user defined tags and system tags as well.
     */
    public final Map<String, String> generateTagsForCreate(final ResourceModel resourceModel, final ResourceHandlerRequest<ResourceModel> handlerRequest) {
        final Map<String, String> tagMap = new HashMap<>();

        if (handlerRequest.getSystemTags() != null) {
            tagMap.putAll(handlerRequest.getSystemTags());
        }

        if (handlerRequest.getDesiredResourceTags() != null) {
            tagMap.putAll(handlerRequest.getDesiredResourceTags());
        }

        if (resourceModel.getTags() != null) {
            tagMap.putAll(convertToMap(
                    resourceModel.getTags().stream().filter(tag -> tag.getKey() != null)
                            .map(tag -> Tag.builder().key(tag.getKey()).value(tag.getValue()).build())
                            .collect(Collectors.toList())
            ));
        }
        return Collections.unmodifiableMap(tagMap);
    }

    /**
     * shouldUpdateTags
     * <p>
     * Determines whether user defined tags have been changed during update.
     */
    public final boolean shouldUpdateTags(final ResourceHandlerRequest<ResourceModel> handlerRequest) {
        final Map<String, String> previousTags = getPreviouslyAttachedTags(handlerRequest);
        final Map<String, String> desiredTags = getNewDesiredTags(handlerRequest);
        return ObjectUtils.notEqual(previousTags, desiredTags);
    }

    /**
     * getPreviouslyAttachedTags
     * <p>
     * If stack tags and resource tags are not merged together in Configuration class,
     * we will get previously attached system (with `aws:cloudformation` prefix) and user defined tags from
     * handlerRequest.getPreviousSystemTags() (system tags),
     * handlerRequest.getPreviousResourceTags() (stack tags),
     * handlerRequest.getPreviousResourceState().getTags() (resource tags).
     * <p>
     * System tags are an optional feature. Merge them to your tags if you have enabled them for your resource.
     * System tags can change on resource update if the resource is imported to the stack.
     */
    public Map<String, String> getPreviouslyAttachedTags(final ResourceHandlerRequest<ResourceModel> handlerRequest) {
        final Map<String, String> previousTags = new HashMap<>();

        // get previous system tags if your service supports CloudFormation system tags
        if (handlerRequest.getPreviousSystemTags() != null) {
            previousTags.putAll(handlerRequest.getPreviousSystemTags());
        }

        // get previous stack level tags from handlerRequest
        if (handlerRequest.getPreviousResourceTags() != null) {
            previousTags.putAll(handlerRequest.getPreviousResourceTags());
        }

        // get resource level tags from previous resource state based on your tag property name
        if (handlerRequest.getPreviousResourceState() != null &&
                handlerRequest.getPreviousResourceState().getTags() != null) {
            previousTags.putAll(convertToMap(handlerRequest.getPreviousResourceState().getTags()
                    .stream().filter(tag -> tag.getKey() != null)
                    .map(tag -> Tag.builder().key(tag.getKey()).value(tag.getValue()).build())
                    .collect(Collectors.toList())));
        }
        return previousTags;
    }

    /**
     * getNewDesiredTags
     * <p>
     * If stack tags and resource tags are not merged together in Configuration class,
     * we will get new desired system (with `aws:cloudformation` prefix) and user defined tags from
     * handlerRequest.getSystemTags() (system tags),
     * handlerRequest.getDesiredResourceTags() (stack tags),
     * handlerRequest.getDesiredResourceState().getTags() (resource tags).
     * <p>
     * System tags are an optional feature. Merge them to your tags if you have enabled them for your resource.
     * System tags can change on resource update if the resource is imported to the stack.
     */
    public Map<String, String> getNewDesiredTags(final ResourceHandlerRequest<ResourceModel> handlerRequest) {
        final Map<String, String> desiredTags = new HashMap<>();

        // merge system tags with desired resource tags if your service supports CloudFormation system tags
        if (handlerRequest.getSystemTags() != null) {
            desiredTags.putAll(handlerRequest.getSystemTags());
        }

        // get desired stack level tags from handlerRequest
        if (handlerRequest.getDesiredResourceTags() != null) {
            desiredTags.putAll(handlerRequest.getDesiredResourceTags());
        }

        // get resource level tags from resource model based on your tag property name
        if (handlerRequest.getDesiredResourceState() != null &&
                handlerRequest.getDesiredResourceState().getTags() != null) {
            desiredTags.putAll(convertToMap(handlerRequest.getDesiredResourceState().getTags()
                    .stream().filter(tag -> tag.getKey() != null)
                    .map(tag -> Tag.builder().key(tag.getKey()).value(tag.getValue()).build())
                    .collect(Collectors.toList())));
        }
        return desiredTags;
    }

    /**
     * generateTagsToAdd
     * <p>
     * Determines the tags the customer desired to define or redefine.
     */
    public Map<String, String> generateTagsToAdd(final Map<String, String> previousTags, final Map<String, String> desiredTags) {
        return desiredTags.entrySet().stream()
                .filter(e -> !previousTags.containsKey(e.getKey()) || !Objects.equals(previousTags.get(e.getKey()), e.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue));
    }

    /**
     * getTagsToRemove
     * <p>
     * Determines the tags the customer desired to remove from the function.
     */
    public Set<String> generateTagsToRemove(final Map<String, String> previousTags, final Map<String, String> desiredTags) {
        final Set<String> desiredTagNames = desiredTags.keySet();

        return previousTags.keySet().stream()
                .filter(tagName -> !desiredTagNames.contains(tagName))
                .collect(Collectors.toSet());
    }

    /**
     * tagResource during update
     * <p>
     * Calls the service:TagResource API.
     */
    public ProgressEvent<ResourceModel, CallbackContext>
    tagResource(final AmazonWebServicesClientProxy proxy, final ProxyClient<EventBridgeClient> serviceClient,
            final ResourceModel resourceModel,
            final ResourceHandlerRequest<ResourceModel> handlerRequest, final CallbackContext callbackContext,
            final Map<String, String> addedTags, final Logger logger) {
        logger.log(String.format("[UPDATE][IN PROGRESS] Going to add tags for ... resource: %s with AccountId: %s",
                ResourceModel.TYPE_NAME, handlerRequest.getAwsAccountId()));

        return proxy.initiate("AWS-Events-EventBus::TagOps", serviceClient, resourceModel, callbackContext)
                .translateToServiceRequest(model ->
                        Translator.tagResourceRequest(model, addedTags))
                .makeServiceCall((request, client) -> {
                    TagResourceResponse awsResponse =
                            proxy.injectCredentialsAndInvokeV2(request, client.client()::tagResource);

                    return awsResponse;
                })
                .handleError((tagResourceRequest, e, proxyClient, resourceModel1, callbackContext1) -> {
                    final ReadHandler handler = new ReadHandler();
                    return handler.handleError(tagResourceRequest, e, proxyClient, resourceModel1, callbackContext1);
                })
                .progress();
    }

    /**
     * untagResource during update
     * <p>
     * Calls the service:UntagResource API.
     */
    public ProgressEvent<ResourceModel, CallbackContext>
    untagResource(final AmazonWebServicesClientProxy proxy, final ProxyClient<EventBridgeClient> serviceClient,
            final ResourceModel resourceModel, final ResourceHandlerRequest<ResourceModel> handlerRequest,
            final CallbackContext callbackContext, final Set<String> removedTags, final Logger logger) {
        logger.log(String.format("[UPDATE][IN PROGRESS] Going to remove tags for ... resource: %s with AccountId: %s",
                ResourceModel.TYPE_NAME, handlerRequest.getAwsAccountId()));

        return proxy.initiate("AWS-Events-EventBus::TagOps", serviceClient, resourceModel, callbackContext)
                .translateToServiceRequest(model ->
                        Translator.untagResourceRequest(model, removedTags))
                .makeServiceCall((request, client) -> {
                    UntagResourceResponse awsResponse = proxy.injectCredentialsAndInvokeV2(request, client.client()::untagResource);
                    return awsResponse;
                })
                .handleError((tagResourceRequest, e, proxyClient, resourceModel1, callbackContext1) -> {
                    final ReadHandler handler = new ReadHandler();
                    return handler.handleError(tagResourceRequest, e, proxyClient, resourceModel1, callbackContext1);
                })
                .progress();
    }
}
