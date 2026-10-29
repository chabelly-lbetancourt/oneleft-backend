package es.upm.miw.oneleft.chat.infrastructure.persistence;

import es.upm.miw.oneleft.chat.domain.port.out.PersonalData;
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
        EXPORT.put("messages",
                "SELECT plan_id, text, sent_at FROM chat_message WHERE author_id = :userId ORDER BY sent_at");
    }

    private static final List<String> ERASE = List.of(
            "UPDATE chat_message SET author_id = gen_random_uuid(), author_name = 'Persona eliminada' WHERE "
                + "author_id = :userId");

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
