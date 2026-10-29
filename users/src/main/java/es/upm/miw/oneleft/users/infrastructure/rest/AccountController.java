package es.upm.miw.oneleft.users.infrastructure.rest;

import es.upm.miw.oneleft.users.domain.model.ConsentStatus;
import es.upm.miw.oneleft.users.domain.port.in.ManageAccountUseCase;
import es.upm.miw.oneleft.users.domain.port.in.ManageConsentUseCase;
import es.upm.miw.oneleft.users.infrastructure.config.KeycloakJwt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/** My data, my account and my consent to the privacy text (HU-045). */
@RestController
@RequestMapping(UserController.USERS + "/me")
@Tag(name = "My account", description = "Download my data, delete my account and accept the privacy text (RGPD)")
public class AccountController {

    public static final String MY_DATA = "/data";
    public static final String MY_ACCOUNT = "/account";
    public static final String MY_CONSENT = "/consent";

    private final ManageAccountUseCase account;
    private final ManageConsentUseCase consent;

    public AccountController(ManageAccountUseCase account, ManageConsentUseCase consent) {
        this.account = account;
        this.consent = consent;
    }

    @GetMapping(MY_DATA)
    @Operation(summary = "Download all my data",
            description = "One JSON file with what every service keeps of me (right of access and portability).")
    @ApiResponse(responseCode = "200", description = "My data, as a file to save")
    @ApiResponse(responseCode = "503", description = "data.unavailable: a service cannot give its part",
            content = @Content)
    public ResponseEntity<Map<String, Object>> myData(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        var copy = account.exportMyData(KeycloakJwt.toIdentity(jwt));
        var file = ContentDisposition.attachment().filename("oneleft-my-data.json").build();
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, file.toString()).body(copy);
    }

    @DeleteMapping(MY_ACCOUNT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete my account",
            description = "Erases my data here and in Keycloak and announces it so that every service erases its part. "
                    + "What is shared with other people stays, without my name.")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "503", description = "account.unavailable: try again", content = @Content)
    public void deleteMyAccount(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        account.deleteMyAccount(KeycloakJwt.toIdentity(jwt));
    }

    @GetMapping(MY_CONSENT)
    @Operation(summary = "Whether I accepted the privacy text in force")
    @ApiResponse(responseCode = "200", description = "The version in force and the one I accepted, if any")
    public ConsentResponse myConsent(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ConsentResponse.of(consent.status(KeycloakJwt.toIdentity(jwt)));
    }

    @PutMapping(MY_CONSENT)
    @Operation(summary = "Accept the privacy text", description = "Only the version in force can be accepted.")
    @ApiResponse(responseCode = "200", description = "Accepted")
    @ApiResponse(responseCode = "400", description = "consent.version", content = @Content)
    public ConsentResponse accept(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                  @Valid @RequestBody ConsentRequest request) {
        return ConsentResponse.of(consent.accept(KeycloakJwt.toIdentity(jwt), request.version()));
    }

    public record ConsentRequest(@NotBlank String version) {
    }

    public record ConsentResponse(boolean accepted, String acceptedVersion, Instant acceptedAt, String currentVersion) {

        static ConsentResponse of(ConsentStatus status) {
            return new ConsentResponse(status.accepted(), status.acceptedVersion(), status.acceptedAt(),
                    status.currentVersion());
        }
    }
}
