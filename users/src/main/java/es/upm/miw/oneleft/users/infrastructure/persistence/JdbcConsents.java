package es.upm.miw.oneleft.users.infrastructure.persistence;

import es.upm.miw.oneleft.users.domain.model.AcceptedConsent;
import es.upm.miw.oneleft.users.domain.port.out.Consents;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Output adapter: the consent of each person in PostgreSQL (HU-045). */
@Repository
public class JdbcConsents implements Consents {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcConsents(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<AcceptedConsent> find(UUID userId) {
        return jdbc.query("SELECT version, accepted_at FROM consent WHERE user_id = :userId",
                new MapSqlParameterSource("userId", userId),
                (row, number) -> new AcceptedConsent(row.getString("version"),
                        row.getTimestamp("accepted_at").toInstant())).stream().findFirst();
    }

    @Override
    public void save(UUID userId, String version, Instant acceptedAt) {
        jdbc.update("INSERT INTO consent (user_id, version, accepted_at) VALUES (:userId, :version, :at) "
                        + "ON CONFLICT (user_id) DO UPDATE SET version = :version, accepted_at = :at",
                new MapSqlParameterSource("userId", userId).addValue("version", version)
                        .addValue("at", Timestamp.from(acceptedAt)));
    }
}
