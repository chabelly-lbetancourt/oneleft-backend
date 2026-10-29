package es.upm.miw.oneleft.notifications.domain.port.in;

import java.time.Instant;
import java.util.UUID;

/**
 * Learns what the notices led to (HU-012): who took a spot, and when a plan needs no more notices.
 */
public interface TrackAcceptanceUseCase {

    /** Someone took a spot: if they had been notified, the notice worked; and a full plan needs no more notices. */
    void joined(UUID planId, UUID userId, boolean full, Instant at);

    /** The plan was cancelled: nobody else is notified. */
    void closed(UUID planId);
}
