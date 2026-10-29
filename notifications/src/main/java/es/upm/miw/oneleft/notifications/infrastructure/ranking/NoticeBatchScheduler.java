package es.upm.miw.oneleft.notifications.infrastructure.ranking;

import es.upm.miw.oneleft.notifications.domain.port.in.SendNoticeBatchesUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Input adapter: every 30 seconds, sends the batches that are due (HU-012). Off in tests that do not want it. */
@Component
@ConditionalOnProperty(name = "oneleft.notices.ranking.scheduler-enabled", havingValue = "true", matchIfMissing = true)
class NoticeBatchScheduler {

    private final SendNoticeBatchesUseCase batches;

    NoticeBatchScheduler(SendNoticeBatchesUseCase batches) {
        this.batches = batches;
    }

    @Scheduled(fixedDelayString = "PT30S", initialDelayString = "PT30S")
    void run() {
        batches.sendDue();
    }
}
