package es.upm.miw.oneleft.reputation.infrastructure.messaging;

import es.upm.miw.oneleft.reputation.domain.model.Badge;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** A badge is announced only once it is saved (backend#120). */
class RabbitBadgeNoticesTest {

    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final RabbitBadgeNotices notices = new RabbitBadgeNotices(rabbit);
    private final UUID lucia = UUID.randomUUID();

    @AfterEach
    void endTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void announce() {
        notices.earned(lucia, Badge.TEN_ACTIVITIES, Instant.parse("2026-10-16T10:00:00Z"));
    }

    @Test
    void withoutATransactionTheNoticeLeavesAtOnce() {
        announce();

        verify(rabbit).convertAndSend(eq(RabbitConfig.NOTIFICATIONS_EXCHANGE), eq(RabbitConfig.BADGE_EARNED),
                eq(new ReputationMessages.BadgeEarned(lucia, "TEN_ACTIVITIES",
                        Instant.parse("2026-10-16T10:00:00Z"))));
    }

    @Test
    void insideATransactionTheNoticeWaitsForTheCommit() {
        TransactionSynchronizationManager.initSynchronization();

        announce();
        verify(rabbit, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(rabbit).convertAndSend(eq(RabbitConfig.NOTIFICATIONS_EXCHANGE), eq(RabbitConfig.BADGE_EARNED),
                any(ReputationMessages.BadgeEarned.class));
    }

    @Test
    void aTransactionThatFailsAnnouncesNothing() {
        TransactionSynchronizationManager.initSynchronization();

        announce();
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(rabbit, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }
}
