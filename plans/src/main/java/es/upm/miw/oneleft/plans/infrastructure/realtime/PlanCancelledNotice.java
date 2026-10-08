package es.upm.miw.oneleft.plans.infrastructure.realtime;

import es.upm.miw.oneleft.plans.domain.model.PlanCancelled;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/** Notice for everyone in a plan: it has been cancelled (HU-039). */
@Schema(description = "A plan you are in has been cancelled")
public record PlanCancelledNotice(UUID planId, String title,
                                  @Schema(example = "Pistas de la Albufera") String placeName, Instant startsAt,
                                  @Schema(example = "MINIMUM_NOT_REACHED") PlanCancelled.Reason reason) {

    static PlanCancelledNotice of(PlanCancelled event) {
        return new PlanCancelledNotice(event.planId(), event.title(), event.placeName(), event.startsAt(),
                event.reason());
    }
}
