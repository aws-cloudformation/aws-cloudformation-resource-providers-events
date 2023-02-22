package software.amazon.events.rule;

public class CompositeId {
    public final String ruleName;
    public final String eventBusName;

    public CompositeId(final ResourceModel model) {
        if (model.getName() != null) {
            this.ruleName = model.getName();
            this.eventBusName = model.getEventBusName();
        } else { // if (model.getArn() != null) {
            String ruleName;
            String eventBusName = null;
            String[] splitArn = model.getArn().split("/", 0);

            if (splitArn.length == 2) {
                ruleName = splitArn[1];
            } else { // if (splitArn.length == 3
                ruleName = splitArn[2];
                eventBusName = splitArn[1];
            }

            this.ruleName = ruleName;
            this.eventBusName = eventBusName;
        }
    }
}
