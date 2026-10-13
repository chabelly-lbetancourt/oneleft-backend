package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.CheckIn;
import es.upm.miw.oneleft.plans.domain.port.in.CloseAttendanceUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.AttendancePublisher;
import es.upm.miw.oneleft.plans.domain.port.out.AttendanceRegister;
import es.upm.miw.oneleft.plans.domain.port.out.CheckInRepository;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Closes the attendance of the plans whose check-in has closed, 30 minutes after their start: who arrived and who did
 * not is published once per plan. If the event cannot be published, nothing is marked and the next run tries again;
 * the reputation ignores repeated events.
 */
@Service
public class AttendanceService implements CloseAttendanceUseCase {

    /** Plans closed per run */
    static final int BATCH = 100;

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

    private final AttendanceRegister register;
    private final PlanRepository plans;
    private final CheckInRepository checkIns;
    private final AttendancePublisher events;
    private final Clock clock;

    public AttendanceService(AttendanceRegister register, PlanRepository plans, CheckInRepository checkIns,
                             AttendancePublisher events, Clock clock) {
        this.register = register;
        this.plans = plans;
        this.checkIns = checkIns;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int closeDue() {
        var now = clock.instant();
        var closed = 0;
        for (var planId : register.dueToClose(now.minus(CheckIn.CLOSES_AFTER), BATCH)) {
            var plan = plans.findById(planId);
            if (plan.isPresent()) {
                events.publish(plan.get().attendance(checkIns.findByPlan(planId), now));
                closed++;
            }
            register.markClosed(planId, now);
        }
        if (closed > 0) {
            log.info("Attendance: {} plans closed", closed);
        }
        return closed;
    }
}
