package es.upm.miw.oneleft.notifications.domain.port.out;

import es.upm.miw.oneleft.notifications.domain.model.NoticeHistory;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Notices already sent: nobody hears twice about the same plan, the daily limit can be checked, and what each notice
 * led to is known (HU-012).
 */
public interface NoticeLog {

    /**
     * Records the notice unless it was already sent.
     *
     * @param ranking how the person was chosen: {@code model}, {@code distance}, {@code fallback} (the model was not
     *                available) or {@code alert}
     * @param batch   number of the batch that carried it, 0 for the first
     * @return {@code false} if the person had already been told
     */
    boolean record(UUID userId, UUID planId, Instant at, String ranking, int batch);

    default boolean record(UUID userId, UUID planId, Instant at) {
        return record(userId, planId, at, "alert", 0);
    }

    long countSince(UUID userId, Instant since);

    /** The notice led to a spot: marks it, if the person had been notified of that plan. */
    void accepted(UUID userId, UUID planId, Instant at);

    /** What each person did with their notices since a date, and how many they got since the start of the day. */
    Map<UUID, NoticeHistory> historyOf(Collection<UUID> userIds, Instant since, Instant startOfDay);
}
