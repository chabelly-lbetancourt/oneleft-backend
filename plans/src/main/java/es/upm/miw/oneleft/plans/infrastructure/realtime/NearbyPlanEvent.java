package es.upm.miw.oneleft.plans.infrastructure.realtime;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.NearbySearch;
import es.upm.miw.oneleft.plans.domain.model.PlanPublished;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Notice of a new plan nearby. It carries just enough to notify; the client reloads the list or opens the detail.
 */
@Schema(description = "Newly published plan that matches the search")
public record NearbyPlanEvent(UUID planId, Activity activity, Instant startsAt, int freeSpots,
                              @Schema(example = "850") long distanceMeters) {

    static NearbyPlanEvent of(PlanPublished event, NearbySearch search) {
        return new NearbyPlanEvent(event.planId(), event.activity(), event.startsAt(), event.freeSpots(),
                Math.round(search.distanceTo(event.latitude(), event.longitude())));
    }
}
