package es.upm.miw.oneleft.notifications.application;

import es.upm.miw.oneleft.notifications.domain.model.Candidate;
import es.upm.miw.oneleft.notifications.domain.model.GeoDistance;
import es.upm.miw.oneleft.notifications.domain.model.NearbyPlanNotice;
import es.upm.miw.oneleft.notifications.domain.model.NoticeHistory;
import es.upm.miw.oneleft.notifications.domain.model.NotificationPreferences;
import es.upm.miw.oneleft.notifications.domain.model.PublishedPlan;
import es.upm.miw.oneleft.notifications.domain.model.RankedCandidate;
import es.upm.miw.oneleft.notifications.domain.model.RankingSettings;
import es.upm.miw.oneleft.notifications.domain.port.in.NotifyNearbyPlanUseCase;
import es.upm.miw.oneleft.notifications.domain.port.out.AlertRepository;
import es.upm.miw.oneleft.notifications.domain.port.out.Blocks;
import es.upm.miw.oneleft.notifications.domain.port.out.NoticeBatches;
import es.upm.miw.oneleft.notifications.domain.port.out.NoticeLog;
import es.upm.miw.oneleft.notifications.domain.port.out.PreferencesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Decides who hears about a newly published plan and delivers the notices (HU-006): people with notices on, near the
 * meeting point, interested in the activity, and people with a saved alert that matches the plan (HU-036); always
 * outside their quiet hours and under their daily limit. Each person hears about a plan once, even if several alerts
 * match it, in the app if it is open and as a system notification in the browsers they subscribed. Nobody hears
 * about a plan of someone they blocked or who blocked them (HU-033).
 * <p>
 * People of the first group are ranked by how likely they are to take a spot (HU-012) and notified in batches: the
 * best now and the rest later, while the plan still has free spots ({@link NoticeBatchService}). People with a saved
 * alert asked for exactly this plan, so they are notified at once.
 * <p>
 * The clock carries the local time zone: quiet hours and the daily limit follow the local day.
 */
@Service
public class NearbyPlanNotifier implements NotifyNearbyPlanUseCase {

    private static final Logger log = LoggerFactory.getLogger(NearbyPlanNotifier.class);
    /** The recent past that tells how a person reacts to notices. */
    static final Duration HISTORY = Duration.ofDays(90);

    private final PreferencesRepository preferences;
    private final AlertRepository alerts;
    private final NoticeLog notices;
    private final NoticeDelivery delivery;
    private final Blocks blocks;
    private final CandidateRanker ranker;
    private final NoticeBatches batches;
    private final RankingSettings settings;
    private final Clock clock;

    @SuppressWarnings("java:S107")
    public NearbyPlanNotifier(PreferencesRepository preferences, AlertRepository alerts, NoticeLog notices,
                              NoticeDelivery delivery, Blocks blocks, CandidateRanker ranker, NoticeBatches batches,
                              RankingSettings settings, Clock clock) {
        this.preferences = preferences;
        this.alerts = alerts;
        this.notices = notices;
        this.delivery = delivery;
        this.blocks = blocks;
        this.ranker = ranker;
        this.batches = batches;
        this.settings = settings;
        this.clock = clock;
    }

    @Override
    public int notifyNearby(PublishedPlan plan) {
        var now = clock.instant();
        if (!plan.startsAt().isAfter(now) || plan.freeSpots() < 1) {
            return 0;
        }
        var notified = notifyPreferred(plan, now) + notifyAlerts(plan, now);
        log.info("Plan {} ({}): {} people notified", plan.planId(), plan.activity(), notified);
        return notified;
    }

    /** People who chose to hear about this kind of plan near them: ranked, the first batch now and the rest later. */
    private int notifyPreferred(PublishedPlan plan, Instant now) {
        var localTime = LocalTime.now(clock);
        var startOfDay = LocalDate.now(clock).atStartOfDay(clock.getZone()).toInstant();
        var people = new ArrayList<NotificationPreferences>();
        for (var person : preferences.findEnabledFor(plan.activity())) {
            if (person.wants(plan) && !person.isQuietAt(localTime)
                    && !blocks.between(plan.organizerId(), person.userId())
                    && notices.countSince(person.userId(), startOfDay) < person.maxPerDay()) {
                people.add(person);
            }
        }
        var ranking = ranker.rank(plan, candidates(plan, people, now, startOfDay),
                ZonedDateTime.ofInstant(now, clock.getZone()));
        var ordered = ranking.ordered();
        var first = ordered.subList(0, Math.min(settings.batchSize(), ordered.size()));
        var notified = 0;
        for (var ranked : first) {
            var userId = ranked.candidate().userId();
            if (notices.record(userId, plan.planId(), now, ranking.how(), 0)) {
                delivery.deliver(userId, NearbyPlanNotice.of(userId, plan, ranked.candidate().distanceMeters()));
                notified++;
            }
        }
        List<RankedCandidate> rest = ordered.subList(first.size(), ordered.size());
        if (!rest.isEmpty()) {
            batches.schedule(plan, ranking.how(), rest, now.plus(settings.interval()));
        }
        return notified;
    }

    private List<Candidate> candidates(PublishedPlan plan, List<NotificationPreferences> people, Instant now,
                                       Instant startOfDay) {
        var history = notices.historyOf(people.stream().map(NotificationPreferences::userId).toList(),
                now.minus(HISTORY), startOfDay);
        return people.stream()
                .map(p -> new Candidate(p.userId(), p.distanceTo(plan), p.radiusMeters(),
                        !p.activities().isEmpty(), history.getOrDefault(p.userId(), NoticeHistory.NONE)))
                .toList();
    }

    /** Saved alerts (HU-036): with the quiet hours and the daily limit of the person, or the default ones. */
    private int notifyAlerts(PublishedPlan plan, Instant now) {
        var localTime = LocalTime.now(clock);
        var startOfDay = LocalDate.now(clock).atStartOfDay(clock.getZone()).toInstant();
        var notified = 0;
        for (var alert : alerts.findFor(plan.activity())) {
            var person = preferences.findByUser(alert.userId())
                    .orElseGet(() -> NotificationPreferences.defaults(alert.userId()));
            if (alert.matches(plan, clock.getZone()) && !person.isQuietAt(localTime)
                    && !blocks.between(plan.organizerId(), alert.userId())
                    && notices.countSince(alert.userId(), startOfDay) < person.maxPerDay()
                    && notices.record(alert.userId(), plan.planId(), now, "alert", 0)) {
                var distance = GeoDistance.meters(alert.latitude(), alert.longitude(), plan.latitude(),
                        plan.longitude());
                delivery.deliver(alert.userId(), NearbyPlanNotice.of(alert.userId(), plan, distance));
                notified++;
            }
        }
        return notified;
    }
}
