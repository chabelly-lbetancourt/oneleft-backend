package es.upm.miw.oneleft.notifications.application;

import es.upm.miw.oneleft.notifications.domain.model.PlanCancellation;
import es.upm.miw.oneleft.notifications.domain.port.in.NotifyCancellationUseCase;
import es.upm.miw.oneleft.notifications.domain.port.out.PushSender;
import es.upm.miw.oneleft.notifications.domain.port.out.PushSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Sends the cancellation of a plan (HU-039) to the browsers of everyone who was in it or waiting for it. Like the
 * reminder, it is about a plan of their own, so neither the quiet hours nor the daily limit apply. Inside the app the
 * plans service shows it through each person's real-time stream.
 */
@Service
public class PlanCancellationNotifier implements NotifyCancellationUseCase {

    private static final Logger log = LoggerFactory.getLogger(PlanCancellationNotifier.class);

    private final PushSubscriptionRepository subscriptions;
    private final PushSender push;

    public PlanCancellationNotifier(PushSubscriptionRepository subscriptions, PushSender push) {
        this.subscriptions = subscriptions;
        this.push = push;
    }

    @Override
    public int notifyCancellation(PlanCancellation cancellation) {
        if (!push.enabled()) {
            return 0;
        }
        var sent = 0;
        for (var userId : cancellation.recipientIds()) {
            for (var subscription : subscriptions.findByUser(userId)) {
                var result = push.send(subscription, cancellation);
                if (result == PushSender.Result.SENT) {
                    sent++;
                } else if (result == PushSender.Result.GONE) {
                    subscriptions.delete(subscription.endpoint());
                }
            }
        }
        log.info("Plan {}: cancellation sent to {} browsers", cancellation.planId(), sent);
        return sent;
    }
}
