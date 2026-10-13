package es.upm.miw.oneleft.plans.infrastructure.realtime;

import es.upm.miw.oneleft.plans.domain.model.PlanCheckedIn;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Notice for the group of a plan: someone arrived at the meeting point (HU-025). */
@Schema(description = "Someone of the group arrived at the meeting point")
public record PlanCheckInNotice(UUID planId, String title, @Schema(example = "Lucía") String name) {

    static PlanCheckInNotice of(PlanCheckedIn event) {
        return new PlanCheckInNotice(event.planId(), event.title(), event.name());
    }
}
