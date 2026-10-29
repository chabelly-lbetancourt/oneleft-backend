package es.upm.miw.oneleft.notifications.infrastructure.persistence;

import es.upm.miw.oneleft.notifications.domain.model.NoticeHistory;
import es.upm.miw.oneleft.notifications.domain.port.out.NoticeLog;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Repository
class JpaNoticeLog implements NoticeLog {

    private static final String HISTORY = """
            SELECT user_id,
                   count(*) AS sent,
                   count(accepted_at) AS accepted,
                   count(*) FILTER (WHERE sent_at >= :startOfDay) AS today
            FROM sent_notice
            WHERE user_id IN (:userIds) AND sent_at >= :since
            GROUP BY user_id""";

    private final SpringDataSentNoticeRepository repository;
    private final NamedParameterJdbcTemplate jdbc;

    JpaNoticeLog(SpringDataSentNoticeRepository repository, NamedParameterJdbcTemplate jdbc) {
        this.repository = repository;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public boolean record(UUID userId, UUID planId, Instant at, String ranking, int batch) {
        // Each plan is handled by a single replica (shared queue), so checking first is enough
        if (repository.existsById(new SentNoticeEntity.Key(userId, planId))) {
            return false;
        }
        repository.save(new SentNoticeEntity(userId, planId, at, ranking, batch));
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public long countSince(UUID userId, Instant since) {
        return repository.countSince(userId, since);
    }

    @Override
    @Transactional
    public void accepted(UUID userId, UUID planId, Instant at) {
        jdbc.update("UPDATE sent_notice SET accepted_at = :at WHERE user_id = :userId AND plan_id = :planId "
                        + "AND accepted_at IS NULL",
                new MapSqlParameterSource("at", Timestamp.from(at)).addValue("userId", userId)
                        .addValue("planId", planId));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, NoticeHistory> historyOf(Collection<UUID> userIds, Instant since, Instant startOfDay) {
        var history = new HashMap<UUID, NoticeHistory>();
        if (userIds.isEmpty()) {
            return history;
        }
        var params = new MapSqlParameterSource("userIds", userIds)
                .addValue("since", Timestamp.from(since)).addValue("startOfDay", Timestamp.from(startOfDay));
        jdbc.query(HISTORY, params, rs -> {
            history.put(rs.getObject("user_id", UUID.class),
                    new NoticeHistory(rs.getLong("sent"), rs.getLong("accepted"), rs.getLong("today")));
        });
        return history;
    }
}
