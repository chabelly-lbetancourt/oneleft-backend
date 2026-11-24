package es.upm.miw.oneleft.notifications.infrastructure.persistence;

import es.upm.miw.oneleft.notifications.domain.model.MemberPerks;
import es.upm.miw.oneleft.notifications.domain.port.out.PerkCopies;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Output adapter: the copy of the perks, in PostgreSQL (HU-063). */
@Repository
class JdbcPerkCopies implements PerkCopies {

    private final NamedParameterJdbcTemplate jdbc;

    JdbcPerkCopies(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<UUID, MemberPerks> findByUsers(Collection<UUID> userIds) {
        var found = new HashMap<UUID, MemberPerks>();
        if (userIds.isEmpty()) {
            return found;
        }
        jdbc.query("""
                SELECT user_id, level, notice_weight, max_saved_alerts, occurred_at
                FROM member_perks WHERE user_id IN (:userIds)""",
                new MapSqlParameterSource("userIds", userIds), rs -> {
                    var userId = rs.getObject("user_id", UUID.class);
                    found.put(userId, new MemberPerks(userId, rs.getString("level"), rs.getDouble("notice_weight"),
                            rs.getInt("max_saved_alerts"), rs.getTimestamp("occurred_at").toInstant()));
                });
        return found;
    }

    @Override
    public void save(MemberPerks perks) {
        jdbc.update("""
                INSERT INTO member_perks (user_id, level, notice_weight, max_saved_alerts, occurred_at)
                VALUES (:userId, :level, :weight, :alerts, :at)
                ON CONFLICT (user_id) DO UPDATE
                SET level = :level, notice_weight = :weight, max_saved_alerts = :alerts, occurred_at = :at
                WHERE member_perks.occurred_at <= :at""",
                new MapSqlParameterSource("userId", perks.userId()).addValue("level", perks.level())
                        .addValue("weight", perks.noticeWeight()).addValue("alerts", perks.maxSavedAlerts())
                        .addValue("at", Timestamp.from(perks.occurredAt())));
    }
}
