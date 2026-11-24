package es.upm.miw.oneleft.reputation.infrastructure.messaging;

import es.upm.miw.oneleft.reputation.domain.model.Level;
import es.upm.miw.oneleft.reputation.domain.model.Perks;
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

/** HU-063: what a level is worth is announced with its numbers, and only once the score is saved. */
class RabbitPerkNoticesTest {

    private static final Instant AT = Instant.parse("2026-11-24T10:00:00Z");
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final RabbitPerkNotices notices = new RabbitPerkNotices(rabbit);
    private final UUID lucia = UUID.randomUUID();

    @AfterEach
    void endTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void announce() {
        notices.changed(lucia, Perks.of(Level.REGULAR), AT);
    }

    @Test
    void withoutATransactionTheNoticeLeavesAtOnceWithTheNumbersOfTheLevel() {
        announce();

        verify(rabbit).convertAndSend(eq(RabbitConfig.REPUTATION_EXCHANGE), eq(RabbitConfig.PERKS_CHANGED),
                eq(new ReputationMessages.PerksChanged(lucia, "REGULAR", 1, 1.1, 6, 6, AT)));
    }

    @Test
    void insideATransactionTheNoticeWaitsForTheCommit() {
        TransactionSynchronizationManager.initSynchronization();

        announce();
        verify(rabbit, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(rabbit).convertAndSend(eq(RabbitConfig.REPUTATION_EXCHANGE), eq(RabbitConfig.PERKS_CHANGED),
                any(ReputationMessages.PerksChanged.class));
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
