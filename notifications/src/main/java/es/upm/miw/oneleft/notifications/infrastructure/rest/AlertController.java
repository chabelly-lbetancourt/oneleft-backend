package es.upm.miw.oneleft.notifications.infrastructure.rest;

import es.upm.miw.oneleft.notifications.domain.model.Activity;
import es.upm.miw.oneleft.notifications.domain.model.Level;
import es.upm.miw.oneleft.notifications.domain.model.SavedAlert;
import es.upm.miw.oneleft.notifications.domain.port.in.ManageAlertsUseCase;
import es.upm.miw.oneleft.notifications.infrastructure.config.KeycloakJwt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Saved alerts (HU-036): searches that turn into notices when a plan like them is published. */
@RestController
@RequestMapping(AlertController.ALERTS)
@Tag(name = "Alerts", description = "Saved searches that notify matching plans (HU-036)")
public class AlertController {

    public static final String ALERTS = NotificationController.NOTIFICATIONS + "/alerts";
    public static final String ALERT = "/{alertId}";

    private final ManageAlertsUseCase alerts;

    public AlertController(ManageAlertsUseCase alerts) {
        this.alerts = alerts;
    }

    @GetMapping
    @Operation(summary = "My saved alerts", description = "Oldest first.")
    public List<AlertDto> mine(@AuthenticationPrincipal Jwt jwt) {
        return alerts.alertsOf(KeycloakJwt.userId(jwt)).stream().map(AlertDto::of).toList();
    }

    @PostMapping
    @Operation(summary = "Save an alert", description = "Up to " + ManageAlertsUseCase.MAX_ALERTS + " per person.")
    @ApiResponse(responseCode = "201", description = "Saved alert")
    @ApiResponse(responseCode = "400", description = "Invalid alert (code in the problem)")
    @ApiResponse(responseCode = "409", description = "alerts.limit")
    public ResponseEntity<AlertDto> create(@AuthenticationPrincipal Jwt jwt, @RequestBody AlertDto request) {
        var userId = KeycloakJwt.userId(jwt);
        var alert = alerts.create(userId, request.toDomain(userId));
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path(ALERT).buildAndExpand(alert.id()).toUri();
        return ResponseEntity.created(location).body(AlertDto.of(alert));
    }

    @PutMapping(ALERT)
    @Operation(summary = "Edit one of my alerts")
    @ApiResponse(responseCode = "200", description = "Saved alert")
    @ApiResponse(responseCode = "404", description = "alerts.notFound")
    public AlertDto update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID alertId,
                           @RequestBody AlertDto request) {
        var userId = KeycloakJwt.userId(jwt);
        return AlertDto.of(alerts.update(userId, alertId, request.toDomain(userId)));
    }

    @DeleteMapping(ALERT)
    @Operation(summary = "Delete one of my alerts")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "404", description = "alerts.notFound")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID alertId) {
        alerts.delete(KeycloakJwt.userId(jwt), alertId);
        return ResponseEntity.noContent().build();
    }

    @Schema(description = "Saved alert: the plans it looks for")
    public record AlertDto(UUID id, @Schema(example = "Padel after work") String name, Set<Activity> activities,
                           @Schema(description = "Null for any level") Level level,
                           @Schema(example = "40.39") double latitude, @Schema(example = "-3.63") double longitude,
                           @Schema(example = "2000") int radiusMeters,
                           @Schema(description = "Empty for any day") Set<DayOfWeek> days,
                           @Schema(example = "17:00") LocalTime from, @Schema(example = "21:00") LocalTime to) {

        static AlertDto of(SavedAlert alert) {
            return new AlertDto(alert.id(), alert.name(), alert.activities(), alert.level(), alert.latitude(),
                    alert.longitude(), alert.radiusMeters(), alert.days(), alert.from(), alert.to());
        }

        /** The id and the owner come from the path and the token, never from the body. */
        SavedAlert toDomain(UUID userId) {
            return new SavedAlert(UUID.randomUUID(), userId, name, activities, level, latitude, longitude,
                    radiusMeters, days, from, to);
        }
    }
}
