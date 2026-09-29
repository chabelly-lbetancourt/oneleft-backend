package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException;
import es.upm.miw.oneleft.plans.domain.port.in.QueryPlansUseCase;
import es.upm.miw.oneleft.plans.infrastructure.rest.PlanDtos.PublicPlanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Plans shared by link (HU-024), without a session. Only what the link needs: no people, approximate place.
 */
@RestController
@Tag(name = "Shared plans", description = "Plans opened from a shared link, without a session (HU-024)")
public class PublicPlanController {

    public static final String PUBLIC_PLAN = "/api/v1/public/plans/{planId}";
    public static final String SHARE = "/share/plans/{planId}";
    private static final Locale SPANISH = Locale.forLanguageTag("es");

    private final QueryPlansUseCase queryPlans;
    private final SharePage sharePage;

    public PublicPlanController(QueryPlansUseCase queryPlans, SharePage sharePage) {
        this.queryPlans = queryPlans;
        this.sharePage = sharePage;
    }

    @GetMapping(PUBLIC_PLAN)
    @Operation(summary = "A plan as anyone with the link sees it",
            description = "No session needed. Without the organizer or participants, and with the meeting point "
                    + "rounded to about 100 m.")
    @ApiResponse(responseCode = "200", description = "Public view of the plan")
    @ApiResponse(responseCode = "404", description = "The plan does not exist", content = @Content)
    public PublicPlanResponse publicPlan(@PathVariable UUID planId) {
        return PublicPlanResponse.of(queryPlans.plan(planId));
    }

    @GetMapping(path = SHARE, produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Link to share a plan",
            description = "HTML page with Open Graph tags (title, activity, time, free spots) for the link preview in "
                    + "messaging apps and social networks; it sends people on to the plan in the web app.")
    @ApiResponse(responseCode = "200", description = "Preview page", content = @Content(mediaType = "text/html"))
    @ApiResponse(responseCode = "404", description = "The plan does not exist: a page that leads to the home screen",
            content = @Content(mediaType = "text/html"))
    public ResponseEntity<String> share(@PathVariable UUID planId,
                                        @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false)
                                        String acceptLanguage) {
        var locale = language(acceptLanguage);
        try {
            var plan = queryPlans.plan(planId);
            // The spots change: previews can be cached only for a short while
            return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                    .body(sharePage.of(plan, locale));
        } catch (PlanNotFoundException missing) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(sharePage.missing(locale));
        }
    }

    /**
     * Language of the preview: the first one the client asks for. Crawlers often send none; then Spanish, the main
     * language of the app, rather than the language of the server.
     */
    static Locale language(String acceptLanguage) {
        try {
            var ranges = acceptLanguage == null ? List.<Locale.LanguageRange>of() : Locale.LanguageRange.parse(acceptLanguage);
            return ranges.isEmpty() ? SPANISH : Locale.forLanguageTag(ranges.getFirst().getRange());
        } catch (IllegalArgumentException malformed) {
            return SPANISH;
        }
    }
}
