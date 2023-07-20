package software.amazon.events.apidestination;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.ConcurrentModificationException;
import software.amazon.awssdk.services.eventbridge.model.LimitExceededException;
import software.amazon.awssdk.services.eventbridge.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.eventbridge.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.*;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.function.Supplier;

// Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

    public static final String VALIDATION_ERROR_CODE = "ValidationException";

    @Override
    public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<software.amazon.events.apidestination.ResourceModel> request,
            final software.amazon.events.apidestination.CallbackContext callbackContext,
            final Logger logger) {
        return handleRequest(
                proxy,
                request,
                callbackContext != null ? callbackContext : new software.amazon.events.apidestination.CallbackContext(),
                proxy.newProxy(ClientBuilder::getClient),
                logger
        );
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<EventBridgeClient> proxyClient,
            final Logger logger);

    protected <T> T translateAwsServiceException(String operation, Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (final ResourceAlreadyExistsException e) {
            throw new CfnAlreadyExistsException(e);
        } catch (final ConcurrentModificationException e) {
            throw new CfnResourceConflictException(e);
        } catch (final ResourceNotFoundException e) {
            throw new CfnNotFoundException(e);
        } catch (final LimitExceededException e) {
            throw new CfnServiceLimitExceededException(ResourceModel.TYPE_NAME, operation, e);
        } catch (final AwsServiceException e) {
            if (e.awsErrorDetails() != null && VALIDATION_ERROR_CODE.equals(e.awsErrorDetails().errorCode())) {
                throw new CfnInvalidRequestException(e.awsErrorDetails().errorMessage(), e);
            }
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }
    }

}
