package es.upm.miw.oneleft.social.domain.port.in;

import es.upm.miw.oneleft.social.domain.model.ReportReason;
import es.upm.miw.oneleft.social.domain.model.ReportTarget;

import java.util.UUID;

/**
 * A report as it comes from the app (HU-033): a person ({@code targetId} is their id) or a plan ({@code targetId} is
 * the plan, {@code organizerId} who organizes it).
 */
public record ReportCommand(ReportTarget target, UUID targetId, UUID organizerId, ReportReason reason,
                            String comment) {
}
