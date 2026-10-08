package es.upm.miw.oneleft.plans.infrastructure.config;

import es.upm.miw.oneleft.plans.domain.port.in.AvailabilityUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Removes the free modes that have expired (HU-035). They are already ignored by every query; this only keeps the
 * table small. Off together with the lifecycle of the plans ({@code oneleft.plans.lifecycle.enabled=false}).
 */
@Component
@ConditionalOnProperty(name = "oneleft.plans.lifecycle.enabled", havingValue = "true", matchIfMissing = true)
class AvailabilityCleanup {

    private static final Logger log = LoggerFactory.getLogger(AvailabilityCleanup.class);

    private final AvailabilityUseCase availability;

    AvailabilityCleanup(AvailabilityUseCase availability) {
        this.availability = availability;
    }

    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT1M")
    void run() {
        var removed = availability.purgeExpired();
        if (removed > 0) {
            log.info("Free mode: {} expired removed", removed);
        }
    }
}
