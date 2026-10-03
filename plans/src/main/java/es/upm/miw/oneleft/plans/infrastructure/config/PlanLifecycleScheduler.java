package es.upm.miw.oneleft.plans.infrastructure.config;

import es.upm.miw.oneleft.plans.domain.port.in.AdvancePlansUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the lifecycle of the plans every minute (HU-007): reminders, starts and ends. It can be turned off with
 * {@code oneleft.plans.lifecycle.enabled=false} (the tests do, to control time themselves).
 */
@Component
@ConditionalOnProperty(name = "oneleft.plans.lifecycle.enabled", havingValue = "true", matchIfMissing = true)
class PlanLifecycleScheduler {

    private final AdvancePlansUseCase lifecycle;

    PlanLifecycleScheduler(AdvancePlansUseCase lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Scheduled(fixedDelayString = "${oneleft.plans.lifecycle.interval:PT1M}",
            initialDelayString = "${oneleft.plans.lifecycle.initial-delay:PT30S}")
    void run() {
        lifecycle.advance();
    }
}
