package es.upm.miw.oneleft.plans.infrastructure.realtime;

import es.upm.miw.oneleft.plans.domain.model.PlanJoined;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserEventSubscriptionsTest {

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final UserEventSubscriptions subscriptions = new UserEventSubscriptions(meters);

    @Test
    void countsTheConnectedUsersAndForgetsTheOnesThatAreGone() {
        var organizer = UUID.randomUUID();
        subscriptions.subscribe(organizer).complete();
        subscriptions.subscribe(UUID.randomUUID());
        assertThat(meters.get("oneleft.plans.user.subscriptions").gauge().value()).isEqualTo(2);

        subscriptions.dispatch(new PlanJoined(UUID.randomUUID(), organizer, "Padel", UUID.randomUUID(), "Lucía", 0,
                true, Instant.now()));

        assertThat(subscriptions.size()).isEqualTo(1);
    }

    @Test
    void theNoticeCarriesWhatTheOrganizerNeeds() {
        var event = new PlanJoined(UUID.randomUUID(), UUID.randomUUID(), "Padel", UUID.randomUUID(), "Lucía", 1, false,
                Instant.now());
        assertThat(PlanJoinedNotice.of(event))
                .isEqualTo(new PlanJoinedNotice(event.planId(), "Padel", "Lucía", 1, false));
    }
}
