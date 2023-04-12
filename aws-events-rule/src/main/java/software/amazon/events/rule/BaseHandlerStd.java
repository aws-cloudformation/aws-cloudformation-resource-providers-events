package software.amazon.events.rule;

import static java.util.Objects.requireNonNull;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;

import software.amazon.awssdk.services.cloudwatchevents.model.CloudWatchEventsRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.DeleteRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.DeleteRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.DescribeRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.DescribeRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.InternalException;
import software.amazon.awssdk.services.cloudwatchevents.model.InvalidEventPatternException;
import software.amazon.awssdk.services.cloudwatchevents.model.LimitExceededException;
import software.amazon.awssdk.services.cloudwatchevents.model.ListTargetsByRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.ListTargetsByRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.PutRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.PutRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.PutTargetsRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.PutTargetsResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.PutTargetsResultEntry;
import software.amazon.awssdk.services.cloudwatchevents.model.RemoveTargetsRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.RemoveTargetsResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.RemoveTargetsResultEntry;
import software.amazon.awssdk.services.cloudwatchevents.model.ResourceNotFoundException;
import software.amazon.awssdk.services.cloudwatchevents.model.Target;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.function.Supplier;

// Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers


public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {
    public static final int MAX_RETRIES_ON_PUT_TARGETS = 5;
    public static final int MAX_RETRIES_ON_REMOVE_TARGETS = 5;
    protected Logger logger;

    private final CloudWatchEventsClient cloudWatchEventsClient;

    protected BaseHandlerStd() {
        this(ClientBuilder.getClient());
    }

    protected BaseHandlerStd(CloudWatchEventsClient cloudWatchEventsClient) {
        this.cloudWatchEventsClient = requireNonNull(cloudWatchEventsClient);
    }

    /**
     * Checks for failed target puts and retries. Returns true iff a retry was not required and performed.
     *
     * @param proxyClient     The client to execute the request
     * @param model           The ResourceModel originally used
     * @param callbackContext The CallbackContext containing the number of retries attempted and the response from the last attempt
     * @param logger          The logger
     * @return Whether it is safe to move on to stabilization
     */
    static boolean mitigateFailedPutTargets(ProxyClient<CloudWatchEventsClient> proxyClient, ResourceModel model, CallbackContext callbackContext, Logger logger, CompositePID compositePID) {
        boolean hasFailedEntries = callbackContext.getPutTargetsResponse().hasFailedEntries() && callbackContext.getPutTargetsResponse().failedEntries().size() > 0;

        if (hasFailedEntries) {
            if (callbackContext.getRetryAttemptsForPutTargets() < MAX_RETRIES_ON_PUT_TARGETS) {
                logger.log(String.format("PutTargets has %s failed entries. Retrying...", callbackContext.getPutTargetsResponse().failedEntryCount()));

                PutTargetsRequest originalPutTargetsRequest = Translator.translateToPutTargetsRequest(model, compositePID);

                // Build a new request from failed entries
                HashMap<String, Target> targetMap = new HashMap<>();
                for (Target target : originalPutTargetsRequest.targets()) {
                    targetMap.put(target.id(), target);
                }

                ArrayList<Target> targetsToPut = new ArrayList<>();
                for (PutTargetsResultEntry failedEntry : callbackContext.getPutTargetsResponse().failedEntries()) {
                    logger.log(failedEntry.errorMessage());
                    targetsToPut.add(targetMap.get(failedEntry.targetId()));
                }

                PutTargetsRequest putTargetsRequest = PutTargetsRequest.builder()
                        .targets(targetsToPut)
                        .build();

                // Retry request
                callbackContext.setRetryAttemptsForPutTargets(callbackContext.getRetryAttemptsForPutTargets() + 1);
                callbackContext.setPutTargetsResponse(proxyClient.injectCredentialsAndInvokeV2(putTargetsRequest, proxyClient.client()::putTargets));
            } else {
                throw AwsServiceException.builder()
                        .awsErrorDetails(AwsErrorDetails.builder().errorCode("FailedEntries (put)").build())
                        .build();
            }
        }

        return !hasFailedEntries;
    }

    /**
     * Checks for failed target removals and retries. Returns true iff a retry was
     * not required and performed.
     *
     * @param proxyClient     The client to execute the request
     * @param callbackContext The CallbackContext containing the number of retries
     *                        attempted and the response from the last attempt
     * @param logger          The logger
     * @return Whether it is safe to move on to stabilization
     */
    static boolean mitigateFailedRemoveTargets(ProxyClient<CloudWatchEventsClient> proxyClient,
                                               CompositePID compositePID, CallbackContext callbackContext, Logger logger) {
        boolean hasFailedEntries = callbackContext.getRemoveTargetsResponse().hasFailedEntries()
                && callbackContext.getRemoveTargetsResponse().failedEntries().size() > 0;

        if (hasFailedEntries) {
            if (callbackContext.getRetryAttemptsForRemoveTargets() < MAX_RETRIES_ON_REMOVE_TARGETS) {
                logger.log(String.format("RemoveTTargets has %s failed entries. Retrying...",
                        callbackContext.getRemoveTargetsResponse().failedEntryCount()));

                // Build a new request from failed entries
                ArrayList<String> failedEntryIds = new ArrayList<>();
                for (RemoveTargetsResultEntry failedEntry : callbackContext.getRemoveTargetsResponse()
                        .failedEntries()) {
                    failedEntryIds.add(failedEntry.targetId());
                    logger.log(failedEntry.errorMessage());
                }

                RemoveTargetsRequest removeTargetsRequest = Translator.translateToRemoveTargetsRequest(compositePID,
                        failedEntryIds);

                // Retry request
                callbackContext
                        .setRetryAttemptsForRemoveTargets(callbackContext.getRetryAttemptsForRemoveTargets() + 1);
                callbackContext.setRemoveTargetsResponse(proxyClient.injectCredentialsAndInvokeV2(removeTargetsRequest,
                        proxyClient.client()::removeTargets));
            } else {
                logger.log("Failed to remove Targets.");
                throw AwsServiceException.builder()
                        .awsErrorDetails(AwsErrorDetails.builder().errorCode("FailedEntries (remove)").build())
                        .build();
            }
        }

        return !hasFailedEntries;
    }

    /**
     * Determines whether PutRule has stabilized.
     *
     * @param proxyClient  The client used to read the resource
     * @param compositePID CompositePID object
     * @param logger       The logger
     * @param stackId      The stack id (sued for logging
     * @return Whether the request has stabilized
     */
    static boolean stabilizePutRule(ProxyClient<CloudWatchEventsClient> proxyClient, CompositePID compositePID,
                                    Logger logger, String stackId) {
        boolean stabilized;

        try {
            proxyClient.injectCredentialsAndInvokeV2(
                    Translator.translateToDescribeRuleRequest(compositePID),
                    proxyClient.client()::describeRule);

            stabilized = true;
        } catch (ResourceNotFoundException e) {
            stabilized = false;
        }

        logger.log(String.format("StackId: %s: %s [%s] has been stabilized: %s", stackId, ResourceModel.TYPE_NAME,
                compositePID.getEventRuleName(), stabilized));
        return stabilized;
    }

    /**
     * Determines whether PutTargets has stabilized.
     *
     * @param awsResponse     The response from the first call to PutTargets
     * @param proxyClient     The client used to read the resource and retry if necessary
     * @param model           The model used to generate a read request
     * @param callbackContext The CallbackContext containing the number of retries attempted and the response from the last attempt
     * @param logger          The logger
     * @param stackId         The stack id (used for logging)
     * @return Whether the request has stabilized
     */
    static boolean stabilizePutTargets(PutTargetsResponse awsResponse, ProxyClient<CloudWatchEventsClient> proxyClient, ResourceModel model, CallbackContext callbackContext, Logger logger, String stackId, CompositePID compositePID) {

        if (callbackContext.getPutTargetsResponse() == null) {
            callbackContext.setPutTargetsResponse(awsResponse);
        }

        boolean stabilized = mitigateFailedPutTargets(proxyClient, model, callbackContext, logger, compositePID);

        logger.log(String.format("StackId: %s: %s [%s] have been stabilized: %s", stackId, "AWS::Events::Target", model.getTargets().size(), stabilized));
        return stabilized;
    }

    /**
     * Determines whether RemoveTargets has stabilized.
     *
     * @param awsResponse       The response from the first call to RemoveTargets
     * @param proxyClient       The client used to read the resource and retry if
     *                          necessary
     * @param compositePID      CompositePID object
     * @param callbackContext   The CallbackContext containing the number of retries
     *                          attempted and the response from the last attempt
     * @param logger            The logger
     * @param stackId           The stack id (used for logging)
     * @return Whether the request has stabilized
     */
    static boolean stabilizeRemoveTargets(RemoveTargetsResponse awsResponse, ProxyClient<CloudWatchEventsClient> proxyClient,
                                          CompositePID compositePID, CallbackContext callbackContext,
                                          Logger logger, String stackId) {

        if (callbackContext.getRemoveTargetsResponse() == null) {
            callbackContext.setRemoveTargetsResponse(awsResponse);
        }

        boolean stabilized = mitigateFailedRemoveTargets(proxyClient, compositePID, callbackContext, logger);

        logger.log(String.format("StackId: %s: %s delete has stabilized: %s", stackId, "AWS::Events::Target", stabilized));
        return stabilized;
    }


    /**
     * Calls PutRule and returns the result.
     *
     * @param awsRequest  The PutRuleRequest
     * @param proxyClient The client used to make the request
     * @param logger      The logger
     * @param stackId     The stack id (used for logging)
     * @return The PutRuleResponse
     */
    static PutRuleResponse putRule(PutRuleRequest awsRequest, ProxyClient<CloudWatchEventsClient> proxyClient, Logger logger, String stackId) {
        PutRuleResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::putRule);
        logger.log(String.format("StackId: %s: %s [%s] has successfully been updated.", stackId, ResourceModel.TYPE_NAME, awsRequest.name()));
        return awsResponse;
    }

    /**
     * Calls DeleteRule and returns the result.
     *
     * @param awsRequest  The DeleteRuleRequest
     * @param proxyClient The client used to make the request
     * @param logger      The logger
     * @param stackId     The stack id (used for logging)
     * @return The DeleteRuleResponse
     */
    static DeleteRuleResponse deleteRule(DeleteRuleRequest awsRequest, ProxyClient<CloudWatchEventsClient> proxyClient, Logger logger, String stackId) {
        DeleteRuleResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::deleteRule);
        logger.log(String.format("StackId: %s: %s [%s] successfully deleted.", stackId, ResourceModel.TYPE_NAME, awsRequest.name()));
        return awsResponse;
    }

    /**
     * Calls DescribeRule and returns the result.
     *
     * @param awsRequest  The DescribeRuleRequest
     * @param proxyClient The client used to make the request
     * @param logger      The logger
     * @param stackId     The stack id (used for logging)
     * @return The DescribeRuleResponse
     */
    static DescribeRuleResponse describeRule(DescribeRuleRequest awsRequest, ProxyClient<CloudWatchEventsClient> proxyClient, Logger logger, String stackId) {
        DescribeRuleResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::describeRule);
        logger.log(String.format("StackId: %s: %s [%s] has successfully been read.", stackId, ResourceModel.TYPE_NAME, awsRequest.name()));
        return awsResponse;
    }

    /**
     * Calls PutTargets and returns the result.
     *
     * @param awsRequest  The PutTargetsRequest
     * @param proxyClient The client used to make the request
     * @param logger      The logger
     * @param stackId     The stack id (used for logging)
     * @return The PutTargetsResponse
     */
    static PutTargetsResponse putTargets(PutTargetsRequest awsRequest, ProxyClient<CloudWatchEventsClient> proxyClient, Logger logger, String stackId) {
        PutTargetsResponse awsResponse = null;

        if (awsRequest != null) {
            awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::putTargets);
            logger.log(String.format("StackId: %s: %s [%s] has successfully been updated.", stackId, "AWS::Events::Target", (awsRequest).targets().size()));
        }

        return awsResponse;
    }

    /**
     * Calls RemoveTargets and returns the result.
     *
     * @param awsRequest  The RemoveTargetsRequest
     * @param proxyClient The client used to make the request
     * @param logger      The logger
     * @param stackId     The stack id (used for logging)
     * @return The RemoveTargetsResponse
     */
    static RemoveTargetsResponse removeTargets(RemoveTargetsRequest awsRequest, ProxyClient<CloudWatchEventsClient> proxyClient, Logger logger, String stackId) {

        // Delete targets that should not exist after update
        RemoveTargetsResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::removeTargets);

        logger.log(String.format("StackId: %s: %s [%s] has successfully been deleted.", stackId, "AWS::Events::Target", awsRequest.ids()));
        return awsResponse;
    }

    /**
     * Calls ListTargetsByRule and returns the result.
     * @param awsRequest The ListTargetsByRuleRequest
     * @param proxyClient The client used to make the request
     * @param logger The logger
     * @param stackId The stack id (used for logging)
     * @return The ListTargetsByRuleResponse
     */
    static ListTargetsByRuleResponse listTargets(ListTargetsByRuleRequest awsRequest, ProxyClient<CloudWatchEventsClient> proxyClient, Logger logger, String stackId) {
        ListTargetsByRuleResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::listTargetsByRule);
        logger.log(String.format("StackId: %s: %s [%s] successfully read.", stackId, "AWS::Events::Target", awsResponse.targets().size()));
        return awsResponse;
    }

    /**
     * Returns a ProgressEvent with a delay that does not result in an infinite loop.
     *
     * @param progress             The ProgressEvent object
     * @param callbackDelaySeconds The length of the delay
     * @param delayCount           Which call to delayedProgress this is. For example: if this function is called twice in one
     *                             handler, the delayCount of the first call will be 1, and the delayCount of the second call will
     *                             be 2.
     * @return A ProgressEvent with a 30-second delay on the first invocation, and a normal ProgressEvent on subsequent
     * invocations.
     */
    static ProgressEvent<ResourceModel, CallbackContext> delayedProgress(ProgressEvent<ResourceModel, CallbackContext> progress, int callbackDelaySeconds, int delayCount) {
        ProgressEvent<ResourceModel, CallbackContext> progressEvent;

        if (progress.getCallbackContext().getCompletedPropagationDelays() < delayCount) {
            progress.getCallbackContext().setCompletedPropagationDelays(delayCount);
            progressEvent = ProgressEvent.defaultInProgressHandler(progress.getCallbackContext(), callbackDelaySeconds, progress.getResourceModel());
        } else {
            progressEvent = ProgressEvent.progress(progress.getResourceModel(), progress.getCallbackContext());
        }

        return progressEvent;
    }

    /**
     * Filters out access denied errors. This is used to maintain backwards compatibility.
     * Features like tagging were added after the initial implementation of AWS::KMS::Key,
     * and existing permissions may deny the use of these newer features.
     */
    protected ProgressEvent<ResourceModel, CallbackContext> softFailAccessDenied(
            final Supplier<ProgressEvent<ResourceModel, CallbackContext>> eventSupplier,
            final ResourceModel model,
            final CallbackContext callbackContext) {
        try {
            return eventSupplier.get();
        } catch (final CfnAccessDeniedException e) {
            return ProgressEvent.progress(model, callbackContext);
        }
    }


    private CloudWatchEventsClient getCloudWatchEventsClient() {
        return cloudWatchEventsClient;
    }

    @Override
    public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {
        return handleRequest(
                proxy,
                request,
                callbackContext != null ? callbackContext : new CallbackContext(),
                proxy.newProxy(this::getCloudWatchEventsClient),
                logger
        );
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudWatchEventsClient> proxyClient,
            final Logger logger);

    public ProgressEvent<ResourceModel, CallbackContext> handleError(final CloudWatchEventsRequest request, final Exception e, final ProxyClient<CloudWatchEventsClient> proxyClient, final ResourceModel resourceModel, final CallbackContext callbackContext) {
        logger.log(String.format("handleError for: %s", e));

        if (e.getStackTrace() != null) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.log(sw.toString());
        }

        BaseHandlerException ex;
        if (e instanceof ConcurrentModificationException) {
            ex = new CfnResourceConflictException(e);
        } else if (e instanceof LimitExceededException) {
            ex = new CfnServiceLimitExceededException(e);
        } else if (e instanceof InvalidEventPatternException) {
            ex = new CfnInvalidRequestException(e);
        } else if (e instanceof InternalException) {
            ex = new CfnInternalFailureException(e);
        } else if (e instanceof ResourceNotFoundException) {
            // READ with an invalid or missing RestApiId or AuthorizerId throws NotFoundException
            ex = new CfnNotFoundException(e);
        } else if (e instanceof CfnAlreadyExistsException) {
            // if you do a CREATE with an existing name, you get BadRequestException
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.AlreadyExists);
        } else if (e instanceof AwsServiceException) {
            if (((AwsServiceException) e).awsErrorDetails().equals("")) { // Do not touch. IDK man...
                ex = new CfnInternalFailureException(e);
            }
            if (((AwsServiceException) e).awsErrorDetails().errorCode().equals("FailedEntries (put)")) {
                ex = new CfnInternalFailureException(e);
                return ProgressEvent.failed(resourceModel, callbackContext, ex.getErrorCode(), "Target(s) failed to create/update");
            }
            if (((AwsServiceException) e).awsErrorDetails().errorCode().equals("FailedEntries (remove)")) {
                ex = new CfnInternalFailureException(e);
                return ProgressEvent.failed(resourceModel, callbackContext, ex.getErrorCode(), "Target(s) failed to be removed");
            } else {
                ex = new CfnGeneralServiceException(e);
            }
        } else { // InternalException
            ex = new CfnGeneralServiceException(e);
        }
        return ProgressEvent.failed(resourceModel, callbackContext, ex.getErrorCode(), ex.getMessage());
    }
}
