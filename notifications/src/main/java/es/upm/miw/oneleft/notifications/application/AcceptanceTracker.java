package es.upm.miw.oneleft.notifications.application;

import es.upm.miw.oneleft.notifications.domain.port.in.TrackAcceptanceUseCase;
import es.upm.miw.oneleft.notifications.domain.port.out.NoticeBatches;
import es.upm.miw.oneleft.notifications.domain.port.out.NoticeLog;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Learns what the notices led to (HU-012): the people who took a spot after a notice are what the acceptance model is
 * measured and retrained with, and a full or cancelled plan needs no more batches.
 */
@Service
public class AcceptanceTracker implements TrackAcceptanceUseCase {

    private final NoticeLog notices;
    private final NoticeBatches batches;

    public AcceptanceTracker(NoticeLog notices, NoticeBatches batches) {
        this.notices = notices;
        this.batches = batches;
    }

    @Override
    public void joined(UUID planId, UUID userId, boolean full, Instant at) {
        notices.accepted(userId, planId, at);
        if (full) {
            batches.close(planId);
        }
    }

    @Override
    public void closed(UUID planId) {
        batches.close(planId);
    }
}
