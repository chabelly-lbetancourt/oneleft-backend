package es.upm.miw.oneleft.plans.infrastructure.config;

import es.upm.miw.oneleft.plans.domain.port.in.AdvancePlansUseCase;
import es.upm.miw.oneleft.plans.domain.port.in.CloseAttendanceUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the lifecycle of the plans every minute (HU-007): reminders, starts and ends, and closes the attendance of
 * the plans whose check-in has closed (HU-009). It can be turned off with
 * {@code oneleft.plans.lifecycle.enabled=false} (the tests do, to control time themselves).
 */
@Component
@ConditionalOnProperty(name = "oneleft.plans.lifecycle.enabled", havingValue = "true", matchIfMissing = true)
class PlanLifecycleScheduler {

    private final AdvancePlansUseCase lifecycle;
    private final CloseAttendanceUseCase attendance;

    PlanLifecycleScheduler(AdvancePlansUseCase lifecycle, CloseAttendanceUseCase attendance) {
        this.lifecycle = lifecycle;
        this.attendance = attendance;
    }

    @Scheduled(fixedDelayString = "${oneleft.plans.lifecycle.interval:PT1M}",
            initialDelayString = "${oneleft.plans.lifecycle.initial-delay:PT30S}")
    void run() {
        lifecycle.advance();
        attendance.closeDue();
    }
}
