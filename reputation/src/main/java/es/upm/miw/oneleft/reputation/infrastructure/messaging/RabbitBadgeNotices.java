package es.upm.miw.oneleft.reputation.infrastructure.messaging;

import es.upm.miw.oneleft.reputation.domain.model.Badge;
import es.upm.miw.oneleft.reputation.domain.port.out.BadgeNotices;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

/**
 * Output adapter: a new badge is a notice on the {@code oneleft.notifications} exchange, which the plans service
 * delivers through the real-time stream of the person, like the notices of nearby plans (HU-006). Inside a transaction
 * the notice waits for the commit: a badge that is not saved is never announced, and a retry announces it once.
 */
@Component
public class RabbitBadgeNotices implements BadgeNotices {

    private final RabbitTemplate rabbit;

    public RabbitBadgeNotices(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @Override
    public void earned(UUID userId, Badge badge, Instant at) {
        var notice = new ReputationMessages.BadgeEarned(userId, badge.name(), at);
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

    private void send(ReputationMessages.BadgeEarned notice) {
        rabbit.convertAndSend(RabbitConfig.NOTIFICATIONS_EXCHANGE, RabbitConfig.BADGE_EARNED, notice);
    }
}
