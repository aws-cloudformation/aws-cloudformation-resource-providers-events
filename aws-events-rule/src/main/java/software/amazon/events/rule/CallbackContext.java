package software.amazon.events.rule;

import software.amazon.awssdk.services.cloudwatchevents.model.PutTargetsResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.RemoveTargetsResponse;
import software.amazon.cloudformation.proxy.StdCallbackContext;

import java.util.Collection;
import java.util.Set;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {

    private int retryAttemptsForPutTargets;
    private int retryAttemptsForRemoveTargets;
    private PutTargetsResponse putTargetsResponse;
    private RemoveTargetsResponse removeTargetsResponse;
    private boolean ruleExists;
    private ResourceModel.ResourceModelBuilder resourceModelBuilder;
    private int completedPropagationDelays;
    private Collection<String> targetIds;
}
