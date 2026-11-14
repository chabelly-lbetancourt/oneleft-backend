package es.upm.miw.oneleft.notifications.infrastructure.persistence;

import es.upm.miw.oneleft.notifications.domain.model.MemberAudience;
import es.upm.miw.oneleft.notifications.domain.port.out.Audiences;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Output adapter: the copy of gender and age, in PostgreSQL (HU-070). */
@Repository
class JdbcAudiences implements Audiences {

    private final NamedParameterJdbcTemplate jdbc;

    JdbcAudiences(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<UUID, MemberAudience> findByUsers(Collection<UUID> userIds) {
        var found = new HashMap<UUID, MemberAudience>();
        if (userIds.isEmpty()) {
            return found;
        }
        jdbc.query("SELECT user_id, gender, age FROM member_audience WHERE user_id IN (:userIds)",
                new MapSqlParameterSource("userIds", userIds), rs -> {
                    var userId = rs.getObject("user_id", UUID.class);
                    found.put(userId, new MemberAudience(userId, rs.getString("gender"), rs.getInt("age")));
                });
        return found;
    }

    @Override
    public void save(MemberAudience audience) {
        jdbc.update("""
                INSERT INTO member_audience (user_id, gender, age, updated_at)
                VALUES (:userId, :gender, :age, :now)
                ON CONFLICT (user_id) DO UPDATE SET gender = :gender, age = :age, updated_at = :now""",
                new MapSqlParameterSource("userId", audience.userId()).addValue("gender", audience.gender())
                        .addValue("age", audience.age()).addValue("now", Timestamp.from(Instant.now())));
    }
}
