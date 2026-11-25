package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.domain.port.in.TrustedLinksUseCase;
import es.upm.miw.oneleft.plans.infrastructure.config.ShareProperties;
import es.upm.miw.oneleft.plans.infrastructure.rest.TrustedLinkDtos.CreatedTrustedLinkResponse;
import es.upm.miw.oneleft.plans.infrastructure.rest.TrustedLinkDtos.TrustedLinksResponse;
import es.upm.miw.oneleft.plans.infrastructure.rest.TrustedLinkDtos.TrustedPlanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * Sharing a plan with a trusted contact (HU-034). Whoever goes to the plan creates and revokes their links with
 * their session; the contact opens one without an account, as the shared plans of HU-024, but with a secret instead
 * of the id of the plan and with the exact place.
 */
@RestController
@Tag(name = "Trusted contacts", description = "Links to tell someone I trust where, when and with whom I meet "
        + "(HU-034)")
public class TrustedLinkController {

    public static final String TRUSTED_LINKS = PlanController.PLANS + PlanController.PLAN + "/trusted-links";
    public static final String TRUSTED_LINK = TRUSTED_LINKS + "/{linkId}";
    public static final String PUBLIC_TRUSTED_LINK = "/api/v1/public/plans/trusted-links/{token}";
    /** Where the web app shows the plan to the contact */
    static final String WEB_PATH = "/trusted/";

    private final TrustedLinksUseCase links;
    private final ShareProperties share;

    public TrustedLinkController(TrustedLinksUseCase links, ShareProperties share) {
        this.links = links;
        this.share = share;
    }

    @PostMapping(TRUSTED_LINKS)
    @Operation(summary = "Create a link for a trusted contact",
            description = "Only the organizer and the participants, until the plan is over. The link is read-only, "
                    + "expires when the plan is over (3 hours after it starts) and stops working if I leave the "
                    + "plan. Its address is only given in this answer: send it with the share sheet of the device. "
                    + "Up to 5 links per person and plan.")
    @ApiResponse(responseCode = "201", description = "The link, with its address")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content)
    @ApiResponse(responseCode = "403", description = "plan.notParticipant: neither the organizer nor a participant",
            content = @Content)
    @ApiResponse(responseCode = "404", description = "plan.notFound", content = @Content)
    @ApiResponse(responseCode = "409", description = "trustedLink.planOver (the plan is over or cancelled) or "
            + "trustedLink.limit (5 links already)", content = @Content)
    public ResponseEntity<CreatedTrustedLinkResponse> create(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                             @PathVariable UUID planId) {
        var issued = links.create(planId, userId(jwt));
        // The secret must not stay in any cache
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(CreatedTrustedLinkResponse.of(issued, webUrl(issued.token())));
    }

    @GetMapping(TRUSTED_LINKS)
    @Operation(summary = "My links for a plan that still work",
            description = "Only mine, oldest first and at most 5, so there are no pages. Without their addresses.")
    @ApiResponse(responseCode = "200", description = "My links; empty if I have none or the plan does not exist")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content)
    public TrustedLinksResponse mine(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                     @PathVariable UUID planId) {
        return TrustedLinksResponse.of(links.mine(planId, userId(jwt)));
    }

    @DeleteMapping(TRUSTED_LINK)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke a link of mine",
            description = "It stops working at once. Idempotent: revoking it again, or a link that is not mine, "
                    + "answers the same and changes nothing.")
    @ApiResponse(responseCode = "204", description = "Revoked, or there was nothing of mine to revoke")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content)
    public void revoke(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable UUID planId,
                       @PathVariable UUID linkId) {
        links.revoke(planId, userId(jwt), linkId);
    }

    @GetMapping(PUBLIC_TRUSTED_LINK)
    @Operation(summary = "The plan as a trusted contact sees it",
            description = "No session needed: whoever has the link. Where (the exact meeting point), when and with "
                    + "whom (the name of the organizer and of whoever shared it); nobody else of the group and no "
                    + "identifiers. A link that does not exist, expired, was revoked or belongs to someone who left "
                    + "the plan answers exactly the same 404.")
    @ApiResponse(responseCode = "200", description = "The plan")
    @ApiResponse(responseCode = "404", description = "trustedLink.notFound", content = @Content)
    public ResponseEntity<TrustedPlanResponse> open(@PathVariable String token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer")
                .header("X-Robots-Tag", "noindex, nofollow")
                .body(TrustedPlanResponse.of(links.open(token)));
    }

    private URI webUrl(String token) {
        return share.webUrl().resolve(WEB_PATH + token);
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
