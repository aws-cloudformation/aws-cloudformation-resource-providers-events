package software.amazon.events.eventbus;

import software.amazon.cloudformation.proxy.StdCallbackContext;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {
    private boolean eventBusExists;
    private ResourceModel.ResourceModelBuilder resourceModelBuilder;
    private boolean propagationDelay = false;
}
