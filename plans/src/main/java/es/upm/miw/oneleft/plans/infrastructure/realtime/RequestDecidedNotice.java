package es.upm.miw.oneleft.plans.infrastructure.realtime;

import es.upm.miw.oneleft.plans.domain.model.RequestDecided;
import es.upm.miw.oneleft.plans.domain.model.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Notice for who asked for a spot: the organizer accepted or rejected them, or the plan filled up (HU-069). */
@Schema(description = "The decision on your request for a spot")
public record RequestDecidedNotice(UUID planId, String title, RequestStatus decision,
                                   @Schema(description = "The plan filled up: the rejection was not personal")
                                   boolean planFull) {

    static RequestDecidedNotice of(RequestDecided event) {
        return new RequestDecidedNotice(event.planId(), event.title(), event.decision(), event.planFull());
    }
}
