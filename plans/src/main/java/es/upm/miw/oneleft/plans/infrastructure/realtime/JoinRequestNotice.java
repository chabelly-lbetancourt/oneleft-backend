package es.upm.miw.oneleft.plans.infrastructure.realtime;

import es.upm.miw.oneleft.plans.domain.model.JoinRequested;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Notice for the organizer: someone asked for a spot in one of their plans (HU-069). */
@Schema(description = "Someone asked for a spot in one of your plans")
public record JoinRequestNotice(UUID planId, String title, UUID requesterId,
                                @Schema(example = "Lucía") String requesterName) {

    static JoinRequestNotice of(JoinRequested event) {
        return new JoinRequestNotice(event.planId(), event.title(), event.requesterId(), event.requesterName());
    }
}
