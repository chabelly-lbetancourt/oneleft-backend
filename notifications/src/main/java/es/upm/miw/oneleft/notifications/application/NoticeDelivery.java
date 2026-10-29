package es.upm.miw.oneleft.notifications.application;

import es.upm.miw.oneleft.notifications.domain.model.NearbyPlanNotice;
import es.upm.miw.oneleft.notifications.domain.port.out.NoticePublisher;
import es.upm.miw.oneleft.notifications.domain.port.out.PushSender;
import es.upm.miw.oneleft.notifications.domain.port.out.PushSubscriptionRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Delivers a notice of a nearby plan: in the app if it is open and as a system notification in the browsers the person
 * subscribed. Shared by the first batch (HU-006) and the later ones (HU-012).
 */
@Component
public class NoticeDelivery {

    private final NoticePublisher publisher;
    private final PushSubscriptionRepository subscriptions;
    private final PushSender push;

    public NoticeDelivery(NoticePublisher publisher, PushSubscriptionRepository subscriptions, PushSender push) {
        this.publisher = publisher;
        this.subscriptions = subscriptions;
        this.push = push;
    }

    public void deliver(UUID userId, NearbyPlanNotice notice) {
        publisher.publish(notice);
        if (!push.enabled()) {
            return;
        }
        for (var subscription : subscriptions.findByUser(userId)) {
            if (push.send(subscription, notice) == PushSender.Result.GONE) {
                // The browser dropped the subscription (permission removed, data cleared...)
                subscriptions.delete(subscription.endpoint());
            }
        }
    }
}
