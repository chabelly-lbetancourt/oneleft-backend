package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.Arrival;
import es.upm.miw.oneleft.plans.domain.model.ArrivalStatus;
import es.upm.miw.oneleft.plans.domain.model.Level;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.Minimum;
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
            @Schema(example = "INTERMEDIATE") Level level,
            @Min(1) @Max(Plan.MAX_SPOTS)
            @Schema(description = "Optional minimum of participants (HU-039), at most the spots", example = "2")
            Integer minParticipants,
            @Schema(description = "Deadline of the minimum: at least 5 minutes from now and not after the start",
                    example = "2026-09-28T16:30:00Z") Instant minimumDeadline) {
    }

    @Schema(description = "Minimum of participants (HU-039): if it is not reached by the deadline, the plan is "
            + "cancelled")
    public record MinimumDto(@Schema(example = "2") int participants, Instant deadline,
                             @Schema(description = "Reached at the deadline: the plan goes ahead") boolean confirmed) {

        static MinimumDto of(Minimum minimum) {
            return minimum == null ? null
                    : new MinimumDto(minimum.participants(), minimum.deadline(), !minimum.pending());
        }
    }

    @Schema(description = "Person who has taken a spot")
    public record ParticipantDto(UUID userId, @Schema(example = "Lucía") String name, Instant joinedAt) {

        static ParticipantDto of(Participant participant) {
            return new ParticipantDto(participant.userId(), participant.name(), participant.joinedAt());
        }
    }

    @Schema(description = "«On my way» or «running late» of someone of the group (HU-040)")
    public record ArrivalDto(UUID userId, @Schema(example = "Lucía") String name, ArrivalStatus status,
                             @Schema(example = "10") Integer minutesLate, Instant at) {

        static ArrivalDto of(Arrival arrival) {
            return new ArrivalDto(arrival.userId(), arrival.name(), arrival.status(), arrival.minutesLate(),
                    arrival.at());
        }
    }

    @Schema(description = "My status of arrival (HU-040)")
    public record ArrivalRequest(@jakarta.validation.constraints.NotNull ArrivalStatus status,
                                 @Schema(description = "5, 10, 15 or 30, only when running late", example = "10")
                                 Integer minutesLate) {
    }

    @Schema(description = "Published plan")
    public record PlanResponse(UUID id, UUID organizerId, @Schema(example = "Ana") String organizerName,
                               Activity activity, String title, String description, MeetingPointDto meetingPoint,
                               Instant startsAt, int spots, int occupied, int freeSpots, Level level,
                               PlanStatus status, Instant publishedAt, List<ParticipantDto> participants,
                               @Schema(description = "People waiting for a spot, first to last (HU-023)")
                               List<ParticipantDto> waitlist,
                               @Schema(description = "Null when the plan goes ahead with anyone") MinimumDto minimum,
                               @Schema(description = "Only for the group of the plan, until it starts (HU-040)")
                               List<ArrivalDto> arrivals) {

        /** The plan without the statuses of arrival: for anyone, such as in the nearby search. */
        static PlanResponse of(Plan plan) {
            return of(plan, null);
        }

        /** The plan as {@code viewer} sees it: the statuses of arrival only if they are in its group (HU-040). */
        static PlanResponse of(Plan plan, UUID viewer) {
            var arrivals = viewer != null && plan.isMember(viewer)
                    ? plan.arrivals().stream().map(ArrivalDto::of).toList() : List.<ArrivalDto>of();
            return new PlanResponse(plan.id(), plan.organizer().id(), plan.organizer().name(), plan.activity(),
                    plan.title(), plan.description(), MeetingPointDto.of(plan.meetingPoint()), plan.startsAt(),
                    plan.spots(), plan.occupied(), plan.freeSpots(), plan.level(), plan.status(), plan.publishedAt(),
                    plan.participants().stream().map(ParticipantDto::of).toList(),
                    plan.waitlist().stream().map(ParticipantDto::of).toList(), MinimumDto.of(plan.minimum()),
                    arrivals);
        }
    }

    /**
     * Public view of a plan (HU-024), for anyone with the link: no people (neither organizer nor participants) and the
     * meeting point rounded to about 100 m, like in the nearby search.
     */
    @Schema(description = "Plan as seen without a session, from a shared link")
    public record PublicPlanResponse(UUID id, Activity activity, String title, String description,
                                     MeetingPointDto meetingPoint, Instant startsAt, int spots, int occupied,
                                     int freeSpots, Level level, PlanStatus status, MinimumDto minimum) {

        /** 3 decimals: about 100 m */
        private static final double PRECISION = 1000;

        static PublicPlanResponse of(Plan plan) {
            var point = plan.meetingPoint();
            return new PublicPlanResponse(plan.id(), plan.activity(), plan.title(), plan.description(),
                    new MeetingPointDto(point.name(), Math.round(point.latitude() * PRECISION) / PRECISION,
                            Math.round(point.longitude() * PRECISION) / PRECISION),
                    plan.startsAt(), plan.spots(), plan.occupied(), plan.freeSpots(), plan.level(), plan.status(),
                    MinimumDto.of(plan.minimum()));
        }
    }

    @Schema(description = "Nearby plan with the distance from the search position")
    public record NearbyPlanResponse(PlanResponse plan, @Schema(example = "850") long distanceMeters) {

        static NearbyPlanResponse of(NearbyPlan nearby) {
            return new NearbyPlanResponse(PlanResponse.of(nearby.plan()), Math.round(nearby.distanceMeters()));
        }
    }
}
