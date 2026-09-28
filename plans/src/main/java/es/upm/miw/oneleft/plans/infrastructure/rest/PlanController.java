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
@Tag(name = "Planes", description = "Planes para las próximas horas con plazas libres")
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
    @Operation(summary = "Publicar un plan (HU-003)",
            description = "Publica un plan que empieza entre 5 minutos y 12 horas desde ahora, con las plazas libres "
                    + "que faltan por cubrir. Emite el evento PlanPublicado.")
    @ApiResponse(responseCode = "201", description = "Plan publicado")
    @ApiResponse(responseCode = "400", description = "Datos no válidos", content = @Content)
    @ApiResponse(responseCode = "401", description = "Falta el token o no es válido", content = @Content)
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
    @Operation(summary = "Detalle de un plan")
    @ApiResponse(responseCode = "200", description = "Plan")
    @ApiResponse(responseCode = "404", description = "El plan no existe", content = @Content)
    public PlanResponse plan(@PathVariable UUID planId) {
        return PlanResponse.of(queryPlans.plan(planId));
    }

    @GetMapping(MINE)
    @Operation(summary = "Mis próximos planes", description = "Planes que organizo y aún no han empezado.")
    public List<PlanResponse> mine(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return queryPlans.upcomingPlansOrganizedBy(UUID.fromString(jwt.getSubject())).stream()
                .map(PlanResponse::of).toList();
    }

    @GetMapping(NEARBY)
    @Operation(summary = "Planes cercanos (HU-004)",
            description = "Planes abiertos con plazas libres a menos de `radius` metros, del más cercano al más "
                    + "lejano. No incluye los planes propios. Como mucho 50.")
    @ApiResponse(responseCode = "200", description = "Planes cercanos")
    @ApiResponse(responseCode = "400", description = "Parámetros fuera de rango", content = @Content)
    @SuppressWarnings("java:S107")
    public List<NearbyPlanResponse> nearby(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                           @Parameter(example = "40.391") @RequestParam double latitude,
                                           @Parameter(example = "-3.629") @RequestParam double longitude,
                                           @Parameter(description = "Radio en metros (500 a 25 000)")
                                           @RequestParam(defaultValue = "5000") int radius,
                                           @Parameter(description = "Actividades; sin indicar, todas")
                                           @RequestParam(name = "activity", required = false) List<Activity> activities,
                                           @Parameter(description = "Empiezan en las próximas horas (1 a 12)")
                                           @RequestParam(defaultValue = "12") int withinHours) {
        var search = search(jwt, latitude, longitude, radius, activities, withinHours);
        return queryPlans.nearbyPlans(search).stream().map(NearbyPlanResponse::of).toList();
    }

    @GetMapping(path = NEARBY_STREAM, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Planes cercanos en tiempo real (HU-004)",
            description = "Server-Sent Events con los mismos filtros que `/nearby`. Emite `ready` al conectar y "
                    + "`plan-published` con un NearbyPlanEvent cada vez que se publica un plan que encaja.")
    @ApiResponse(responseCode = "200", description = "Flujo de eventos",
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
