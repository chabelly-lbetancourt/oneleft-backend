package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.CheckIn;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanAttendanceClosed;
import es.upm.miw.oneleft.plans.domain.port.out.AttendanceRegister;
import es.upm.miw.oneleft.plans.domain.port.out.CheckInRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static es.upm.miw.oneleft.plans.PlanFixtures.padelPlan;
import static org.assertj.core.api.Assertions.assertThat;

/** HU-009: the attendance of each plan is closed and published once, when its check-in has closed. */
class AttendanceServiceTest {

    private static final UUID LUCIA = UUID.randomUUID();

    private final JoinPlanServiceTest.Plans plans = new JoinPlanServiceTest.Plans();
    private final Plan plan = plans.save(padelPlan(ana(), Duration.ofHours(1), CLOCK).join(LUCIA, "Lucía", CLOCK));
    private final Instant closing = plan.startsAt().plus(CheckIn.CLOSES_AFTER);
    private final List<PlanAttendanceClosed> events = new ArrayList<>();
    private final Register register = new Register();
    private final CheckInRepository checkIns = new CheckInRepository() {
        @Override
        public Optional<CheckIn> find(UUID planId, UUID userId) {
            return Optional.empty();
        }

        @Override
        public CheckIn save(CheckIn checkIn) {
            return checkIn;
        }

        @Override
        public List<CheckIn> findByPlan(UUID planId) {
            return List.of(new CheckIn(planId, LUCIA, "Lucía", plan.startsAt()));
        }
    };

    private AttendanceService serviceAt(Instant now) {
        return new AttendanceService(register, plans, checkIns, events::add, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void theAttendanceIsPublishedOnceWhenTheCheckInCloses() {
        register.open.add(plan.id());

        assertThat(serviceAt(closing.minusSeconds(1)).closeDue()).isZero();
        assertThat(serviceAt(closing).closeDue()).isEqualTo(1);
        assertThat(serviceAt(closing.plusSeconds(60)).closeDue()).isZero();

        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.attendeeIds()).containsExactly(LUCIA);
            assertThat(event.absenteeIds()).containsExactly(plan.organizer().id());
        });
        assertThat(register.open).isEmpty();
    }

    @Test
    void aPlanThatNoLongerExistsIsClosedWithoutEvent() {
        var gone = UUID.randomUUID();
        register.open.add(gone);

        assertThat(serviceAt(closing).closeDue()).isZero();

        assertThat(events).isEmpty();
        assertThat(register.open).isEmpty();
    }

    /** Plans whose attendance is open, due when they started before the given time (as the SQL query). */
    private final class Register implements AttendanceRegister {
        private final Set<UUID> open = new LinkedHashSet<>();

        @Override
        public List<UUID> dueToClose(Instant startedBefore, int limit) {
            return open.stream().filter(id -> plans.findById(id).map(found -> !found.startsAt().isAfter(startedBefore))
                    .orElse(true)).limit(limit).toList();
        }

        @Override
        public void markClosed(UUID planId, Instant at) {
            open.remove(planId);
        }
    }
}
