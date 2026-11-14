package es.upm.miw.oneleft.users.infrastructure.messaging;

import es.upm.miw.oneleft.users.domain.model.Gender;
import es.upm.miw.oneleft.users.domain.port.out.DemographicsEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

/**
 * Output adapter: {@code demographics.changed} on {@code oneleft.accounts} (HU-070). Inside a transaction it waits
 * for the commit. If the broker is down the data is already saved here: the failure is logged and does not undo it;
 * the other services learn the new age on the next birthday or the next change.
 */
@Component
public class RabbitDemographicsEvents implements DemographicsEvents {

    private static final Logger log = LoggerFactory.getLogger(RabbitDemographicsEvents.class);

    private final RabbitTemplate rabbit;

    public RabbitDemographicsEvents(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @Override
    public void changed(UUID userId, Gender gender, int age, Instant at) {
        var event = new DemographicsChanged(userId, gender.name(), age, at);
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

    private void send(DemographicsChanged event) {
        try {
            rabbit.convertAndSend(AccountsRabbitConfig.ACCOUNTS_EXCHANGE, AccountsRabbitConfig.DEMOGRAPHICS_CHANGED,
                    event);
        } catch (RuntimeException error) {
            log.error("demographics.changed of {} was not published: the other services keep the previous age",
                    event.userId(), error);
        }
    }

    /** demographics.changed: gender and age in years, never the date of birth. */
    record DemographicsChanged(UUID userId, String gender, int age, Instant occurredAt) {
    }
}
