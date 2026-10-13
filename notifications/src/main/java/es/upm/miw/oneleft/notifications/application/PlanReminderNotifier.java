package es.upm.miw.oneleft.notifications.application;

import es.upm.miw.oneleft.notifications.domain.model.PlanReminder;
import es.upm.miw.oneleft.notifications.domain.port.in.RemindPlanUseCase;
import es.upm.miw.oneleft.notifications.domain.port.out.PushSender;
import es.upm.miw.oneleft.notifications.domain.port.out.PushSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Sends the reminder of a plan that is about to start (HU-007) to the browsers of everyone in it. Unlike the notices
 * of nearby plans, it is about a plan the person joined, so neither the quiet hours nor the daily limit apply. Inside
 * the app the plans service shows it through each person's real-time stream.
 */
@Service
public class PlanReminderNotifier implements RemindPlanUseCase {

    private static final Logger log = LoggerFactory.getLogger(PlanReminderNotifier.class);

    private final PushSubscriptionRepository subscriptions;
    private final PushSender push;

    public PlanReminderNotifier(PushSubscriptionRepository subscriptions, PushSender push) {
        this.subscriptions = subscriptions;
        this.push = push;
    }

    @Override
    public int remind(PlanReminder reminder) {
        if (!push.enabled()) {
            return 0;
        }
        var sent = 0;
        for (var userId : reminder.recipientIds()) {
            for (var subscription : subscriptions.findByUser(userId)) {
                var result = push.send(subscription, reminder);
                if (result == PushSender.Result.SENT) {
                    sent++;
                } else if (result == PushSender.Result.GONE) {
                    subscriptions.delete(subscription.endpoint());
                }
            }
        }
        log.info("Plan {}: reminder sent to {} browsers", reminder.planId(), sent);
        return sent;
    }
}
