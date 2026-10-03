package es.upm.miw.oneleft.plans.infrastructure.realtime;

import es.upm.miw.oneleft.plans.domain.model.PlanReminder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/** Notice for everyone in a plan: it is about to start (HU-007). */
@Schema(description = "A plan you are in is about to start")
public record PlanReminderNotice(UUID planId, String title, @Schema(example = "Pistas de la Albufera") String placeName,
                                 Instant startsAt) {

    static PlanReminderNotice of(PlanReminder event) {
        return new PlanReminderNotice(event.planId(), event.title(), event.placeName(), event.startsAt());
    }
}
