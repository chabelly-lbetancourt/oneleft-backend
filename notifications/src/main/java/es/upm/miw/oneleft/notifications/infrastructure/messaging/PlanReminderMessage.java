package es.upm.miw.oneleft.notifications.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import es.upm.miw.oneleft.notifications.domain.model.PlanReminder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** The {@code plan.reminder} event as the plans service sends it (HU-007). */
@JsonIgnoreProperties(ignoreUnknown = true)
record PlanReminderMessage(UUID planId, String title, String placeName, Instant startsAt, List<UUID> recipientIds) {

    PlanReminder toDomain() {
        return new PlanReminder(planId, title, placeName, startsAt, recipientIds);
    }
}
