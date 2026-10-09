package es.upm.miw.oneleft.plans.infrastructure.realtime;

import es.upm.miw.oneleft.plans.domain.model.ArrivalStatus;
import es.upm.miw.oneleft.plans.domain.model.PlanArrival;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Notice for the group of a plan: someone is on the way or running late (HU-040). */
@Schema(description = "Someone of the group is on the way or running late")
public record PlanArrivalNotice(UUID planId, String title, @Schema(example = "Lucía") String name,
                                ArrivalStatus status, @Schema(example = "10") Integer minutesLate) {

    static PlanArrivalNotice of(PlanArrival event) {
        return new PlanArrivalNotice(event.planId(), event.title(), event.name(), event.status(),
                event.minutesLate());
    }
}
