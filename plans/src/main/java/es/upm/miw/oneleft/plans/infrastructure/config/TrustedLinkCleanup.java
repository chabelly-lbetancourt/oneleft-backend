package es.upm.miw.oneleft.plans.infrastructure.config;

import es.upm.miw.oneleft.plans.domain.port.in.TrustedLinksUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Removes the links for trusted contacts that have expired (HU-034). They already open nothing; this keeps no data
 * longer than it is needed. Off together with the lifecycle of the plans
 * ({@code oneleft.plans.lifecycle.enabled=false}).
 */
@Component
@ConditionalOnProperty(name = "oneleft.plans.lifecycle.enabled", havingValue = "true", matchIfMissing = true)
class TrustedLinkCleanup {

    private static final Logger log = LoggerFactory.getLogger(TrustedLinkCleanup.class);

    private final TrustedLinksUseCase links;

    TrustedLinkCleanup(TrustedLinksUseCase links) {
        this.links = links;
    }

    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT2M")
    void run() {
        var removed = links.purgeExpired();
        if (removed > 0) {
            log.info("Trusted links: {} expired removed", removed);
        }
    }
}
