package es.upm.miw.oneleft.reputation.infrastructure.persistence;

import es.upm.miw.oneleft.reputation.domain.port.out.PersonalData;
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
        EXPORT.put("ledger",
                "SELECT reason, points, source_id, plan_title, occurred_at FROM reputation_entry WHERE user_id = "
                + ":userId ORDER BY occurred_at");
        EXPORT.put("score",
                "SELECT score FROM reputation_score WHERE user_id = :userId");
        EXPORT.put("trackRecord",
                "SELECT plan_id, organizer FROM track_record WHERE user_id = :userId");
        EXPORT.put("badges",
                "SELECT badge FROM badge WHERE user_id = :userId");
        EXPORT.put("kudosGiven",
                "SELECT receiver_id, plan_id, kind, given_at FROM kudos WHERE giver_id = :userId");
        EXPORT.put("kudosReceived",
                "SELECT plan_id, kind, given_at FROM kudos WHERE receiver_id = :userId");
    }

    private static final List<String> ERASE = List.of(
            "SELECT set_config('oneleft.ledger_erasure', 'on', true)",
            "DELETE FROM reputation_entry WHERE user_id = :userId",
            "DELETE FROM reputation_score WHERE user_id = :userId",
            "DELETE FROM track_record WHERE user_id = :userId",
            "DELETE FROM badge WHERE user_id = :userId",
            "DELETE FROM kudos WHERE receiver_id = :userId",
            "UPDATE kudos SET giver_id = gen_random_uuid() WHERE giver_id = :userId");

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
