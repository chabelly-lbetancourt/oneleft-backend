package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.IssuedTrustedLink;
import es.upm.miw.oneleft.plans.domain.model.JoinRejectedException;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.MemoryForbiddenException;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException;
import es.upm.miw.oneleft.plans.domain.model.PlanStatus;
import es.upm.miw.oneleft.plans.domain.model.TrustedLink;
import es.upm.miw.oneleft.plans.domain.model.TrustedLinkNotFoundException;
import es.upm.miw.oneleft.plans.domain.model.TrustedLinkToken;
import es.upm.miw.oneleft.plans.domain.port.out.TrustedLinkRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HU-034: only who goes to a plan shares it with a trusted contact; the link shows just enough, until the plan is
 * over, and a link that shows nothing never says why.
 */
class TrustedLinkServiceTest {

    private static final UUID LUCIA = UUID.randomUUID();
    private static final UUID DIEGO = UUID.randomUUID();
    private static final UUID MARTA = UUID.randomUUID();

    static class InMemoryLinks implements TrustedLinkRepository {
        final Map<UUID, TrustedLink> data = new HashMap<>();
        final List<UUID> locked = new ArrayList<>();

        @Override
        public void lockPlan(UUID planId) {
            locked.add(planId);
        }

        @Override
        public TrustedLink save(TrustedLink link) {
            data.put(link.id(), link);
            return link;
        }

        @Override
        public Optional<TrustedLink> findByTokenHash(String tokenHash) {
            return data.values().stream().filter(link -> link.tokenHash().equals(tokenHash)).findFirst();
        }

        @Override
        public List<TrustedLink> findActive(UUID planId, UUID userId, Instant now) {
            return data.values().stream().filter(link -> link.planId().equals(planId)
                            && link.userId().equals(userId) && link.isActive(now))
                    .sorted(Comparator.comparing(TrustedLink::createdAt)).toList();
        }

        @Override
        public void delete(UUID linkId, UUID planId, UUID userId) {
            data.values().removeIf(link -> link.id().equals(linkId) && link.planId().equals(planId)
                    && link.userId().equals(userId));
        }

        @Override
        public int deleteExpired(Instant now) {
            var before = data.size();
            data.values().removeIf(link -> !link.isActive(now));
            return before - data.size();
        }
    }

    private final JoinPlanServiceTest.Plans plans = new JoinPlanServiceTest.Plans();
    private final InMemoryLinks links = new InMemoryLinks();
    private final Plan plan = plans.save(Plan.publish(ana(), Activity.PADEL, "Padel 2 vs 2", "Bring water",
            new MeetingPoint("Courts", 40.3912, -3.6287), NOW.plus(Duration.ofHours(1)), 3, null, CLOCK)
            .join(LUCIA, "Lucía", CLOCK).join(DIEGO, "Diego", CLOCK));
    private final UUID ana = plan.organizer().id();
    private final TrustedLinkService service = serviceAt(NOW);

    private TrustedLinkService serviceAt(Instant now) {
        return new TrustedLinkService(plans, links, Clock.fixed(now, ZoneOffset.UTC));
    }

    private static void assertRejected(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(JoinRejectedException.class,
                error -> assertThat(error.code()).isEqualTo(code));
    }

    private static void assertNotFound(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(TrustedLinkNotFoundException.class)
                .hasMessage("This link does not exist or is no longer available");
    }

    @Test
    void aParticipantGetsALinkThatExpiresWhenThePlanIsOverAndOnlyItsHashIsKept() {
        var issued = service.create(plan.id(), LUCIA);

        assertThat(TrustedLinkToken.isWellFormed(issued.token())).isTrue();
        assertThat(issued.link().planId()).isEqualTo(plan.id());
        assertThat(issued.link().userId()).isEqualTo(LUCIA);
        assertThat(issued.link().createdAt()).isEqualTo(NOW);
        assertThat(issued.link().expiresAt()).isEqualTo(plan.startsAt().plus(Plan.DURATION));
        assertThat(links.data.values()).singleElement().satisfies(stored -> {
            assertThat(stored.tokenHash()).isEqualTo(TrustedLinkToken.hash(issued.token()));
            assertThat(stored.toString()).doesNotContain(issued.token());
        });
        assertThat(links.locked).containsExactly(plan.id());
    }

    @Test
    void theOrganizerCanShareTheirPlanToo() {
        var view = service.open(service.create(plan.id(), ana).token());

        assertThat(view.sharedBy()).isEqualTo("Ana");
        assertThat(view.organizerName()).isEqualTo("Ana");
    }

    @Test
    void theContactSeesWhereWhenAndWithWhomAndNobodyElse() {
        var issued = service.create(plan.id(), LUCIA);

        var view = service.open(issued.token());

        assertThat(view.title()).isEqualTo("Padel 2 vs 2");
        assertThat(view.activity()).isEqualTo(Activity.PADEL);
        assertThat(view.meetingPoint()).isEqualTo(new MeetingPoint("Courts", 40.3912, -3.6287));
        assertThat(view.startsAt()).isEqualTo(plan.startsAt());
        assertThat(view.endsAt()).isEqualTo(plan.endsAt());
        assertThat(view.status()).isEqualTo(PlanStatus.OPEN);
        assertThat(view.organizerName()).isEqualTo("Ana");
        assertThat(view.sharedBy()).isEqualTo("Lucía");
        assertThat(view.expiresAt()).isEqualTo(plan.endsAt());
        // Nothing of the rest of the group, no identifiers and not the description
        assertThat(view.toString()).doesNotContain("Diego", "Bring water", ana.toString(), LUCIA.toString(),
                DIEGO.toString(), plan.id().toString());
    }

    @Test
    void whoIsNotGoingCannotShareThePlan() {
        assertThatThrownBy(() -> service.create(plan.id(), MARTA))
                .isInstanceOfSatisfying(MemoryForbiddenException.class,
                        error -> assertThat(error.code()).isEqualTo("plan.notParticipant"));
        assertThatThrownBy(() -> service.create(UUID.randomUUID(), LUCIA)).isInstanceOf(PlanNotFoundException.class);
        assertThat(links.data).isEmpty();
    }

    @Test
    void someoneOnTheWaitingListIsNotGoingYet() {
        var full = plans.save(Plan.publish(ana(), Activity.PADEL, "One missing", null,
                new MeetingPoint("Courts", 40.39, -3.62), NOW.plus(Duration.ofHours(1)), 1, null, CLOCK)
                .join(LUCIA, "Lucía", CLOCK).joinWaitlist(MARTA, "Marta", CLOCK));

        assertThatThrownBy(() -> service.create(full.id(), MARTA)).isInstanceOf(MemoryForbiddenException.class);
    }

    @Test
    void aPlanThatIsOverOrCancelledIsNotShared() {
        assertRejected(() -> serviceAt(plan.endsAt()).create(plan.id(), LUCIA), "trustedLink.planOver");

        plans.save(plan.cancel(ana, NOW).plan());
        assertRejected(() -> service.create(plan.id(), LUCIA), "trustedLink.planOver");
    }

    @Test
    void thePlanCanBeSharedWhileItIsGoingOnUntilItsLastInstant() {
        var lastInstant = plan.endsAt().minusMillis(1);

        var issued = serviceAt(lastInstant).create(plan.id(), LUCIA);

        assertThat(serviceAt(lastInstant).open(issued.token()).title()).isEqualTo("Padel 2 vs 2");
    }

    @Test
    void eachPersonHasALimitOfLinksPerPlanThatRevokingFreesAndOthersDoNotShare() {
        var issued = new ArrayList<IssuedTrustedLink>();
        for (var i = 0; i < TrustedLink.MAX_PER_PERSON; i++) {
            issued.add(service.create(plan.id(), LUCIA));
        }

        assertRejected(() -> service.create(plan.id(), LUCIA), "trustedLink.limit");
        assertThat(service.create(plan.id(), DIEGO)).isNotNull();

        service.revoke(plan.id(), LUCIA, issued.getFirst().link().id());
        assertThat(service.create(plan.id(), LUCIA)).isNotNull();
    }

    @Test
    void iSeeOnlyMyLinksOfThatPlanOldestFirstAndNotTheExpiredOnes() {
        var first = service.create(plan.id(), LUCIA);
        var second = serviceAt(NOW.plusSeconds(60)).create(plan.id(), LUCIA);
        service.create(plan.id(), DIEGO);

        assertThat(service.mine(plan.id(), LUCIA)).extracting(TrustedLink::id)
                .containsExactly(first.link().id(), second.link().id());
        assertThat(service.mine(plan.id(), MARTA)).isEmpty();
        assertThat(service.mine(UUID.randomUUID(), LUCIA)).isEmpty();
        assertThat(serviceAt(plan.endsAt()).mine(plan.id(), LUCIA)).isEmpty();
    }

    @Test
    void aRevokedLinkStopsWorkingAtOnceAndRevokingAgainChangesNothing() {
        var issued = service.create(plan.id(), LUCIA);
        var other = service.create(plan.id(), LUCIA);

        service.revoke(plan.id(), LUCIA, issued.link().id());
        service.revoke(plan.id(), LUCIA, issued.link().id());

        assertNotFound(() -> service.open(issued.token()));
        assertThat(service.open(other.token()).sharedBy()).isEqualTo("Lucía");
    }

    @Test
    void nobodyRevokesTheLinkOfSomeoneElseNorThroughAnotherPlan() {
        var issued = service.create(plan.id(), LUCIA);

        service.revoke(plan.id(), DIEGO, issued.link().id());
        service.revoke(plan.id(), ana, issued.link().id());
        service.revoke(UUID.randomUUID(), LUCIA, issued.link().id());

        assertThat(service.open(issued.token())).isNotNull();
    }

    @Test
    void theLinkExpiresExactlyWhenThePlanIsOver() {
        var issued = service.create(plan.id(), LUCIA);

        assertThat(serviceAt(plan.endsAt().minusMillis(1)).open(issued.token())).isNotNull();
        assertNotFound(() -> serviceAt(plan.endsAt()).open(issued.token()));
        assertNotFound(() -> serviceAt(plan.endsAt().plus(Duration.ofDays(1))).open(issued.token()));
    }

    @Test
    void aLinkThatNeverExistedOrIsNotEvenATokenGetsTheSameAnswer() {
        service.create(plan.id(), LUCIA);

        assertNotFound(() -> service.open(TrustedLinkToken.random()));
        assertNotFound(() -> service.open("not-a-token"));
        assertNotFound(() -> service.open(""));
        assertNotFound(() -> service.open(null));
    }

    @Test
    void theLinkOfSomeoneWhoLeftThePlanShowsNothing() {
        var issued = service.create(plan.id(), LUCIA);

        plans.save(plan.leave(LUCIA, CLOCK).plan());

        assertNotFound(() -> service.open(issued.token()));
    }

    @Test
    void theLinkShowsNothingIfThePlanIsGone() {
        var issued = service.create(plan.id(), LUCIA);

        plans.data.remove(plan.id());

        assertNotFound(() -> service.open(issued.token()));
    }

    @Test
    void theContactLearnsThatThePlanWasCancelled() {
        var issued = service.create(plan.id(), LUCIA);

        plans.save(plan.cancel(ana, NOW).plan());

        assertThat(service.open(issued.token()).status()).isEqualTo(PlanStatus.CANCELLED);
    }

    @Test
    void expiredLinksAreRemovedAndTheOthersStay() {
        var later = plans.save(Plan.publish(ana(), Activity.PADEL, "Later", null,
                new MeetingPoint("Courts", 40.39, -3.62), NOW.plus(Duration.ofHours(6)), 2, null, CLOCK)
                .join(LUCIA, "Lucía", CLOCK));
        service.create(plan.id(), LUCIA);
        service.create(plan.id(), DIEGO);
        var alive = service.create(later.id(), LUCIA);

        assertThat(serviceAt(NOW).purgeExpired()).isZero();
        assertThat(serviceAt(plan.endsAt()).purgeExpired()).isEqualTo(2);
        assertThat(links.data.keySet()).containsExactly(alive.link().id());
    }
}
