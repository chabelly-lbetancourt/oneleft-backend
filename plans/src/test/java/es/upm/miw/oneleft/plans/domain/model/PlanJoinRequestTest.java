package es.upm.miw.oneleft.plans.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.COURTS;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-069: the organizer chooses who comes. */
class PlanJoinRequestTest {

    private static final UUID LUCIA = UUID.randomUUID();
    private static final UUID DIEGO = UUID.randomUUID();
    private static final UUID MARTA = UUID.randomUUID();
    private final Organizer organizer = ana();

    private Plan plan(int spots, boolean approval) {
        return Plan.publish(organizer, Activity.PADEL, null, "Pádel, falta uno", null, COURTS,
                NOW.plus(Duration.ofHours(2)), spots, null, null, null, approval, CLOCK);
    }

    private static void rejected(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOf(JoinRejectedException.class)
                .satisfies(error -> assertThat(((JoinRejectedException) error).code()).isEqualTo(code));
    }

    @Test
    void aPlanThatRequiresApprovalIsNotJoinedDirectlyNorByTheWaitingList() {
        var plan = plan(1, true);

        assertThat(plan.requiresApproval()).isTrue();
        rejected(() -> plan.join(LUCIA, "Lucía", CLOCK), "plan.approvalRequired");
        rejected(() -> plan.joinWaitlist(LUCIA, "Lucía", CLOCK), "plan.approvalRequired");
        assertThat(plan(1, false).join(LUCIA, "Lucía", CLOCK).participants()).hasSize(1);
    }

    @Test
    void aRequestOnlyHasAMessageWithoutControlCharactersOfUpTo140Characters() {
        var asked = plan(2, true).request(LUCIA, "Lucía", "  ¡Me encantaría!  ", CLOCK);

        assertThat(asked.requestOf(LUCIA)).hasValueSatisfying(request -> {
            assertThat(request.status()).isEqualTo(RequestStatus.PENDING);
            assertThat(request.message()).isEqualTo("¡Me encantaría!");
        });
        assertThat(asked.occupied()).as("asking takes no spot").isZero();
        assertThat(plan(2, true).request(LUCIA, "Lucía", "  ", CLOCK).requestOf(LUCIA).orElseThrow().message()).isNull();
        for (var invalid : new String[]{"x".repeat(141), "hola\nque tal"}) {
            assertThatThrownBy(() -> plan(2, true).request(LUCIA, "Lucía", invalid, CLOCK))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Test
    void nobodyAsksTwiceOrForTheirOwnPlanOrForAPlanWithoutApprovalOrOnceStartedOrFull() {
        var plan = plan(2, true);
        var asked = plan.request(LUCIA, "Lucía", null, CLOCK);

        rejected(() -> asked.request(LUCIA, "Lucía", null, CLOCK), "plan.alreadyRequested");
        rejected(() -> plan.request(organizer.id(), "Ana", null, CLOCK), "plan.ownPlan");
        rejected(() -> plan(2, false).request(LUCIA, "Lucía", null, CLOCK), "plan.approvalNotRequired");
        var later = Clock.fixed(NOW.plus(Duration.ofHours(3)), ZoneOffset.UTC);
        rejected(() -> plan.request(DIEGO, "Diego", null, later), "plan.started");
        var full = plan.request(LUCIA, "Lucía", null, CLOCK).accept(organizer.id(), LUCIA, CLOCK).plan();
        rejected(() -> plan(1, true).request(LUCIA, "Lucía", null, CLOCK).accept(organizer.id(), LUCIA, CLOCK).plan()
                .request(DIEGO, "Diego", null, CLOCK), "plan.full");
        rejected(() -> full.request(LUCIA, "Lucía", null, CLOCK), "plan.alreadyJoined");
    }

    @Test
    void acceptingGivesTheSpotAndFillingThePlanRejectsTheRestOnTheirOwn() {
        var asked = plan(1, true).request(LUCIA, "Lucía", null, CLOCK).request(DIEGO, "Diego", "Hola", CLOCK)
                .request(MARTA, "Marta", null, CLOCK);

        var decision = asked.accept(organizer.id(), DIEGO, CLOCK);

        assertThat(decision.plan().participants()).extracting(Participant::userId).containsExactly(DIEGO);
        assertThat(decision.plan().status()).isEqualTo(PlanStatus.FULL);
        assertThat(decision.request().status()).isEqualTo(RequestStatus.ACCEPTED);
        assertThat(decision.autoRejected()).extracting(JoinRequest::userId).containsExactlyInAnyOrder(LUCIA, MARTA);
        assertThat(decision.plan().requests()).extracting(JoinRequest::status)
                .containsExactlyInAnyOrder(RequestStatus.REJECTED, RequestStatus.ACCEPTED, RequestStatus.REJECTED);
    }

    @Test
    void acceptingDoesNotRejectTheRestWhileThereAreFreeSpots() {
        var asked = plan(2, true).request(LUCIA, "Lucía", null, CLOCK).request(DIEGO, "Diego", null, CLOCK);

        var decision = asked.accept(organizer.id(), LUCIA, CLOCK);

        assertThat(decision.plan().status()).isEqualTo(PlanStatus.OPEN);
        assertThat(decision.autoRejected()).isEmpty();
        assertThat(decision.plan().requestOf(DIEGO).orElseThrow().status()).isEqualTo(RequestStatus.PENDING);
    }

    @Test
    void onlyTheOrganizerDecidesAndOnlyOnAPendingRequest() {
        var asked = plan(2, true).request(LUCIA, "Lucía", null, CLOCK);

        assertThatThrownBy(() -> asked.accept(LUCIA, LUCIA, CLOCK)).isInstanceOf(NotOrganizerException.class);
        assertThatThrownBy(() -> asked.reject(DIEGO, LUCIA, CLOCK)).isInstanceOf(NotOrganizerException.class);
        rejected(() -> asked.accept(organizer.id(), DIEGO, CLOCK), "plan.noPendingRequest");
        var rejectedOne = asked.reject(organizer.id(), LUCIA, CLOCK);
        assertThat(rejectedOne.request().status()).isEqualTo(RequestStatus.REJECTED);
        rejected(() -> rejectedOne.plan().accept(organizer.id(), LUCIA, CLOCK), "plan.noPendingRequest");
        rejected(() -> rejectedOne.plan().request(LUCIA, "Lucía", null, CLOCK), "plan.requestRejected");
    }

    @Test
    void whoWithdrawsCanAskAgainAndOnlyTheirOwnPendingRequest() {
        var asked = plan(2, true).request(LUCIA, "Lucía", null, CLOCK);

        var withdrawn = asked.withdraw(LUCIA, CLOCK);

        assertThat(withdrawn.requestOf(LUCIA).orElseThrow().status()).isEqualTo(RequestStatus.WITHDRAWN);
        rejected(() -> withdrawn.withdraw(LUCIA, CLOCK), "plan.noPendingRequest");
        rejected(() -> asked.withdraw(DIEGO, CLOCK), "plan.noPendingRequest");
        assertThat(withdrawn.request(LUCIA, "Lucía", "otra vez", CLOCK).requestOf(LUCIA).orElseThrow().status())
                .isEqualTo(RequestStatus.PENDING);
    }

    @Test
    void aPendingRequestExpiresWhenThePlanStarts() {
        var asked = plan(2, true).request(LUCIA, "Lucía", null, CLOCK);
        var request = asked.requestOf(LUCIA).orElseThrow();
        var started = NOW.plus(Duration.ofHours(2));

        assertThat(asked.statusOf(request, NOW.plus(Duration.ofHours(1)))).isEqualTo(RequestStatus.PENDING);
        assertThat(asked.statusOf(request, started)).isEqualTo(RequestStatus.EXPIRED);
        var late = Clock.fixed(started, ZoneOffset.UTC);
        rejected(() -> asked.accept(organizer.id(), LUCIA, late), "plan.noPendingRequest");
        rejected(() -> asked.withdraw(LUCIA, late), "plan.noPendingRequest");
        assertThat(asked.reject(organizer.id(), LUCIA, CLOCK).request().status()).isEqualTo(RequestStatus.REJECTED);
    }

    @Test
    void whenSomeoneLeavesTheSpotReopensToRequestsAndNobodyIsPromoted() {
        var full = plan(1, true).request(LUCIA, "Lucía", null, CLOCK).accept(organizer.id(), LUCIA, CLOCK).plan();

        var left = full.leave(LUCIA, CLOCK).plan();

        assertThat(left.status()).isEqualTo(PlanStatus.OPEN);
        assertThat(left.freeSpots()).isEqualTo(1);
        assertThat(left.waitlist()).isEmpty();
        assertThat(left.request(DIEGO, "Diego", null, CLOCK).requestOf(DIEGO)).isPresent();
    }
}
