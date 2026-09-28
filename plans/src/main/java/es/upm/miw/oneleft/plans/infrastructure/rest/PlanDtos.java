package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.Level;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class PlanDtos {

    private PlanDtos() {
    }

    @Schema(description = "Lugar de encuentro")
    public record MeetingPointDto(
            @NotBlank @Size(max = MeetingPoint.MAX_NAME_LENGTH) @Schema(example = "Pistas del polideportivo") String name,
            @NotNull @DecimalMin("-90") @DecimalMax("90") @Schema(example = "40.3912") Double latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") @Schema(example = "-3.6287") Double longitude) {

        static MeetingPointDto of(MeetingPoint point) {
            return new MeetingPointDto(point.name(), point.latitude(), point.longitude());
        }

        MeetingPoint toDomain() {
            return new MeetingPoint(name, latitude, longitude);
        }
    }

    @Schema(description = "Plan que se quiere publicar")
    public record PublishPlanRequest(
            @NotNull @Schema(example = "PADEL") Activity activity,
            @NotBlank @Size(min = Plan.MIN_TITLE_LENGTH, max = Plan.MAX_TITLE_LENGTH)
            @Schema(example = "Partido de pádel, falta uno") String title,
            @Size(max = Plan.MAX_DESCRIPTION_LENGTH) @Schema(example = "Nivel medio, pista cubierta") String description,
            @NotNull @Valid MeetingPointDto meetingPoint,
            @NotNull @Schema(description = "Hora de inicio: entre 5 minutos y 12 horas desde ahora",
                    example = "2026-09-28T17:00:00Z") Instant startsAt,
            @Min(1) @Max(Plan.MAX_SPOTS) @Schema(description = "Plazas libres a cubrir", example = "1") int spots,
            @Schema(example = "INTERMEDIO") Level level) {
    }

    @Schema(description = "Plan publicado")
    public record PlanResponse(UUID id, UUID organizerId, @Schema(example = "Ana") String organizerName,
                               Activity activity, String title, String description, MeetingPointDto meetingPoint,
                               Instant startsAt, int spots, int occupied, int freeSpots, Level level,
                               PlanStatus status, Instant publishedAt) {

        static PlanResponse of(Plan plan) {
            return new PlanResponse(plan.id(), plan.organizer().id(), plan.organizer().name(), plan.activity(),
                    plan.title(), plan.description(), MeetingPointDto.of(plan.meetingPoint()), plan.startsAt(),
                    plan.spots(), plan.occupied(), plan.freeSpots(), plan.level(), plan.status(), plan.publishedAt());
        }
    }
}
