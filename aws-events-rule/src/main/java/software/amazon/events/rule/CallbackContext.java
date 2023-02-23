package software.amazon.events.rule;

import software.amazon.awssdk.services.cloudwatchevents.model.ListTargetsByRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.PutTargetsResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.RemoveTargetsResponse;
import software.amazon.cloudformation.proxy.StdCallbackContext;

import java.util.ArrayList;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {

    private int retryAttemptsForPutTargets;
    private int retryAttemptsForRemoveTargets;
    private ListTargetsByRuleResponse listTargetsByRuleResponse;
    private PutTargetsResponse putTargetsResponse;
    private RemoveTargetsResponse removeTargetsResponse;
    private boolean ruleExists;
    private ArrayList<String> targetIdsToDelete;
    private ResourceModel.ResourceModelBuilder resourceModelBuilder;
}
