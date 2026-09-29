package es.upm.miw.oneleft.plans.infrastructure.realtime;

import es.upm.miw.oneleft.plans.domain.model.PlanJoined;
import es.upm.miw.oneleft.plans.domain.model.PlanLeftEvent;
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

    @Test
    void aDepartureIsToldToTheOrganizerAndToWhoeverTookTheSpot() {
        var organizer = UUID.randomUUID();
        var promoted = UUID.randomUUID();
        subscriptions.subscribe(organizer);
        subscriptions.subscribe(UUID.randomUUID());
        // Whoever took the spot closed the app: the stream is found broken when writing to it and forgotten
        subscriptions.subscribe(promoted).complete();

        subscriptions.dispatch(new PlanLeftEvent(UUID.randomUUID(), organizer, "Padel", UUID.randomUUID(), "Lucía",
                promoted, "Diego", 0, true, Instant.now()));

        assertThat(subscriptions.size()).isEqualTo(2);
    }

    @Test
    void theDepartureNoticesCarryWhatEachPersonNeeds() {
        var event = new PlanLeftEvent(UUID.randomUUID(), UUID.randomUUID(), "Padel", UUID.randomUUID(), "Lucía",
                UUID.randomUUID(), "Diego", 0, true, Instant.now());
        assertThat(PlanLeftNotice.of(event))
                .isEqualTo(new PlanLeftNotice(event.planId(), "Padel", "Lucía", "Diego", 0, true));
        assertThat(SpotFreedNotice.of(event)).isEqualTo(new SpotFreedNotice(event.planId(), "Padel"));
    }
}
