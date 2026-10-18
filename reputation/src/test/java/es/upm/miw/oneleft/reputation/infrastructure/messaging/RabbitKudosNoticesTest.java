package es.upm.miw.oneleft.reputation.infrastructure.messaging;

import es.upm.miw.oneleft.reputation.domain.model.Kudos;
import es.upm.miw.oneleft.reputation.domain.model.KudosKind;
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

/** A kudos is told to whoever receives it only once it is saved (HU-060). */
class RabbitKudosNoticesTest {

    private static final Instant AT = Instant.parse("2026-10-18T12:00:00Z");
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final RabbitKudosNotices notices = new RabbitKudosNotices(rabbit);
    private final Kudos kudos = new Kudos(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            KudosKind.GOOD_VIBES, AT);

    @AfterEach
    void endTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void theNoticeCarriesWhoGaveItAndWhat() {
        notices.received(kudos, "Lucía");

        verify(rabbit).convertAndSend(eq(RabbitConfig.NOTIFICATIONS_EXCHANGE), eq(RabbitConfig.KUDOS_RECEIVED),
                eq(new ReputationMessages.KudosReceived(kudos.receiverId(), "Lucía", "GOOD_VIBES", kudos.planId(),
                        AT)));
    }

    @Test
    void insideATransactionTheNoticeWaitsForTheCommit() {
        TransactionSynchronizationManager.initSynchronization();

        notices.received(kudos, "Lucía");
        verify(rabbit, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(rabbit).convertAndSend(eq(RabbitConfig.NOTIFICATIONS_EXCHANGE), eq(RabbitConfig.KUDOS_RECEIVED),
                any(ReputationMessages.KudosReceived.class));
    }
}
