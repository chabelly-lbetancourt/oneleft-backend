package es.upm.miw.oneleft.social.infrastructure.messaging;

import es.upm.miw.oneleft.social.domain.model.Report;
import es.upm.miw.oneleft.social.domain.port.out.SafetyEvents;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

/**
 * Output adapter: the blocks go to the {@code oneleft.social} exchange, for the copies of the plans and notifications
 * services, and the reports to {@code oneleft.reports}, for the moderation (HU-033). Inside a transaction they wait
 * for the commit: a block or a report that is not saved is never announced.
 */
@Component
public class RabbitSafetyEvents implements SafetyEvents {

    private final RabbitTemplate rabbit;

    public RabbitSafetyEvents(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @Override
    public void blocked(UUID blockerId, UUID blockedId, Instant at) {
        afterCommit(RabbitConfig.SOCIAL_EXCHANGE, RabbitConfig.USER_BLOCKED,
                new SocialMessages.Block(blockerId, blockedId, at));
    }

    @Override
    public void unblocked(UUID blockerId, UUID blockedId, Instant at) {
        afterCommit(RabbitConfig.SOCIAL_EXCHANGE, RabbitConfig.USER_UNBLOCKED,
                new SocialMessages.Block(blockerId, blockedId, at));
    }

    @Override
    public void reported(Report report) {
        afterCommit(RabbitConfig.REPORTS_EXCHANGE, RabbitConfig.REPORT_CREATED,
                SocialMessages.ReportCreated.of(report));
    }

    private void afterCommit(String exchange, String routingKey, Object event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    rabbit.convertAndSend(exchange, routingKey, event);
                }
            });
        } else {
            rabbit.convertAndSend(exchange, routingKey, event);
        }
    }
}
