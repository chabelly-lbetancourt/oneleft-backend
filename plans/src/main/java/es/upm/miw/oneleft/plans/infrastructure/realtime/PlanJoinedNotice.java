package es.upm.miw.oneleft.plans.infrastructure.realtime;

import es.upm.miw.oneleft.plans.domain.model.PlanJoined;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Notice for the organizer: someone has joined one of their plans. */
@Schema(description = "Someone has joined one of your plans")
public record PlanJoinedNotice(UUID planId, String title, @Schema(example = "Lucía") String participantName,
                               int freeSpots, boolean full) {

    static PlanJoinedNotice of(PlanJoined event) {
        return new PlanJoinedNotice(event.planId(), event.title(), event.participantName(), event.freeSpots(),
                event.full());
    }
}
