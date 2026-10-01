package es.upm.miw.oneleft.notifications.infrastructure.messaging;

import es.upm.miw.oneleft.notifications.domain.model.NearbyPlanNotice;
import es.upm.miw.oneleft.notifications.domain.port.out.NoticePublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Output adapter: the notice goes to the plans service, which keeps each person's real-time stream open, so the app
 * needs a single connection for every kind of notice.
 */
@Component
class RabbitNoticePublisher implements NoticePublisher {

    private final RabbitTemplate rabbit;

    RabbitNoticePublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @Override
    public void publish(NearbyPlanNotice notice) {
        rabbit.convertAndSend(RabbitConfig.NOTIFICATIONS_EXCHANGE, RabbitConfig.NEARBY_PLAN, notice);
    }
}
