package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.Level;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.NearbyPlan;
import es.upm.miw.oneleft.plans.domain.model.Participant;
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
import java.util.List;
import java.util.UUID;

public final class PlanDtos {

    private PlanDtos() {
    }

    @Schema(description = "Meeting point")
    public record MeetingPointDto(
            @NotBlank @Size(max = MeetingPoint.MAX_NAME_LENGTH) @Schema(example = "Sports centre courts") String name,
            @NotNull @DecimalMin("-90") @DecimalMax("90") @Schema(example = "40.3912") Double latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") @Schema(example = "-3.6287") Double longitude) {

        static MeetingPointDto of(MeetingPoint point) {
            return new MeetingPointDto(point.name(), point.latitude(), point.longitude());
        }

        MeetingPoint toDomain() {
            return new MeetingPoint(name, latitude, longitude);
        }
    }

    @Schema(description = "Plan to publish")
    public record PublishPlanRequest(
            @NotNull @Schema(example = "PADEL") Activity activity,
            @NotBlank @Size(min = Plan.MIN_TITLE_LENGTH, max = Plan.MAX_TITLE_LENGTH)
            @Schema(example = "Padel match, one player missing") String title,
            @Size(max = Plan.MAX_DESCRIPTION_LENGTH) @Schema(example = "Intermediate level, indoor court") String description,
            @NotNull @Valid MeetingPointDto meetingPoint,
            @NotNull @Schema(description = "Start time: between 5 minutes and 12 hours from now",
                    example = "2026-09-28T17:00:00Z") Instant startsAt,
            @Min(1) @Max(Plan.MAX_SPOTS) @Schema(description = "Free spots to fill", example = "1") int spots,
            @Schema(example = "INTERMEDIATE") Level level) {
    }

    @Schema(description = "Person who has taken a spot")
    public record ParticipantDto(UUID userId, @Schema(example = "Lucía") String name, Instant joinedAt) {

        static ParticipantDto of(Participant participant) {
            return new ParticipantDto(participant.userId(), participant.name(), participant.joinedAt());
        }
    }

    @Schema(description = "Published plan")
    public record PlanResponse(UUID id, UUID organizerId, @Schema(example = "Ana") String organizerName,
                               Activity activity, String title, String description, MeetingPointDto meetingPoint,
                               Instant startsAt, int spots, int occupied, int freeSpots, Level level,
                               PlanStatus status, Instant publishedAt, List<ParticipantDto> participants,
                               @Schema(description = "People waiting for a spot, first to last (HU-023)")
                               List<ParticipantDto> waitlist) {

        static PlanResponse of(Plan plan) {
            return new PlanResponse(plan.id(), plan.organizer().id(), plan.organizer().name(), plan.activity(),
                    plan.title(), plan.description(), MeetingPointDto.of(plan.meetingPoint()), plan.startsAt(),
                    plan.spots(), plan.occupied(), plan.freeSpots(), plan.level(), plan.status(), plan.publishedAt(),
                    plan.participants().stream().map(ParticipantDto::of).toList(),
                    plan.waitlist().stream().map(ParticipantDto::of).toList());
        }
    }

    @Schema(description = "Nearby plan with the distance from the search position")
    public record NearbyPlanResponse(PlanResponse plan, @Schema(example = "850") long distanceMeters) {

        static NearbyPlanResponse of(NearbyPlan nearby) {
            return new NearbyPlanResponse(PlanResponse.of(nearby.plan()), Math.round(nearby.distanceMeters()));
        }
    }
}
