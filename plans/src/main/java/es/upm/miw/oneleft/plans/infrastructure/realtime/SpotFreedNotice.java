package es.upm.miw.oneleft.plans.infrastructure.realtime;

import es.upm.miw.oneleft.plans.domain.model.PlanLeftEvent;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Notice for the first person of the waiting list: a spot was freed and it is now theirs. */
@Schema(description = "You have a spot: you were first on the waiting list (HU-023)")
public record SpotFreedNotice(UUID planId, String title) {

    static SpotFreedNotice of(PlanLeftEvent event) {
        return new SpotFreedNotice(event.planId(), event.title());
    }
}
