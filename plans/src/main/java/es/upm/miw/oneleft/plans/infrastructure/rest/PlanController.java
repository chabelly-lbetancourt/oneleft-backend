package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.domain.model.Organizer;
import es.upm.miw.oneleft.plans.domain.port.in.PublishPlanCommand;
import es.upm.miw.oneleft.plans.domain.port.in.PublishPlanUseCase;
import es.upm.miw.oneleft.plans.domain.port.in.QueryPlansUseCase;
import es.upm.miw.oneleft.plans.infrastructure.rest.PlanDtos.PlanResponse;
import es.upm.miw.oneleft.plans.infrastructure.rest.PlanDtos.PublishPlanRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(PlanController.PLANS)
@Tag(name = "Planes", description = "Planes para las próximas horas con plazas libres")
public class PlanController {

    public static final String PLANS = "/api/v1/plans";
    public static final String PLAN = "/{planId}";
    public static final String MINE = "/mine";

    private final PublishPlanUseCase publishPlan;
    private final QueryPlansUseCase queryPlans;

    public PlanController(PublishPlanUseCase publishPlan, QueryPlansUseCase queryPlans) {
        this.publishPlan = publishPlan;
        this.queryPlans = queryPlans;
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
}
