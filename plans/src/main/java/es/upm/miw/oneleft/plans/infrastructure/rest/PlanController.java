package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.NearbySearch;
import es.upm.miw.oneleft.plans.domain.model.Organizer;
import es.upm.miw.oneleft.plans.domain.port.in.PublishPlanCommand;
import es.upm.miw.oneleft.plans.domain.port.in.PublishPlanUseCase;
import es.upm.miw.oneleft.plans.domain.port.in.QueryPlansUseCase;
import es.upm.miw.oneleft.plans.infrastructure.realtime.NearbyPlanEvent;
import es.upm.miw.oneleft.plans.infrastructure.realtime.NearbyPlanSubscriptions;
import es.upm.miw.oneleft.plans.infrastructure.rest.PlanDtos.NearbyPlanResponse;
import es.upm.miw.oneleft.plans.infrastructure.rest.PlanDtos.PlanResponse;
import es.upm.miw.oneleft.plans.infrastructure.rest.PlanDtos.PublishPlanRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Duration;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(PlanController.PLANS)
@Tag(name = "Plans", description = "Plans for the next few hours with free spots")
public class PlanController {

    public static final String PLANS = "/api/v1/plans";
    public static final String PLAN = "/{planId}";
    public static final String MINE = "/mine";
    public static final String NEARBY = "/nearby";
    public static final String NEARBY_STREAM = NEARBY + "/stream";

    private final PublishPlanUseCase publishPlan;
    private final QueryPlansUseCase queryPlans;
    private final NearbyPlanSubscriptions nearbySubscriptions;

    public PlanController(PublishPlanUseCase publishPlan, QueryPlansUseCase queryPlans,
                          NearbyPlanSubscriptions nearbySubscriptions) {
        this.publishPlan = publishPlan;
        this.queryPlans = queryPlans;
        this.nearbySubscriptions = nearbySubscriptions;
    }

    @PostMapping
    @Operation(summary = "Publish a plan (HU-003)",
            description = "Publishes a plan that starts between 5 minutes and 12 hours from now, with the free spots "
                    + "still to fill. Emits the PlanPublished event.")
    @ApiResponse(responseCode = "201", description = "Plan published")
    @ApiResponse(responseCode = "400", description = "Invalid data", content = @Content)
    @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content)
    public ResponseEntity<PlanResponse> publish(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody PublishPlanRequest request) {
        var organizer = new Organizer(UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("name"));
        var plan = publishPlan.publish(new PublishPlanCommand(organizer, request.activity(), request.title(),
                request.description(), request.meetingPoint().toDomain(), request.startsAt(), request.spots(),
                request.level()));
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path(PLAN).buildAndExpand(plan.id()).toUri();
        return ResponseEntity.created(location).body(PlanResponse.of(plan));
    }

    @GetMapping(PLAN)
    @Operation(summary = "Plan detail")
    @ApiResponse(responseCode = "200", description = "Plan")
    @ApiResponse(responseCode = "404", description = "The plan does not exist", content = @Content)
    public PlanResponse plan(@PathVariable UUID planId) {
        return PlanResponse.of(queryPlans.plan(planId));
    }

    @GetMapping(MINE)
    @Operation(summary = "My upcoming plans", description = "Plans I organize that have not started yet.")
    public List<PlanResponse> mine(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return queryPlans.upcomingPlansOrganizedBy(UUID.fromString(jwt.getSubject())).stream()
                .map(PlanResponse::of).toList();
    }

    @GetMapping(NEARBY)
    @Operation(summary = "Nearby plans (HU-004)",
            description = "Open plans with free spots less than `radius` metres away, from the nearest to the "
                    + "farthest. Own plans are not included. At most 50.")
    @ApiResponse(responseCode = "200", description = "Nearby plans")
    @ApiResponse(responseCode = "400", description = "Parameters out of range", content = @Content)
    @SuppressWarnings("java:S107")
    public List<NearbyPlanResponse> nearby(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                           @Parameter(example = "40.391") @RequestParam double latitude,
                                           @Parameter(example = "-3.629") @RequestParam double longitude,
                                           @Parameter(description = "Radius in metres (500 to 25,000)")
                                           @RequestParam(defaultValue = "5000") int radius,
                                           @Parameter(description = "Activities; all of them if omitted")
                                           @RequestParam(name = "activity", required = false) List<Activity> activities,
                                           @Parameter(description = "Starting within the next hours (1 to 12)")
                                           @RequestParam(defaultValue = "12") int withinHours) {
        var search = search(jwt, latitude, longitude, radius, activities, withinHours);
        return queryPlans.nearbyPlans(search).stream().map(NearbyPlanResponse::of).toList();
    }

    @GetMapping(path = NEARBY_STREAM, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Nearby plans in real time (HU-004)",
            description = "Server-Sent Events with the same filters as `/nearby`. Emits `ready` on connection and "
                    + "`plan-published` with a NearbyPlanEvent every time a matching plan is published.")
    @ApiResponse(responseCode = "200", description = "Event stream",
            content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = NearbyPlanEvent.class)))
    @SuppressWarnings("java:S107")
    public SseEmitter nearbyStream(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                   @RequestParam double latitude, @RequestParam double longitude,
                                   @RequestParam(defaultValue = "5000") int radius,
                                   @RequestParam(name = "activity", required = false) List<Activity> activities,
                                   @RequestParam(defaultValue = "12") int withinHours) {
        return nearbySubscriptions.subscribe(search(jwt, latitude, longitude, radius, activities, withinHours));
    }

    private static NearbySearch search(Jwt jwt, double latitude, double longitude, int radius,
                                       List<Activity> activities, int withinHours) {
        return new NearbySearch(latitude, longitude, radius, activities == null ? null : new HashSet<>(activities),
                Duration.ofHours(withinHours), UUID.fromString(jwt.getSubject()));
    }
}
