package es.upm.miw.oneleft.notifications.infrastructure.rest;

import es.upm.miw.oneleft.notifications.domain.port.in.ManagePreferencesUseCase;
import es.upm.miw.oneleft.notifications.domain.port.in.ManagePushSubscriptionsUseCase;
import es.upm.miw.oneleft.notifications.infrastructure.config.KeycloakJwt;
import es.upm.miw.oneleft.notifications.infrastructure.rest.NotificationDtos.PreferencesDto;
import es.upm.miw.oneleft.notifications.infrastructure.rest.NotificationDtos.PublicKeyResponse;
import es.upm.miw.oneleft.notifications.infrastructure.rest.NotificationDtos.SubscriptionRequest;
import es.upm.miw.oneleft.notifications.infrastructure.webpush.WebPushProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Notices of nearby plans (HU-006): what each person wants to hear about and the browsers that get them as system
 * notifications.
 */
@RestController
@RequestMapping(NotificationController.NOTIFICATIONS)
@Tag(name = "Notifications", description = "Notices of nearby plans (HU-006)")
public class NotificationController {

    public static final String NOTIFICATIONS = "/api/v1/notifications";

    private final ManagePreferencesUseCase preferences;
    private final ManagePushSubscriptionsUseCase subscriptions;
    private final WebPushProperties webPush;

    public NotificationController(ManagePreferencesUseCase preferences, ManagePushSubscriptionsUseCase subscriptions,
                                  WebPushProperties webPush) {
        this.preferences = preferences;
        this.subscriptions = subscriptions;
        this.webPush = webPush;
    }

    @GetMapping("/preferences")
    @Operation(summary = "My notification preferences", description = "Notices are off until the person turns them on.")
    public PreferencesDto myPreferences(@AuthenticationPrincipal Jwt jwt) {
        return PreferencesDto.of(preferences.preferencesOf(KeycloakJwt.userId(jwt)));
    }

    @PutMapping("/preferences")
    @Operation(summary = "Save my notification preferences",
            description = "Zone (rounded to about 1 km), radius, activities (none for all), quiet hours and daily limit.")
    @ApiResponse(responseCode = "200", description = "Saved preferences")
    @ApiResponse(responseCode = "400", description = "Invalid preferences (code in the problem)")
    public PreferencesDto updatePreferences(@AuthenticationPrincipal Jwt jwt, @RequestBody PreferencesDto request) {
        return PreferencesDto.of(preferences.update(request.toDomain(KeycloakJwt.userId(jwt))));
    }

    @GetMapping("/push/public-key")
    @Operation(summary = "Server key for Web Push (VAPID)", description = "Browsers subscribe with it.")
    @ApiResponse(responseCode = "200", description = "The public key, base64url")
    @ApiResponse(responseCode = "404", description = "Web Push is not configured on this server")
    public ResponseEntity<PublicKeyResponse> publicKey() {
        return webPush.configured()
                ? ResponseEntity.ok(new PublicKeyResponse(webPush.publicKey()))
                : ResponseEntity.notFound().build();
    }

    @PutMapping("/push/subscriptions")
    @Operation(summary = "Receive the notices in this browser", description = "The PushSubscription of the Push API.")
    @ApiResponse(responseCode = "204", description = "Subscribed")
    public ResponseEntity<Void> subscribe(@AuthenticationPrincipal Jwt jwt, @RequestBody SubscriptionRequest request) {
        subscriptions.subscribe(request.toDomain(KeycloakJwt.userId(jwt)));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/push/subscriptions")
    @Operation(summary = "Stop receiving the notices in this browser")
    @ApiResponse(responseCode = "204", description = "Unsubscribed")
    public ResponseEntity<Void> unsubscribe(@AuthenticationPrincipal Jwt jwt, @RequestParam String endpoint) {
        subscriptions.unsubscribe(KeycloakJwt.userId(jwt), endpoint);
        return ResponseEntity.noContent().build();
    }
}
