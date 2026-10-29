package es.upm.miw.oneleft.users.infrastructure.persistence;

import es.upm.miw.oneleft.users.domain.port.out.PersonalData;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Output adapter: the personal data of the users service in PostgreSQL (HU-045): the profile with its hobbies and
 * photo, and the consent. The photos in the storage are removed by the use case.
 */
@Repository
public class JdbcPersonalData implements PersonalData {

    private static final Map<String, String> EXPORT = new LinkedHashMap<>();

    static {
        EXPORT.put("profile",
                "SELECT display_name, bio, zone_name, zone_latitude, zone_longitude FROM profile WHERE user_id = :userId");
        EXPORT.put("hobbies", "SELECT activity, level FROM profile_hobby WHERE user_id = :userId");
        EXPORT.put("photo", "SELECT object_key, updated_at FROM profile_photo WHERE user_id = :userId");
        EXPORT.put("consent", "SELECT version, accepted_at FROM consent WHERE user_id = :userId");
    }

    private static final List<String> ERASE = List.of(
            "DELETE FROM profile_photo WHERE user_id = :userId",
            "DELETE FROM profile WHERE user_id = :userId",
            "DELETE FROM consent WHERE user_id = :userId");

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
        ERASE.forEach(sql -> jdbc.update(sql, params));
    }
}
