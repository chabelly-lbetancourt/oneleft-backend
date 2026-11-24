package es.upm.miw.oneleft.plans.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static org.assertj.core.api.Assertions.assertThat;

/** HU-063: whoever has more reputation waits ahead on the waiting list. */
class PlanWaitlistPriorityTest {

    private static final UUID LUCIA = UUID.randomUUID();
    private static final UUID DIEGO = UUID.randomUUID();
    private static final UUID MARTA = UUID.randomUUID();
    private static final UUID JAVIER = UUID.randomUUID();
    private static final Clock LATER = Clock.fixed(NOW.plus(Duration.ofMinutes(10)), ZoneOffset.UTC);

    private static Plan fullWithLucia() {
        return Plan.publish(ana(), Activity.PADEL, "Padel match", null, new MeetingPoint("Courts", 40.39, -3.62),
                NOW.plus(Duration.ofHours(1)), 1, null, CLOCK).join(LUCIA, "Lucía", CLOCK);
    }

    @Test
    void morePriorityGoesAheadAndAmongEqualsTheOrderOfArrivalStays() {
        var waiting = fullWithLucia().joinWaitlist(DIEGO, "Diego", CLOCK)
                .joinWaitlist(MARTA, "Marta", 2, CLOCK)
                .joinWaitlist(JAVIER, "Javier", 2, LATER)
                .joinWaitlist(UUID.randomUUID(), "Nuria", 1, LATER);

        assertThat(waiting.waitlist()).extracting(Participant::name)
                .containsExactly("Marta", "Javier", "Nuria", "Diego");
        assertThat(waiting.waitlist()).extracting(Participant::priority).containsExactly(2, 2, 1, 0);
    }

    @Test
    void theFreedSpotGoesToWhoeverHasTheMostPriorityAsAnyParticipant() {
        var waiting = fullWithLucia().joinWaitlist(DIEGO, "Diego", CLOCK).joinWaitlist(MARTA, "Marta", 3, LATER);

        var left = waiting.leave(LUCIA, LATER);

        assertThat(left.promoted()).isEqualTo(new Participant(MARTA, "Marta", LATER.instant()));
        assertThat(left.promoted().priority()).isZero();
        assertThat(left.plan().isParticipant(MARTA)).isTrue();
        assertThat(left.plan().waitlist()).extracting(Participant::userId).containsExactly(DIEGO);
    }

    @Test
    void aPerkOnlyAdds() {
        assertThat(new ReputationPerks(-3, 1)).isEqualTo(ReputationPerks.BASE);
        assertThat(new ReputationPerks(2, 8).maxActivePlans()).isEqualTo(8);
        assertThat(new PlanLimitException(5).max()).isEqualTo(5);
    }
}
