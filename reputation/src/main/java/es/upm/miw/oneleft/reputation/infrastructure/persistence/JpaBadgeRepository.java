package es.upm.miw.oneleft.reputation.infrastructure.persistence;

import es.upm.miw.oneleft.reputation.domain.model.Attendance;
import es.upm.miw.oneleft.reputation.domain.model.Badge;
import es.upm.miw.oneleft.reputation.domain.model.TrackRecord;
import es.upm.miw.oneleft.reputation.domain.port.out.Badges;
import es.upm.miw.oneleft.reputation.domain.port.out.TrackRecords;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Output adapter: the track record and the badges in PostgreSQL. */
@Repository
public class JpaBadgeRepository implements TrackRecords, Badges {

    private final SpringDataBadgeRepository badges;

    public JpaBadgeRepository(SpringDataBadgeRepository badges) {
        this.badges = badges;
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
