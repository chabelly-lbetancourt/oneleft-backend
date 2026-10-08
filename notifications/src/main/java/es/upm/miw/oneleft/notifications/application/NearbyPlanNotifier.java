package es.upm.miw.oneleft.notifications.application;

import es.upm.miw.oneleft.notifications.domain.model.GeoDistance;
import es.upm.miw.oneleft.notifications.domain.model.NearbyPlanNotice;
import es.upm.miw.oneleft.notifications.domain.model.NotificationPreferences;
import es.upm.miw.oneleft.notifications.domain.model.PublishedPlan;
import es.upm.miw.oneleft.notifications.domain.port.in.NotifyNearbyPlanUseCase;
import es.upm.miw.oneleft.notifications.domain.port.out.AlertRepository;
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
import java.util.UUID;

/**
 * Decides who hears about a newly published plan and delivers the notices (HU-006): people with notices on, near the
 * meeting point, interested in the activity, and people with a saved alert that matches the plan (HU-036); always
 * outside their quiet hours and under their daily limit. Each person hears about a plan once, even if several alerts
 * match it, in the app if it is open and as a system notification in the browsers they subscribed.
 * <p>
 * The clock carries the local time zone: quiet hours and the daily limit follow the local day.
 */
@Service
public class NearbyPlanNotifier implements NotifyNearbyPlanUseCase {

    private static final Logger log = LoggerFactory.getLogger(NearbyPlanNotifier.class);

    private final PreferencesRepository preferences;
    private final AlertRepository alerts;
    private final NoticeLog notices;
    private final NoticePublisher publisher;
    private final PushSubscriptionRepository subscriptions;
    private final PushSender push;
    private final Clock clock;

    @SuppressWarnings("java:S107")
    public NearbyPlanNotifier(PreferencesRepository preferences, AlertRepository alerts, NoticeLog notices,
                              NoticePublisher publisher, PushSubscriptionRepository subscriptions, PushSender push,
                              Clock clock) {
        this.preferences = preferences;
        this.alerts = alerts;
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
                deliver(person.userId(), NearbyPlanNotice.of(person.userId(), plan, person.distanceTo(plan)));
                notified++;
            }
        }
        // Saved alerts (HU-036): with the quiet hours and the daily limit of the person, or the default ones
        for (var alert : alerts.findFor(plan.activity())) {
            var person = preferences.findByUser(alert.userId())
                    .orElseGet(() -> NotificationPreferences.defaults(alert.userId()));
            if (alert.matches(plan, clock.getZone()) && !person.isQuietAt(localTime)
                    && notices.countSince(alert.userId(), startOfDay) < person.maxPerDay()
                    && notices.record(alert.userId(), plan.planId(), now)) {
                var distance = GeoDistance.meters(alert.latitude(), alert.longitude(), plan.latitude(),
                        plan.longitude());
                deliver(alert.userId(), NearbyPlanNotice.of(alert.userId(), plan, distance));
                notified++;
            }
        }
        log.info("Plan {} ({}): {} people notified", plan.planId(), plan.activity(), notified);
        return notified;
    }

    private void deliver(UUID userId, NearbyPlanNotice notice) {
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
