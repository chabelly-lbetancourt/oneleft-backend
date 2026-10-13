package es.upm.miw.oneleft.reputation.infrastructure.messaging;

import es.upm.miw.oneleft.reputation.domain.model.Badge;
import es.upm.miw.oneleft.reputation.domain.port.out.BadgeNotices;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Output adapter: a new badge is a notice on the {@code oneleft.notifications} exchange, which the plans service
 * delivers through the real-time stream of the person, like the notices of nearby plans (HU-006).
 */
@Component
public class RabbitBadgeNotices implements BadgeNotices {

    private final RabbitTemplate rabbit;

    public RabbitBadgeNotices(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @Override
    public void earned(UUID userId, Badge badge, Instant at) {
        rabbit.convertAndSend(RabbitConfig.NOTIFICATIONS_EXCHANGE, RabbitConfig.BADGE_EARNED,
                new ReputationMessages.BadgeEarned(userId, badge.name(), at));
    }
}
