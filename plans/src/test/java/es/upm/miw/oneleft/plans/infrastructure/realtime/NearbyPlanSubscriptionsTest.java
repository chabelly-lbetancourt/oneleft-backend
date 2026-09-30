package es.upm.miw.oneleft.plans.infrastructure.realtime;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.NearbySearch;
import es.upm.miw.oneleft.plans.domain.model.PlanPublished;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.COURTS;
import static org.assertj.core.api.Assertions.assertThat;

class NearbyPlanSubscriptionsTest {

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final NearbyPlanSubscriptions subscriptions = new NearbyPlanSubscriptions(CLOCK, meters);
    private final NearbySearch search = new NearbySearch(COURTS.latitude(), COURTS.longitude(), 1_000, null, null,
            UUID.randomUUID());

    @Test
    void countsTheConnectedClientsInAGauge() {
        subscriptions.subscribe(search);
        subscriptions.subscribe(search);

        assertThat(subscriptions.size()).isEqualTo(2);
        assertThat(meters.get("oneleft.plans.nearby.subscriptions").gauge().value()).isEqualTo(2);
    }

    @Test
    void forgetsTheClientsThatAreGone() {
        var gone = subscriptions.subscribe(search);
        subscriptions.subscribe(search);
        gone.complete();

        subscriptions.heartbeat();

        assertThat(subscriptions.size()).isEqualTo(1);
    }

    @Test
    void dispatchingToAClosedClientAlsoForgetsIt() {
        subscriptions.subscribe(search).complete();
        var event = new PlanPublished(UUID.randomUUID(), UUID.randomUUID(), Activity.PADEL, "Pádel", "Pistas",
                COURTS.latitude(),
                COURTS.longitude(), NOW.plus(Duration.ofHours(1)), 1, null, NOW);

        subscriptions.dispatch(event);

        assertThat(subscriptions.size()).isZero();
    }

    @Test
    void theEventCarriesTheRoundedDistance() {
        var event = new PlanPublished(UUID.randomUUID(), UUID.randomUUID(), Activity.PADEL, "Pádel", "Pistas",
                COURTS.latitude() + 0.005,
                COURTS.longitude(), NOW.plus(Duration.ofHours(1)), 1, null, NOW);

        var nearby = NearbyPlanEvent.of(event, search);

        assertThat(nearby.planId()).isEqualTo(event.planId());
        assertThat(nearby.distanceMeters()).isEqualTo(556);
        assertThat(nearby.freeSpots()).isEqualTo(1);
    }
}
