package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanJoined;
import es.upm.miw.oneleft.plans.domain.model.PlanLeftEvent;
import es.upm.miw.oneleft.plans.domain.model.PlanPublished;
import es.upm.miw.oneleft.plans.domain.model.PlanReminder;
import es.upm.miw.oneleft.plans.domain.model.PlanStatus;
import es.upm.miw.oneleft.plans.domain.port.out.PlanEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static es.upm.miw.oneleft.plans.PlanFixtures.padelPlan;
import static org.assertj.core.api.Assertions.assertThat;

class PlanLifecycleServiceTest {

    /** A clock the test moves forward. */
    private static final class MovingClock extends Clock {
        private Instant now = NOW;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final JoinPlanServiceTest.Plans plans = new JoinPlanServiceTest.Plans();
    private final List<PlanReminder> reminders = new ArrayList<>();
    private final PlanEventPublisher publisher = new PlanEventPublisher() {
        @Override
        public void publish(PlanPublished event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void publish(PlanJoined event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void publish(PlanLeftEvent event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void publish(PlanReminder event) {
            reminders.add(event);
        }
    };
    private final MovingClock clock = new MovingClock();
    private final PlanLifecycleService service = new PlanLifecycleService(plans,
            new OptimisticPlanUpdates(plans, TransactionOperations.withoutTransaction()), publisher, clock);

    @Test
    void remindsStartsAndFinishesEachPlanOnce() {
        var lucia = UUID.randomUUID();
        var plan = plans.save(padelPlan(ana(), Duration.ofHours(1), CLOCK).join(lucia, "Lucía", CLOCK));

        assertThat(service.advance()).isZero();

        clock.now = plan.startsAt().minus(Plan.REMINDER_LEAD);
        assertThat(service.advance()).isEqualTo(1);
        assertThat(service.advance()).isZero();
        assertThat(reminders).singleElement().satisfies(reminder ->
                assertThat(reminder.recipientIds()).containsExactly(plan.organizer().id(), lucia));

        clock.now = plan.startsAt();
        service.advance();
        assertThat(plans.data.get(plan.id()).status()).isEqualTo(PlanStatus.IN_PROGRESS);

        clock.now = plan.startsAt().plus(Plan.DURATION);
        service.advance();
        assertThat(plans.data.get(plan.id()).status()).isEqualTo(PlanStatus.FINISHED);
        assertThat(service.advance()).isZero();
        assertThat(reminders).hasSize(1);
    }

    @Test
    void aPlanFoundAfterItsStartIsNotRemindedLate() {
        var plan = plans.save(padelPlan(ana(), Duration.ofHours(1), CLOCK));

        clock.now = plan.startsAt().plusSeconds(30);
        service.advance();

        assertThat(plans.data.get(plan.id()).status()).isEqualTo(PlanStatus.IN_PROGRESS);
        assertThat(reminders).isEmpty();
    }

    @Test
    void aBusyPlanIsLeftForTheNextRun() {
        var plan = plans.save(padelPlan(ana(), Duration.ofHours(1), CLOCK));
        clock.now = plan.startsAt();
        // Another replica keeps saving it: every attempt finds a newer version
        plans.conflictsLeft = OptimisticPlanUpdates.MAX_ATTEMPTS;

        assertThat(service.advance()).isZero();
        assertThat(plans.data.get(plan.id()).status()).isEqualTo(PlanStatus.OPEN);

        assertThat(service.advance()).isEqualTo(1);
        assertThat(plans.data.get(plan.id()).status()).isEqualTo(PlanStatus.IN_PROGRESS);
    }
}
