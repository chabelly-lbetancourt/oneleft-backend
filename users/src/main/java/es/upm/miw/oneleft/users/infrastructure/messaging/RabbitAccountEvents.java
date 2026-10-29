package es.upm.miw.oneleft.users.infrastructure.messaging;

import es.upm.miw.oneleft.users.domain.port.out.AccountEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

/**
 * Output adapter: {@code user.deleted} on {@code oneleft.accounts} (HU-045). Inside a transaction it waits for the
 * commit: an account that was not deleted is never announced. If the broker is down at that moment the account is
 * already deleted here and in Keycloak, so the failure is logged with the person's id for the others to be erased by
 * hand: the cost of not having an outbox yet.
 */
@Component
public class RabbitAccountEvents implements AccountEvents {

    private static final Logger log = LoggerFactory.getLogger(RabbitAccountEvents.class);

    private final RabbitTemplate rabbit;

    public RabbitAccountEvents(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @Override
    public void deleted(UUID userId, Instant at) {
        var event = new AccountDeleted(userId, at);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(event);
                }
            });
        } else {
            send(event);
        }
    }

    private void send(AccountDeleted event) {
        try {
            rabbit.convertAndSend(AccountsRabbitConfig.ACCOUNTS_EXCHANGE, AccountsRabbitConfig.USER_DELETED, event);
        } catch (RuntimeException error) {
            log.error("user.deleted of {} was not published: its data in the other services stays", event.userId(),
                    error);
            throw error;
        }
    }

    /** user.deleted */
    record AccountDeleted(UUID userId, Instant occurredAt) {
    }
}
