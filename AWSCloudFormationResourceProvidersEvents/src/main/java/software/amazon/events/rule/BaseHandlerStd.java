package software.amazon.events.rule;

import static java.util.Objects.requireNonNull;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.awssdk.services.cloudwatchevents.model.CloudWatchEventsRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.ConcurrentModificationException;
import software.amazon.awssdk.services.cloudwatchevents.model.ResourceNotFoundException;
import software.amazon.awssdk.services.cloudwatchevents.model.InternalException;
import software.amazon.awssdk.services.cloudwatchevents.model.InvalidEventPatternException;
import software.amazon.awssdk.services.cloudwatchevents.model.LimitExceededException;

import software.amazon.cloudformation.exceptions.BaseHandlerException;
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

// Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers


public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {
  protected Logger logger;

  private final CloudWatchEventsClient cloudWatchEventsClient;

  protected BaseHandlerStd() {
    this(ClientBuilder.getClient());
  }

  protected BaseHandlerStd(CloudWatchEventsClient cloudWatchEventsClient) {
    this.cloudWatchEventsClient = requireNonNull(cloudWatchEventsClient);
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
    if (logger != null) { // FIXME What the hell?
      logger.log(String.format("handleError for: %s", e));
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
      if (((AwsServiceException) e).awsErrorDetails().equals("")) {
        ex = new CfnInternalFailureException(e);
      } else {
        ex = new CfnGeneralServiceException(e);
      }
    } else { // InternalException
      ex = new CfnGeneralServiceException(e);
    }
    return ProgressEvent.failed(resourceModel, callbackContext, ex.getErrorCode(), ex.getMessage());
  }
}
