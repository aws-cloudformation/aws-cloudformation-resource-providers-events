package software.amazon.events.rule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class CompositePIDTest extends AbstractTestBase {
    private static Stream<Arguments> provideParameters() {
        return Stream.of(
                Arguments.of(EVENT_RULE_ARN_DEFAULT_BUS, SOURCE_ACCOUNT_ID),
                Arguments.of(EVENT_RULE_ARN_CUSTOM_BUS, SOURCE_ACCOUNT_ID)
        );
    }
    @ParameterizedTest
    @MethodSource("provideParameters")
    public void testCompositeId(final String arn, final String accountId) {
        final ResourceModel model = ResourceModel.builder().arn(arn).build();
        final CompositePID compositePID = new CompositePID(model, accountId);

        if (arn.split("/").length == 2) {
            assertThat(compositePID.getPid()).isEqualTo(EVENT_RULE_NAME);
            assertThat(compositePID.getEventRuleName()).isEqualTo(EVENT_RULE_NAME);
            assertThat(compositePID.getEventBusName()).isEqualTo(DEFAULT_EVENT_BUS_NAME);
        } else {
            assertThat(compositePID.getPid()).isEqualTo(CUSTOM_EVENT_BUS_NAME + "|" + EVENT_RULE_NAME);
            assertThat(compositePID.getEventRuleName()).isEqualTo(EVENT_RULE_NAME);
            assertThat(compositePID.getEventBusName()).isEqualTo(CUSTOM_EVENT_BUS_NAME);
        }
    }

    private static Stream<Arguments> provideCrossAccountParameters() {
        return Stream.of(
                Arguments.of(CROSS_ACCOUNT_EVENT_RULE_ARN_CUSTOM_BUS, SOURCE_ACCOUNT_ID)
        );
    }
    @ParameterizedTest
    @MethodSource("provideCrossAccountParameters")
    public void testCompositeId_WithCrossAccountEventBusName(final String arn, final String accountId) {
        final ResourceModel model = ResourceModel.builder().arn(arn).build();
        final CompositePID compositePID = new CompositePID(model, accountId);

        assertThat(compositePID.getPid()).isEqualTo(CROSS_ACCOUNT_CUSTOM_PID_CUSTOM_BUS);
        assertThat(compositePID.getEventRuleName()).isEqualTo(EVENT_RULE_NAME);
        assertThat(compositePID.getEventBusName()).isEqualTo(CROSS_ACCOUNT_CUSTOM_EVENT_BUS_ARN);
    }

    private static Stream<Arguments> providePreviousIdentifierParameters() {
        return Stream.of(
                Arguments.of(EVENT_RULE_NAME, null, SOURCE_ACCOUNT_ID),
                Arguments.of(CUSTOM_EVENT_BUS_NAME + "|" + EVENT_RULE_NAME, CUSTOM_EVENT_BUS_NAME, SOURCE_ACCOUNT_ID),
                Arguments.of(CUSTOM_EVENT_BUS_NAME + "|" + EVENT_RULE_NAME, SAME_ACCOUNT_CUSTOM_EVENT_BUS_ARN, SOURCE_ACCOUNT_ID),
                Arguments.of(EVENT_RULE_NAME, SAME_ACCOUNT_DEFAULT_BUS_ARN, SOURCE_ACCOUNT_ID),
                Arguments.of(CROSS_ACCOUNT_CUSTOM_PID_CUSTOM_BUS, CROSS_ACCOUNT_CUSTOM_EVENT_BUS_ARN, SOURCE_ACCOUNT_ID)
        );
    }
    @ParameterizedTest
    @MethodSource("providePreviousIdentifierParameters")
    public void testCompositeId_WithPreviousIdentifier(final String identifier, final String eventBusName, final String accountId) {
        final ResourceModel model = ResourceModel.builder()
            .arn(identifier)
            .eventBusName(eventBusName)
            .build();
        final CompositePID compositePID = new CompositePID(model, accountId);

        assertThat(compositePID.getEventRuleName()).isEqualTo(EVENT_RULE_NAME);

        if (eventBusName == null) {
            assertThat(compositePID.getEventBusName()).isEqualTo(DEFAULT_EVENT_BUS_NAME);
            assertThat(compositePID.getPid()).isEqualTo(EVENT_RULE_NAME);
        } else if (eventBusName.equals(SAME_ACCOUNT_DEFAULT_BUS_ARN)) {
            assertThat(compositePID.getEventBusName()).isEqualTo(SAME_ACCOUNT_DEFAULT_BUS_ARN);
            assertThat(compositePID.getPid()).isEqualTo(EVENT_RULE_NAME);
        } else if (eventBusName.equals(SAME_ACCOUNT_CUSTOM_EVENT_BUS_ARN)) {
            assertThat(compositePID.getEventBusName()).isEqualTo(SAME_ACCOUNT_CUSTOM_EVENT_BUS_ARN);
            assertThat(compositePID.getPid()).isEqualTo(CUSTOM_EVENT_BUS_NAME + "|" + EVENT_RULE_NAME);
        } else if (eventBusName.equals(CUSTOM_EVENT_BUS_NAME)) {
            assertThat(compositePID.getEventBusName()).isEqualTo(CUSTOM_EVENT_BUS_NAME);
            assertThat(compositePID.getPid()).isEqualTo(CUSTOM_EVENT_BUS_NAME + "|" + EVENT_RULE_NAME);
        } else if (eventBusName.equals(CROSS_ACCOUNT_CUSTOM_EVENT_BUS_ARN)) {
            assertThat(compositePID.getEventBusName()).isEqualTo(CROSS_ACCOUNT_CUSTOM_EVENT_BUS_ARN);
            assertThat(compositePID.getPid()).isEqualTo(CROSS_ACCOUNT_CUSTOM_PID_CUSTOM_BUS);
        }
    }

    @Test
    public void test_compositeIPID_FromNative_ArnValueIsPhysicalResourceId() {
        final ResourceModel model = ResourceModel.builder()
                .arn(SAME_ACCOUNT_PID_DEFAULT_BUS)
                .build();
        final CompositePID compositePID = new CompositePID(model, SOURCE_ACCOUNT_ID);

        assertThat(compositePID.getEventRuleName()).isEqualTo(EVENT_RULE_NAME);
        assertThat(compositePID.getEventBusName()).isEqualTo(DEFAULT_EVENT_BUS_NAME);
        assertThat(compositePID.getPid()).isEqualTo(SAME_ACCOUNT_PID_DEFAULT_BUS);
    }

    @Test
    public void test_compositeIPID_FromNative_ArnValueIsPhysicalResourceId_CustomBus() {
        final ResourceModel model = ResourceModel.builder()
                .arn(SAME_ACCOUNT_PID_CUSTOM_BUS)
                .build();
        final CompositePID compositePID = new CompositePID(model, SOURCE_ACCOUNT_ID);

        assertThat(compositePID.getEventRuleName()).isEqualTo(EVENT_RULE_NAME);
        assertThat(compositePID.getEventBusName()).isEqualTo(CUSTOM_EVENT_BUS_NAME);
        assertThat(compositePID.getPid()).isEqualTo(SAME_ACCOUNT_PID_CUSTOM_BUS);
    }

    @Test
    public void test_compositeIPID_FromNative_ArnValueIsPhysicalResourceId_CrossAccount() {
        final ResourceModel model = ResourceModel.builder()
                .arn(CROSS_ACCOUNT_CUSTOM_PID_CUSTOM_BUS)
                .build();
        final CompositePID compositePID = new CompositePID(model, SOURCE_ACCOUNT_ID);

        assertThat(compositePID.getEventRuleName()).isEqualTo(EVENT_RULE_NAME);
        assertThat(compositePID.getEventBusName()).isEqualTo(CROSS_ACCOUNT_CUSTOM_EVENT_BUS_ARN);
        assertThat(compositePID.getPid()).isEqualTo(CROSS_ACCOUNT_CUSTOM_PID_CUSTOM_BUS);
    }

    @Test
    public void test_compositePID_FromUluru_SameAccount() {
        final ResourceModel model = ResourceModel.builder()
                .arn(EVENT_RULE_ARN_DEFAULT_BUS)
                .build();
        final CompositePID compositePID = new CompositePID(model, SOURCE_ACCOUNT_ID);

        assertThat(compositePID.getEventRuleName()).isEqualTo(EVENT_RULE_NAME);
        assertThat(compositePID.getEventBusName()).isEqualTo(DEFAULT_EVENT_BUS_NAME);
        assertThat(compositePID.getPid()).isEqualTo(SAME_ACCOUNT_PID_DEFAULT_BUS);
    }

    @Test
    public void test_compositePID_FromUluru_SameAccount_CustomBus() {
        final ResourceModel model = ResourceModel.builder()
                .arn(EVENT_RULE_ARN_CUSTOM_BUS)
                .build();
        final CompositePID compositePID = new CompositePID(model, SOURCE_ACCOUNT_ID);

        assertThat(compositePID.getEventRuleName()).isEqualTo(EVENT_RULE_NAME);
        assertThat(compositePID.getEventBusName()).isEqualTo(CUSTOM_EVENT_BUS_NAME);
        assertThat(compositePID.getPid()).isEqualTo(SAME_ACCOUNT_PID_CUSTOM_BUS);
    }

    @Test
    public void test_compositePID_FromUluru_CrossAccount() {
        final ResourceModel model = ResourceModel.builder()
                .arn(CROSS_ACCOUNT_EVENT_RULE_ARN_CUSTOM_BUS)
                .build();
        final CompositePID compositePID = new CompositePID(model, SOURCE_ACCOUNT_ID);

        assertThat(compositePID.getEventRuleName()).isEqualTo(EVENT_RULE_NAME);
        assertThat(compositePID.getEventBusName()).isEqualTo(CROSS_ACCOUNT_CUSTOM_EVENT_BUS_ARN);
        assertThat(compositePID.getPid()).isEqualTo(CROSS_ACCOUNT_CUSTOM_PID_CUSTOM_BUS);
    }
}
