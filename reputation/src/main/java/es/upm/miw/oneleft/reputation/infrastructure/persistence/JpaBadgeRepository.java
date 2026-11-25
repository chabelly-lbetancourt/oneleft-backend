package es.upm.miw.oneleft.reputation.infrastructure.persistence;

import es.upm.miw.oneleft.reputation.domain.model.ActivityPlans;
import es.upm.miw.oneleft.reputation.domain.model.AntiCheatRules;
import es.upm.miw.oneleft.reputation.domain.model.Attendance;
import es.upm.miw.oneleft.reputation.domain.model.Badge;
import es.upm.miw.oneleft.reputation.domain.model.MonthPlans;
import es.upm.miw.oneleft.reputation.domain.model.TrackRecord;
import es.upm.miw.oneleft.reputation.domain.port.out.Badges;
import es.upm.miw.oneleft.reputation.domain.port.out.TrackRecords;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.YearMonth;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Output adapter: the track record and the badges in PostgreSQL; who arrived at each plan, for the kudos and the
 * endorsements; and my
 * statistics (HU-043), read from the same track record.
 */
@Repository
public class JpaBadgeRepository implements TrackRecords, Badges {

    private final SpringDataBadgeRepository badges;
    private final SpringDataTrackRecordRepository lines;

    public JpaBadgeRepository(SpringDataBadgeRepository badges, SpringDataTrackRecordRepository lines) {
        this.badges = badges;
        this.lines = lines;
    }

    @Override
    public void record(List<Attendance> attendances) {
        attendances.forEach(line -> badges.recordAttendance(line.userId(), line.planId(), line.activity(),
                line.organizer(), line.arrived(), line.held(), line.closedAt()));
    }

    @Override
    public TrackRecord of(UUID userId) {
        var record = badges.trackRecordOf(userId);
        return new TrackRecord((int) record.getOrganizedHeld(), (int) record.getCompleted(),
                (int) record.getActivities(), (int) record.getStreak(), (int) record.getKudos());
    }

    @Override
    public Map<UUID, Integer> organizedHeldOf(Collection<UUID> userIds) {
        var organized = new HashMap<UUID, Integer>();
        if (!userIds.isEmpty()) {
            badges.organizedHeldOf(userIds)
                    .forEach(row -> organized.put((UUID) row[0], ((Number) row[1]).intValue()));
        }
        return organized;
    }

    @Override
    public Optional<Attendance> attendance(UUID userId, UUID planId) {
        return lines.findById(new TrackRecordEntity.Key(userId, planId)).map(TrackRecordEntity::toDomain);
    }

    @Override
    public List<Attendance> attendancesOf(UUID planId) {
        return lines.findByIdPlanId(planId).stream().map(TrackRecordEntity::toDomain).toList();
    }

    @Override
    public List<String> activitiesTogether(UUID one, UUID other) {
        return lines.activitiesTogether(one, other);
    }

    @Override
    public int joinedOf(UUID userId) {
        return Math.toIntExact(lines.joinedOf(userId));
    }

    @Override
    public List<ActivityPlans> topActivitiesOf(UUID userId, int limit) {
        return lines.topActivitiesOf(userId, limit).stream()
                .map(count -> new ActivityPlans(count.getActivity(), Math.toIntExact(count.getPlans()))).toList();
    }

    @Override
    public List<MonthPlans> monthsOf(UUID userId, Instant since) {
        return lines.monthsOf(userId, since, AntiCheatRules.ZONE.getId()).stream()
                .map(count -> new MonthPlans(YearMonth.parse(count.getMonth()), Math.toIntExact(count.getCompleted()),
                        Math.toIntExact(count.getOrganized()), Math.toIntExact(count.getJoined()))).toList();
    }

    @Override
    public boolean award(UUID userId, Badge badge, Instant at) {
        return badges.award(userId, badge.name(), at) == 1;
    }

    @Override
    public Map<Badge, Instant> earnedBy(UUID userId) {
        var earned = new EnumMap<Badge, Instant>(Badge.class);
        badges.findByIdUserId(userId).forEach(badge -> earned.put(badge.badge(), badge.earnedAt()));
        return earned;
    }
}
