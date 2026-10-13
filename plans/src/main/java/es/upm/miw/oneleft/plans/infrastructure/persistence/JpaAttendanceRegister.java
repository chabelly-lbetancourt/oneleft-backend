package es.upm.miw.oneleft.plans.infrastructure.persistence;

import es.upm.miw.oneleft.plans.domain.port.out.AttendanceRegister;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Output adapter: the attendance of each plan is closed once, with a column of the plan. */
@Repository
public class JpaAttendanceRegister implements AttendanceRegister {

    private final SpringDataPlanRepository plans;

    public JpaAttendanceRegister(SpringDataPlanRepository plans) {
        this.plans = plans;
    }

    @Override
    public List<UUID> dueToClose(Instant startedBefore, int limit) {
        return plans.findAttendanceDue(startedBefore, limit);
    }

    @Override
    public void markClosed(UUID planId, Instant at) {
        plans.markAttendanceClosed(planId, at);
    }
}
