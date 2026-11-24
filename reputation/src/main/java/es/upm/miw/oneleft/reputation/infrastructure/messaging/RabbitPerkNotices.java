package es.upm.miw.oneleft.reputation.infrastructure.messaging;

import es.upm.miw.oneleft.reputation.domain.model.Perks;
import es.upm.miw.oneleft.reputation.domain.port.out.PerkNotices;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

/**
 * Output adapter: what a level is worth goes to the {@code oneleft.reputation} exchange (HU-063), where each service
 * that applies a perk binds its own durable queue. Inside a transaction it waits for the commit, like the badges: a
 * score that is not saved announces nothing.
 */
@Component
public class RabbitPerkNotices implements PerkNotices {

    private final RabbitTemplate rabbit;

    public RabbitPerkNotices(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @Override
    public void changed(UUID userId, Perks perks, Instant at) {
        var notice = new ReputationMessages.PerksChanged(userId, perks.level().name(), perks.waitlistPriority(),
                perks.noticeWeight(), perks.maxActivePlans(), perks.maxSavedAlerts(), at);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(notice);
                }
            });
        } else {
            send(notice);
        }
    }

    private void send(ReputationMessages.PerksChanged notice) {
        rabbit.convertAndSend(RabbitConfig.REPUTATION_EXCHANGE, RabbitConfig.PERKS_CHANGED, notice);
    }
}
