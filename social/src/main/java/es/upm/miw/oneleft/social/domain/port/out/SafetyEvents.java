package es.upm.miw.oneleft.social.domain.port.out;

import es.upm.miw.oneleft.social.domain.model.Report;

import java.time.Instant;
import java.util.UUID;

/**
 * What the other services learn of blocks and reports (HU-033): the plans and notifications services keep their own
 * copy of the blocks, and the moderation (HU-011) receives the reports. Never announced to the blocked person.
 */
public interface SafetyEvents {

    void blocked(UUID blockerId, UUID blockedId, Instant at);

    void unblocked(UUID blockerId, UUID blockedId, Instant at);

    void reported(Report report);
}
