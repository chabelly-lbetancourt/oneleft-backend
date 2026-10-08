package es.upm.miw.oneleft.notifications.application;

import es.upm.miw.oneleft.notifications.domain.model.PlanCancellation;
import es.upm.miw.oneleft.notifications.domain.model.PushSubscription;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-039: the cancellation reaches the browsers of everyone in the plan. */
class PlanCancellationNotifierTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID LUCIA = UUID.randomUUID();
    private static final UUID OTHER = UUID.randomUUID();

    private final NearbyPlanNotifierTest.Subscriptions subscriptions = new NearbyPlanNotifierTest.Subscriptions();
    private final NearbyPlanNotifierTest.Push push = new NearbyPlanNotifierTest.Push();
    private final PlanCancellationNotifier notifier = new PlanCancellationNotifier(subscriptions, push);
    private final PlanCancellation cancellation = new PlanCancellation(UUID.randomUUID(), "Pádel",
            "Pistas de la Albufera", Instant.parse("2026-11-16T17:20:00Z"), List.of(ANA, LUCIA));

    private static PushSubscription browser(UUID userId, String name) {
        return new PushSubscription(userId, "https://push.example.org/" + name, "key", "auth", "es");
    }

    @Test
    void everyBrowserOfEveryoneInThePlanGetsTheCancellation() {
        subscriptions.save(browser(ANA, "ana-laptop"));
        subscriptions.save(browser(ANA, "ana-phone"));
        subscriptions.save(browser(LUCIA, "lucia"));
        subscriptions.save(browser(OTHER, "other"));

        assertThat(notifier.notifyCancellation(cancellation)).isEqualTo(3);
        assertThat(push.sent).containsExactly("cancelled:https://push.example.org/ana-laptop",
                "cancelled:https://push.example.org/ana-phone", "cancelled:https://push.example.org/lucia");
    }

    @Test
    void forgetsTheBrowsersThatDroppedTheSubscription() {
        subscriptions.save(browser(LUCIA, "lucia"));
        push.gone.add("https://push.example.org/lucia");

        assertThat(notifier.notifyCancellation(cancellation)).isZero();
        assertThat(subscriptions.all).isEmpty();
    }

    @Test
    void withoutWebPushNothingIsSent() {
        subscriptions.save(browser(LUCIA, "lucia"));
        push.enabled = false;

        assertThat(notifier.notifyCancellation(cancellation)).isZero();
        assertThat(push.sent).isEmpty();
    }
}
