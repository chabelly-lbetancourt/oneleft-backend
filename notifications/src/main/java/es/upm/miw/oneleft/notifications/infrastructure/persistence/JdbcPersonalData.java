package es.upm.miw.oneleft.notifications.infrastructure.persistence;

import es.upm.miw.oneleft.notifications.domain.port.out.PersonalData;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Output adapter: the personal data of this service in PostgreSQL (HU-045). Each table is exported by name with the
 * columns that are theirs; erasing runs in the order of the list, inside the transaction of the use case.
 */
@Repository
public class JdbcPersonalData implements PersonalData {

    private static final Map<String, String> EXPORT = new LinkedHashMap<>();

    static {
        EXPORT.put("preferences",
                "SELECT * FROM notification_preferences WHERE user_id = :userId");
        EXPORT.put("preferenceActivities",
                "SELECT activity FROM notification_preference_activity WHERE user_id = :userId");
        EXPORT.put("pushSubscriptions",
                "SELECT endpoint FROM push_subscription WHERE user_id = :userId");
        EXPORT.put("sentNotices",
                "SELECT plan_id, sent_at FROM sent_notice WHERE user_id = :userId");
        EXPORT.put("savedAlerts",
                "SELECT * FROM saved_alert WHERE user_id = :userId");
        EXPORT.put("blocks",
                "SELECT blocker_id, blocked_id, blocked_at FROM block WHERE blocker_id = :userId");
    }

    private static final List<String> ERASE = List.of(
            "DELETE FROM sent_notice WHERE user_id = :userId",
            "DELETE FROM saved_alert WHERE user_id = :userId",
            "DELETE FROM push_subscription WHERE user_id = :userId",
            "DELETE FROM notification_preferences WHERE user_id = :userId",
            "DELETE FROM block WHERE :userId IN (blocker_id, blocked_id)");

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcPersonalData(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<String, List<Map<String, Object>>> of(UUID userId) {
        var params = new MapSqlParameterSource("userId", userId);
        var tables = new LinkedHashMap<String, List<Map<String, Object>>>();
        EXPORT.forEach((name, sql) -> tables.put(name, jdbc.queryForList(sql, params)));
        return tables;
    }

    @Override
    public void erase(UUID userId) {
        var params = new MapSqlParameterSource("userId", userId);
        for (var sql : ERASE) {
            if (sql.startsWith("SELECT")) {
                jdbc.queryForList(sql, params);
            } else {
                jdbc.update(sql, params);
            }
        }
    }
}
