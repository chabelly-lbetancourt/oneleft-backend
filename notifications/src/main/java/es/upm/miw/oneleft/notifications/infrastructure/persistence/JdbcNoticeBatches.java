package es.upm.miw.oneleft.notifications.infrastructure.persistence;

import es.upm.miw.oneleft.notifications.domain.model.Activity;
import es.upm.miw.oneleft.notifications.domain.model.BatchedPlan;
import es.upm.miw.oneleft.notifications.domain.model.Level;
import es.upm.miw.oneleft.notifications.domain.model.PublishedPlan;
import es.upm.miw.oneleft.notifications.domain.model.RankedCandidate;
import es.upm.miw.oneleft.notifications.domain.port.out.NoticeBatches;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Output adapter: the people still to be notified of each plan, in PostgreSQL (HU-012). The plan is kept whole so that
 * a later batch does not depend on anything else.
 */
@Repository
@Transactional
class JdbcNoticeBatches implements NoticeBatches {

    private static final int MAX_DUE = 100;

    private final NamedParameterJdbcTemplate jdbc;

    JdbcNoticeBatches(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void schedule(PublishedPlan plan, String ranking, List<RankedCandidate> rest, Instant nextBatchAt) {
        close(plan.planId());
        jdbc.update("""
                INSERT INTO notice_batch_plan (plan_id, organizer_id, activity, title, place_name, latitude, longitude,
                    starts_at, free_spots, level, ranking, next_batch, next_batch_at)
                VALUES (:planId, :organizerId, :activity, :title, :placeName, :latitude, :longitude, :startsAt,
                    :freeSpots, :level, :ranking, 1, :nextBatchAt)""",
                new MapSqlParameterSource("planId", plan.planId()).addValue("organizerId", plan.organizerId())
                        .addValue("activity", plan.activity().name()).addValue("title", plan.title())
                        .addValue("placeName", plan.placeName()).addValue("latitude", plan.latitude())
                        .addValue("longitude", plan.longitude()).addValue("startsAt", Timestamp.from(plan.startsAt()))
                        .addValue("freeSpots", plan.freeSpots())
                        .addValue("level", plan.level() == null ? null : plan.level().name())
                        .addValue("ranking", ranking).addValue("nextBatchAt", Timestamp.from(nextBatchAt)));
        SqlParameterSource[] people = IntStream.range(0, rest.size())
                .mapToObj(i -> new MapSqlParameterSource("planId", plan.planId())
                        .addValue("userId", rest.get(i).candidate().userId()).addValue("position", i)
                        .addValue("score", rest.get(i).score()))
                .toArray(SqlParameterSource[]::new);
        jdbc.batchUpdate("INSERT INTO pending_notice (plan_id, user_id, position, score) "
                + "VALUES (:planId, :userId, :position, :score)", people);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BatchedPlan> due(Instant now) {
        return jdbc.query("SELECT * FROM notice_batch_plan WHERE next_batch_at <= :now ORDER BY next_batch_at LIMIT "
                        + MAX_DUE, new MapSqlParameterSource("now", Timestamp.from(now)), (rs, row) -> {
            var level = rs.getString("level");
            var plan = new PublishedPlan(rs.getObject("plan_id", UUID.class), rs.getObject("organizer_id", UUID.class),
                    Activity.valueOf(rs.getString("activity")), rs.getString("title"), rs.getString("place_name"),
                    rs.getDouble("latitude"), rs.getDouble("longitude"),
                    rs.getTimestamp("starts_at").toInstant(), rs.getInt("free_spots"),
                    level == null ? null : Level.valueOf(level));
            return new BatchedPlan(plan, rs.getString("ranking"), rs.getInt("next_batch"));
        });
    }

    @Override
    public List<UUID> takeNext(UUID planId, int limit) {
        var params = new MapSqlParameterSource("planId", planId).addValue("limit", limit);
        // SKIP LOCKED: two replicas never take the same people
        var people = jdbc.queryForList("SELECT user_id FROM pending_notice WHERE plan_id = :planId "
                + "ORDER BY position LIMIT :limit FOR UPDATE SKIP LOCKED", params, UUID.class);
        if (!people.isEmpty()) {
            jdbc.update("DELETE FROM pending_notice WHERE plan_id = :planId AND user_id IN (:people)",
                    new MapSqlParameterSource("planId", planId).addValue("people", people));
        }
        return people;
    }

    @Override
    public void rescheduleOrFinish(UUID planId, Instant nextBatchAt) {
        var params = new MapSqlParameterSource("planId", planId).addValue("nextBatchAt", Timestamp.from(nextBatchAt));
        var left = jdbc.queryForObject("SELECT count(*) FROM pending_notice WHERE plan_id = :planId", params,
                Long.class);
        if (left != null && left > 0) {
            jdbc.update("UPDATE notice_batch_plan SET next_batch = next_batch + 1, next_batch_at = :nextBatchAt "
                    + "WHERE plan_id = :planId", params);
        } else {
            close(planId);
        }
    }

    @Override
    public void close(UUID planId) {
        // The pending people go with it (on delete cascade)
        jdbc.update("DELETE FROM notice_batch_plan WHERE plan_id = :planId", new MapSqlParameterSource("planId", planId));
    }
}
