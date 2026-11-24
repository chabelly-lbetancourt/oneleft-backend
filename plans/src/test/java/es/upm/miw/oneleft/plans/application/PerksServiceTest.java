package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.Organizer;
import es.upm.miw.oneleft.plans.domain.model.Participant;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanLimitException;
import es.upm.miw.oneleft.plans.domain.model.PlanStatus;
import es.upm.miw.oneleft.plans.domain.model.ReputationPerks;
import es.upm.miw.oneleft.plans.domain.port.in.PublishPlanCommand;
import es.upm.miw.oneleft.plans.domain.port.out.PerksDirectory;
import es.upm.miw.oneleft.plans.domain.port.out.PlanEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.COURTS;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/** HU-063: the reputation of someone lets them organize more plans at once and wait ahead on the waiting lists. */
class PerksServiceTest {

    private static final UUID LUCIA = UUID.randomUUID();
    private static final UUID DIEGO = UUID.randomUUID();
    private static final UUID MARTA = UUID.randomUUID();

    private final PlanServiceTest.InMemoryPlans plans = new PlanServiceTest.InMemoryPlans();
    private final Map<UUID, ReputationPerks> known = new HashMap<>();
    private final List<UUID> asked = new ArrayList<>();
    private final PerksDirectory perks = userId -> {
        asked.add(userId);
        return known.getOrDefault(userId, ReputationPerks.BASE);
    };
    private final PlanEventPublisher publisher = mock(PlanEventPublisher.class);
    private final PlanService planService = new PlanService(plans, publisher, new BlockedPairs(), perks, CLOCK);
    private final ParticipationService participation = new ParticipationService(
            new OptimisticPlanUpdates(plans, TransactionOperations.withoutTransaction()), publisher,
            new Availabilities(), AudienceCheck.NONE, perks, CLOCK);

    private Plan publish(Organizer organizer) {
        return planService.publish(new PublishPlanCommand(organizer, Activity.PADEL, "Padel match", null, COURTS,
                NOW.plus(Duration.ofHours(1)), 1, null));
    }

    @Test
    void everyoneOrganizesUpToFivePlansAtOnceWithoutAskingTheReputation() {
        var organizer = ana();
        for (var plan = 0; plan < ReputationPerks.BASE_PLANS; plan++) {
            publish(organizer);
        }
        assertThat(asked).isEmpty();

        assertThatThrownBy(() -> publish(organizer)).isInstanceOfSatisfying(PlanLimitException.class,
                error -> assertThat(error.max()).isEqualTo(ReputationPerks.BASE_PLANS));
        assertThat(asked).containsExactly(organizer.id());
        assertThat(plans.data).hasSize(ReputationPerks.BASE_PLANS);
    }

    @Test
    void whoHasTheReputationOrganizesMoreUpToTheirOwnLimit() {
        var organizer = ana();
        known.put(organizer.id(), new ReputationPerks(1, 6));
        for (var plan = 0; plan < 6; plan++) {
            publish(organizer);
        }

        assertThatThrownBy(() -> publish(organizer)).isInstanceOfSatisfying(PlanLimitException.class,
                error -> assertThat(error.max()).isEqualTo(6));
        // The plans of someone else do not count
        assertThat(publish(ana()).status()).isEqualTo(PlanStatus.OPEN);
    }

    @Test
    void plansThatHaveStartedDoNotCount() {
        var organizer = ana();
        for (var plan = 0; plan < ReputationPerks.BASE_PLANS; plan++) {
            publish(organizer);
        }
        var twoHoursLater = Clock.offset(CLOCK, Duration.ofHours(2));
        var later = new PlanService(plans, publisher, new BlockedPairs(), perks, twoHoursLater);

        var plan = later.publish(new PublishPlanCommand(organizer, Activity.PADEL, "Padel match", null, COURTS,
                twoHoursLater.instant().plus(Duration.ofHours(1)), 1, null));

        assertThat(plan.status()).isEqualTo(PlanStatus.OPEN);
        assertThat(asked).isEmpty();
    }

    @Test
    void whoHasMoreReputationWaitsAheadOnTheWaitingList() {
        var plan = plans.save(publish(ana()).join(LUCIA, "Lucía", CLOCK));
        known.put(MARTA, new ReputationPerks(2, 8));

        participation.joinWaitlist(plan.id(), DIEGO, "Diego");
        var waiting = participation.joinWaitlist(plan.id(), MARTA, "Marta");

        assertThat(waiting.waitlist()).extracting(Participant::name).containsExactly("Marta", "Diego");
        assertThat(participation.leave(plan.id(), LUCIA).isParticipant(MARTA)).isTrue();
    }

    @Test
    void withoutAReputationServiceEveryoneWaitsInOrderOfArrival() {
        var service = new ParticipationService(
                new OptimisticPlanUpdates(plans, TransactionOperations.withoutTransaction()), publisher,
                new Availabilities(), AudienceCheck.NONE, CLOCK);
        var plan = plans.save(publish(ana()).join(LUCIA, "Lucía", CLOCK));

        service.joinWaitlist(plan.id(), DIEGO, "Diego");

        assertThat(service.joinWaitlist(plan.id(), MARTA, "Marta").waitlist()).extracting(Participant::name)
                .containsExactly("Diego", "Marta");
        assertThat(PerksDirectory.NONE.perksOf(MARTA)).isEqualTo(ReputationPerks.BASE);
    }
}
