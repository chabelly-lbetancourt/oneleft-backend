package es.upm.miw.oneleft.notifications.application;

import es.upm.miw.oneleft.notifications.domain.model.NearbyPlanNotice;
import es.upm.miw.oneleft.notifications.domain.model.NotificationPreferences;
import es.upm.miw.oneleft.notifications.domain.model.PublishedPlan;
import es.upm.miw.oneleft.notifications.domain.port.in.NotifyNearbyPlanUseCase;
import es.upm.miw.oneleft.notifications.domain.port.out.NoticeLog;
import es.upm.miw.oneleft.notifications.domain.port.out.NoticePublisher;
import es.upm.miw.oneleft.notifications.domain.port.out.PreferencesRepository;
import es.upm.miw.oneleft.notifications.domain.port.out.PushSender;
import es.upm.miw.oneleft.notifications.domain.port.out.PushSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Decides who hears about a newly published plan and delivers the notices (HU-006): people with notices on, near the
 * meeting point, interested in the activity, outside their quiet hours and under their daily limit. Each person hears
 * about a plan once, in the app if it is open and as a system notification in the browsers they subscribed.
 * <p>
 * The clock carries the local time zone: quiet hours and the daily limit follow the local day.
 */
@Service
public class NearbyPlanNotifier implements NotifyNearbyPlanUseCase {

    private static final Logger log = LoggerFactory.getLogger(NearbyPlanNotifier.class);

    private final PreferencesRepository preferences;
    private final NoticeLog notices;
    private final NoticePublisher publisher;
    private final PushSubscriptionRepository subscriptions;
    private final PushSender push;
    private final Clock clock;

    public NearbyPlanNotifier(PreferencesRepository preferences, NoticeLog notices, NoticePublisher publisher,
                              PushSubscriptionRepository subscriptions, PushSender push, Clock clock) {
        this.preferences = preferences;
        this.notices = notices;
        this.publisher = publisher;
        this.subscriptions = subscriptions;
        this.push = push;
        this.clock = clock;
    }

    @Override
    public int notifyNearby(PublishedPlan plan) {
        var now = clock.instant();
        if (!plan.startsAt().isAfter(now) || plan.freeSpots() < 1) {
            return 0;
        }
        var localTime = LocalTime.now(clock);
        var startOfDay = LocalDate.now(clock).atStartOfDay(clock.getZone()).toInstant();
        var notified = 0;
        for (var person : preferences.findEnabledFor(plan.activity())) {
            if (person.wants(plan) && !person.isQuietAt(localTime)
                    && notices.countSince(person.userId(), startOfDay) < person.maxPerDay()
                    && notices.record(person.userId(), plan.planId(), now)) {
                deliver(person, NearbyPlanNotice.of(person.userId(), plan, person.distanceTo(plan)));
                notified++;
            }
        }
        log.info("Plan {} ({}): {} people notified", plan.planId(), plan.activity(), notified);
        return notified;
    }

    private void deliver(NotificationPreferences person, NearbyPlanNotice notice) {
        publisher.publish(notice);
        if (!push.enabled()) {
            return;
        }
        for (var subscription : subscriptions.findByUser(person.userId())) {
            if (push.send(subscription, notice) == PushSender.Result.GONE) {
                // The browser dropped the subscription (permission removed, data cleared...)
                subscriptions.delete(subscription.endpoint());
            }
        }
    }
}
