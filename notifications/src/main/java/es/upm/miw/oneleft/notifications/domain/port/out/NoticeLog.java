package es.upm.miw.oneleft.notifications.domain.port.out;

import java.time.Instant;
import java.util.UUID;

/**
 * Notices already sent: nobody hears twice about the same plan, and the daily limit can be checked.
 */
public interface NoticeLog {

    /** Records the notice unless it was already sent. @return {@code false} if the person had already been told */
    boolean record(UUID userId, UUID planId, Instant at);

    long countSince(UUID userId, Instant since);
}
