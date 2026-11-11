package es.upm.miw.oneleft.users.infrastructure.persistence;

import es.upm.miw.oneleft.users.domain.model.Demographics;
import es.upm.miw.oneleft.users.domain.model.Gender;
import es.upm.miw.oneleft.users.domain.port.out.DemographicsRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Output adapter: date of birth and gender in PostgreSQL (HU-070). */
@Repository
class JdbcDemographics implements DemographicsRepository {

    private final NamedParameterJdbcTemplate jdbc;

    JdbcDemographics(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Demographics> findByUser(UUID userId) {
        return jdbc.query("SELECT * FROM demographics WHERE user_id = :userId",
                new MapSqlParameterSource("userId", userId), (rs, row) -> new Demographics(
                        rs.getObject("user_id", UUID.class), rs.getObject("birth_date", LocalDate.class),
                        Gender.valueOf(rs.getString("gender")), rs.getInt("changes"))).stream().findFirst();
    }

    @Override
    public Demographics save(Demographics demographics) {
        jdbc.update("""
                INSERT INTO demographics (user_id, birth_date, gender, changes, updated_at)
                VALUES (:userId, :birthDate, :gender, :changes, :now)
                ON CONFLICT (user_id) DO UPDATE SET birth_date = :birthDate, gender = :gender, changes = :changes,
                    updated_at = :now""",
                new MapSqlParameterSource("userId", demographics.userId())
                        .addValue("birthDate", demographics.birthDate()).addValue("gender", demographics.gender().name())
                        .addValue("changes", demographics.changes()).addValue("now", Timestamp.from(Instant.now())));
        return demographics;
    }
}
