package software.amazon.events.rule;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * AWS::Events::Rule is a peculiar resource because it supports cross-account resource provisioning.
 * To handle that scenario in Uluru, the handler should be able to calculate the event rule name and event bus (name or arn)
 * from the Event Rule ARN (PrimaryIdentifier) OR from native physicalResourceId
 */
public class CompositePID {
    private static final String COMPOSITE_PID_FORMAT = "%s|%s";
    private static final String DEFAULT_EVENT_BUS_NAME = "default";

    private static final String EVENT_BUS_ARN_FORMAT = "arn:%s:events:%s:%s:event-bus/%s";
    private static final String EVENT_BUS_ARN_PATTERN =
            "^arn:(?<Partition>[aws[\\w-]*]*):events:(?<Region>[^:\n]*):(?<AccountId>[0-9]{12}):(?<ResourceType>event-bus)/(?<EventBusName>[^:\n]*)$";
    private static final String EVENT_RULE_ARN_PATTERN =
            "^arn:(?<Partition>[aws[\\w-]*]*):events:(?<Region>[^:\n]*):(?<AccountId>[0-9]{12}):(?<ResourceType>rule)/(?<EventRuleName>[^:\n]*)$";
    private static final String EVENT_RULE_ARN_PATTERN_WITH_EVENT_BUS =
            "^arn:(?<Partition>[aws[\\w-]*]*):events:(?<Region>[^:\n]*):(?<AccountId>[0-9]{12}):(?<ResourceType>rule)/(?<EventBusName>[^:\n]*)/(?<EventRuleName>[^:\n]*)$";

    @Getter public String pid = null;
    @Getter public String eventRuleName = null;
    @Getter public String eventBusName = null;

    public CompositePID(final ResourceModel model, final String stackOwnerAccountId) {
        if (nonNull(model.getName())) {
            eventRuleName = model.getName();
        }
        if (nonNull(model.getEventBusName())) {
            eventBusName = model.getEventBusName();
        }

        final String arnOrPhysicalResourceId = model.getArn();

        if (isNull(eventBusName)) {
            if (nonNull(arnOrPhysicalResourceId) && hasEventRuleArn(arnOrPhysicalResourceId)) {
                eventBusName = getEventBusNameFromRuleArn(arnOrPhysicalResourceId, stackOwnerAccountId);
            } else {
                eventBusName = inferBusNameFromPhysicalResourceId(arnOrPhysicalResourceId);
            }
        }

        if (isNull(eventRuleName)) {
            if (nonNull(arnOrPhysicalResourceId) && hasEventRuleArn(arnOrPhysicalResourceId)) {
                eventRuleName = getEventRuleNameFromArn(arnOrPhysicalResourceId);
            } else {
                eventRuleName = inferRuleNameFromPhysicalResourceId(arnOrPhysicalResourceId);
            }
        }

        pid = calculateEventRulePID(stackOwnerAccountId);
    }

    /**
     * Returns true if string contains Events Rule Arn pattern, false otherwise
     */
    private boolean hasEventRuleArn(final String eventRuleArnOrIdentifier) {
        return (Pattern.compile(EVENT_RULE_ARN_PATTERN).matcher(eventRuleArnOrIdentifier).find()
                || Pattern.compile(EVENT_RULE_ARN_PATTERN_WITH_EVENT_BUS).matcher(eventRuleArnOrIdentifier).find());
    }

    /**
     * For existent Event Rule resources, CFN won't have Event Rule Arn available
     * PrimaryIdentifier value is the current PhysicalResourceId value:
     * ${EventRuleName} - if rule resides in the same account and uses default event bus
     * ${EventBusName|EventRuleName} - if rule resides in the same account and uses a custom event bus
     * ${EventBusArn|EventRuleName} - If rule is created in another account
     * @return PhysicalResourceId
     */
    private String inferRuleNameFromPhysicalResourceId(final String pid) {
        final int identifierSize = pid.split("\\|").length;
        if (identifierSize > 1) {
            return pid.split("\\|")[1];
        } else {
            return pid;
        }
    }

    private String inferBusNameFromPhysicalResourceId(final String pid) {
        if (isNull(pid)) {
            return DEFAULT_EVENT_BUS_NAME;
        }
        final int identifierSize = pid.split("\\|").length;
        if (identifierSize > 1) {
            return pid.split("\\|")[0];
        } else {
            return DEFAULT_EVENT_BUS_NAME;
        }
    }

    /**
     * Return event rule name from event rule ARN
     * @param arn - Event Rule ARN
     * @return Event rule name
     */
    private String getEventRuleNameFromArn(final String arn) {
        final Matcher eventRuleArnMatcherWithBus = Pattern.compile(EVENT_RULE_ARN_PATTERN_WITH_EVENT_BUS).matcher(arn);

        if (eventRuleArnMatcherWithBus.matches()) {
            return eventRuleArnMatcherWithBus.group("EventRuleName");
        }

        final Matcher eventRuleArnMatcher = Pattern.compile(EVENT_RULE_ARN_PATTERN).matcher(arn);
        eventRuleArnMatcher.matches();
        return eventRuleArnMatcher.group("EventRuleName");
    }

    /**
     * Get Event Bus Name from Event Rule ARN
     * NOTE: Event Bus Name should be in ARN format in case Rule was created in another account
     * @param arn - Event Rule ARN
     * @return - Event Bus Name or ARN
     */
    private String getEventBusNameFromRuleArn(final String arn, final String stackOwnerAccountId) {
        String partition;
        String region;
        String accountId;
        String eventBusName = DEFAULT_EVENT_BUS_NAME;
        boolean crossAccountEventRule = false;
        final Matcher eventRuleArnDefaultBus = Pattern.compile(EVENT_RULE_ARN_PATTERN).matcher(arn);
        final Matcher eventRuleArnContainsCustomEventBus = Pattern.compile(EVENT_RULE_ARN_PATTERN_WITH_EVENT_BUS).matcher(arn);

        if (eventRuleArnDefaultBus.matches() || eventRuleArnContainsCustomEventBus.matches()) {
            partition = eventRuleArnDefaultBus.group("Partition");
            region = eventRuleArnDefaultBus.group("Region");
            accountId = eventRuleArnDefaultBus.group("AccountId");

            if (eventRuleArnContainsCustomEventBus.matches()) {
                eventBusName = eventRuleArnContainsCustomEventBus.group("EventBusName");
            }
            crossAccountEventRule = !stackOwnerAccountId.equals(accountId);

            if (crossAccountEventRule) {
                return String.format(EVENT_BUS_ARN_FORMAT, partition, region, accountId, eventBusName);
            }
        }

        return eventBusName;
    }

    /**
     * Because Event rule is a resource migrated from native, the PhysicalResourceId value must not change
     * Event Rule's PID format:
     * ${EventRuleName} - if rule resides in the same account and uses default event bus
     * ${EventBusName|EventRuleName} - if rule resides in the same account and uses a custom event bus
     * ${EventBusArn|EventRuleName} - If rule is created in another account
     * @return PhysicalResourceId
     */
    private String calculateEventRulePID(final String stackOwnerAccountId) {
        if (eventBusName.equals(DEFAULT_EVENT_BUS_NAME)) {
            return eventRuleName;
        }
        final Matcher eventBusNameContainsArn = Pattern.compile(EVENT_BUS_ARN_PATTERN).matcher(eventBusName);

        if (eventBusNameContainsArn.matches()) {
            final boolean eventBusFromSourceAccount = stackOwnerAccountId.equals(eventBusNameContainsArn.group("AccountId"));

            if (eventBusFromSourceAccount) {
                if (eventBusNameContainsArn.group("EventBusName").equals(DEFAULT_EVENT_BUS_NAME)) {
                    return eventRuleName;
                }
                return String.format(COMPOSITE_PID_FORMAT, eventBusNameContainsArn.group("EventBusName"), eventRuleName);
            }
        }

        return String.format(COMPOSITE_PID_FORMAT, eventBusName, eventRuleName);
    }
}
