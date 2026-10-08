package es.upm.miw.oneleft.notifications.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import es.upm.miw.oneleft.notifications.domain.model.PlanCancellation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** The {@code plan.cancelled} event as the plans service sends it (HU-039). */
@JsonIgnoreProperties(ignoreUnknown = true)
record PlanCancelledMessage(UUID planId, String title, String placeName, Instant startsAt, List<UUID> recipientIds) {

    PlanCancellation toDomain() {
        return new PlanCancellation(planId, title, placeName, startsAt, recipientIds);
    }
}
