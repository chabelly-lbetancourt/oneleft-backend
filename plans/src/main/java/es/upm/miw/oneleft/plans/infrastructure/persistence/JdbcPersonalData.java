package es.upm.miw.oneleft.plans.infrastructure.persistence;

import es.upm.miw.oneleft.plans.domain.port.out.PersonalData;
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
        EXPORT.put("plansOrganized",
                "SELECT id, activity, title, description, meeting_point, starts_at, spots, occupied, level, status, "
                + "published_at FROM plan WHERE organizer_id = :userId ORDER BY starts_at");
        EXPORT.put("participations",
                "SELECT plan_id, name, joined_at FROM plan_participant WHERE user_id = :userId");
        EXPORT.put("waitingLists",
                "SELECT plan_id, name, joined_at FROM plan_waitlist WHERE user_id = :userId");
        EXPORT.put("arrivals",
                "SELECT plan_id, status, minutes_late, announced_at FROM plan_arrival WHERE user_id = :userId");
        EXPORT.put("checkIns",
                "SELECT plan_id, checked_in_at FROM plan_check_in WHERE user_id = :userId");
        EXPORT.put("freeMode",
                "SELECT latitude, longitude, until, created_at FROM availability WHERE user_id = :userId");
        EXPORT.put("blocks",
                "SELECT blocker_id, blocked_id, blocked_at FROM block WHERE blocker_id = :userId");
    }

    private static final List<String> ERASE = List.of(
            "UPDATE plan SET occupied = occupied - 1, status = CASE WHEN status = 'FULL' THEN 'OPEN' ELSE status "
                + "END WHERE status IN ('OPEN', 'FULL') AND id IN (SELECT plan_id FROM plan_participant WHERE user_id = "
                + ":userId)",
            "DELETE FROM plan_participant WHERE user_id = :userId AND plan_id IN (SELECT id FROM plan WHERE "
                + "status IN ('OPEN', 'FULL'))",
            "DELETE FROM plan_waitlist WHERE user_id = :userId AND plan_id IN (SELECT id FROM plan WHERE status "
                + "IN ('OPEN', 'FULL'))",
            "UPDATE plan SET status = 'CANCELLED' WHERE organizer_id = :userId AND status IN ('OPEN', 'FULL')",
            "UPDATE plan SET organizer_name = 'Persona eliminada' WHERE organizer_id = :userId",
            "UPDATE plan_participant SET name = 'Persona eliminada' WHERE user_id = :userId",
            "UPDATE plan_waitlist SET name = 'Persona eliminada' WHERE user_id = :userId",
            "UPDATE plan_arrival SET name = 'Persona eliminada' WHERE user_id = :userId",
            "UPDATE plan_check_in SET name = 'Persona eliminada' WHERE user_id = :userId",
            "DELETE FROM availability WHERE user_id = :userId",
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
