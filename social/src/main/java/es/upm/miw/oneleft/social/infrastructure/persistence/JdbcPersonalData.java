package es.upm.miw.oneleft.social.infrastructure.persistence;

import es.upm.miw.oneleft.social.domain.port.out.PersonalData;
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
        EXPORT.put("profile",
                "SELECT user_id, name, visibility, feed_audience FROM person WHERE user_id = :userId");
        EXPORT.put("follows",
                "SELECT follower_id, followee_id, status, requested_at, accepted_at FROM follow WHERE follower_id = "
                + ":userId OR followee_id = :userId");
        EXPORT.put("blocks",
                "SELECT blocked_id, blocked_at FROM block WHERE blocker_id = :userId");
        EXPORT.put("reports",
                "SELECT id, target_type, target_id, reason, comment, status, reported_at FROM report WHERE "
                + "reporter_id = :userId");
        EXPORT.put("hiddenPlans",
                "SELECT plan_id, hidden_at FROM hidden_plan WHERE user_id = :userId");
        EXPORT.put("activity",
                "SELECT type, plan_id, plan_title, plan_activity, starts_at, occurred_at FROM activity WHERE actor_id "
                + "= :userId");
    }

    private static final List<String> ERASE = List.of(
            "DELETE FROM activity WHERE actor_id = :userId",
            "DELETE FROM hidden_plan WHERE user_id = :userId",
            "DELETE FROM person WHERE user_id = :userId");

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
