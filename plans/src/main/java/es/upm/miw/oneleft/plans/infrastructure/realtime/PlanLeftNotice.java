package es.upm.miw.oneleft.plans.infrastructure.realtime;

import es.upm.miw.oneleft.plans.domain.model.PlanLeftEvent;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Notice for the organizer: someone has left one of their plans and, maybe, someone from the waiting list came in. */
@Schema(description = "Someone has left one of your plans (HU-023)")
public record PlanLeftNotice(UUID planId, String title, @Schema(example = "Diego") String participantName,
                             @Schema(example = "Lucía", nullable = true) String promotedName, int freeSpots,
                             boolean full) {

    static PlanLeftNotice of(PlanLeftEvent event) {
        return new PlanLeftNotice(event.planId(), event.title(), event.participantName(), event.promotedName(),
                event.freeSpots(), event.full());
    }
}
