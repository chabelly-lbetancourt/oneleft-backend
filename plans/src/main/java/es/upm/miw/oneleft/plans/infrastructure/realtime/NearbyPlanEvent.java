package es.upm.miw.oneleft.plans.infrastructure.realtime;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.NearbySearch;
import es.upm.miw.oneleft.plans.domain.model.PlanPublished;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Aviso de un plan nuevo cerca. Lleva lo justo para avisar; el cliente recarga la lista o abre el detalle.
 */
@Schema(description = "Plan recién publicado que encaja con la búsqueda")
public record NearbyPlanEvent(UUID planId, Activity activity, Instant startsAt, int freeSpots,
                              @Schema(example = "850") long distanceMeters) {

    static NearbyPlanEvent of(PlanPublished event, NearbySearch search) {
        return new NearbyPlanEvent(event.planId(), event.activity(), event.startsAt(), event.freeSpots(),
                Math.round(search.distanceTo(event.latitude(), event.longitude())));
    }
}
