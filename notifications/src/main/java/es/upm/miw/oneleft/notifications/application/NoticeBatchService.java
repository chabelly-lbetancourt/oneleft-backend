package es.upm.miw.oneleft.notifications.application;

import es.upm.miw.oneleft.notifications.domain.model.BatchedPlan;
import es.upm.miw.oneleft.notifications.domain.model.NearbyPlanNotice;
import es.upm.miw.oneleft.notifications.domain.model.PublishedPlan;
import es.upm.miw.oneleft.notifications.domain.model.RankingSettings;
import es.upm.miw.oneleft.notifications.domain.port.in.SendNoticeBatchesUseCase;
import es.upm.miw.oneleft.notifications.domain.port.out.Blocks;
import es.upm.miw.oneleft.notifications.domain.port.out.NoticeBatches;
import es.upm.miw.oneleft.notifications.domain.port.out.NoticeLog;
import es.upm.miw.oneleft.notifications.domain.port.out.PreferencesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Sends the later batches of notices (HU-012): while a plan has free spots and is not about to start, the next people
 * of its ranking are notified every few minutes. Time has passed since the plan was published, so each person is
 * checked again: notices still on, still near, outside their quiet hours, under their daily limit and not blocked.
 */
@Service
public class NoticeBatchService implements SendNoticeBatchesUseCase {

    private static final Logger log = LoggerFactory.getLogger(NoticeBatchService.class);

    private final NoticeBatches batches;
    private final PreferencesRepository preferences;
    private final NoticeLog notices;
    private final NoticeDelivery delivery;
    private final Blocks blocks;
    private final RankingSettings settings;
    private final Clock clock;

    @SuppressWarnings("java:S107")
    public NoticeBatchService(NoticeBatches batches, PreferencesRepository preferences, NoticeLog notices,
                              NoticeDelivery delivery, Blocks blocks, RankingSettings settings, Clock clock) {
        this.batches = batches;
        this.preferences = preferences;
        this.notices = notices;
        this.delivery = delivery;
        this.blocks = blocks;
        this.settings = settings;
        this.clock = clock;
    }

    @Override
    public int sendDue() {
        var now = clock.instant();
        var sent = 0;
        for (var batched : batches.due(now)) {
            sent += sendBatch(batched, now);
        }
        return sent;
    }

    private int sendBatch(BatchedPlan batched, Instant now) {
        var plan = batched.plan();
        if (!plan.startsAt().minus(settings.cutoff()).isAfter(now)) {
            batches.close(plan.planId());
            return 0;
        }
        var sent = 0;
        for (var userId : batches.takeNext(plan.planId(), settings.batchSize())) {
            var distance = distanceIfWanted(userId, plan);
            if (distance >= 0 && notices.record(userId, plan.planId(), now, batched.ranking(), batched.nextBatch())) {
                delivery.deliver(userId, NearbyPlanNotice.of(userId, plan, distance));
                sent++;
            }
        }
        batches.rescheduleOrFinish(plan.planId(), now.plus(settings.interval()));
        log.info("Plan {}: batch {} sent to {} people", plan.planId(), batched.nextBatch(), sent);
        return sent;
    }

    /** Distance to the plan if the person may be notified now, or -1. */
    private double distanceIfWanted(UUID userId, PublishedPlan plan) {
        var person = preferences.findByUser(userId).orElse(null);
        var startOfDay = LocalDate.now(clock).atStartOfDay(clock.getZone()).toInstant();
        if (person == null || !person.wants(plan) || person.isQuietAt(LocalTime.now(clock))
                || blocks.between(plan.organizerId(), userId)
                || notices.countSince(userId, startOfDay) >= person.maxPerDay()) {
            return -1;
        }
        return person.distanceTo(plan);
    }
}
