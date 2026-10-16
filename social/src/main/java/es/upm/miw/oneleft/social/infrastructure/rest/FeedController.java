package es.upm.miw.oneleft.social.infrastructure.rest;

import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.port.in.FeedUseCase;
import es.upm.miw.oneleft.social.domain.port.in.HiddenPlansUseCase;
import es.upm.miw.oneleft.social.infrastructure.config.KeycloakJwt;
import es.upm.miw.oneleft.social.infrastructure.rest.SocialDtos.FeedPageResponse;
import es.upm.miw.oneleft.social.infrastructure.rest.SocialDtos.HiddenPlanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** The activity of the people I follow (HU-051) and the plans I keep out of mine (HU-059). */
@RestController
@RequestMapping(SocialController.SOCIAL + SocialController.ME)
@Tag(name = "Feed", description = "What the people I follow do in the plans")
public class FeedController {

    public static final String FEED = "/feed";
    public static final String HIDDEN_PLAN = "/hidden-plans/{planId}";

    private final FeedUseCase feeds;
    private final HiddenPlansUseCase hiddenPlans;

    public FeedController(FeedUseCase feeds, HiddenPlansUseCase hiddenPlans) {
        this.feeds = feeds;
        this.hiddenPlans = hiddenPlans;
    }

    @GetMapping(FEED)
    @Operation(summary = "My feed",
            description = "The plans the people I follow publish, join and complete, the latest first. Only the "
                    + "people whose follow is accepted: a private profile shows up once it accepts me. New activity "
                    + "arrives through the real-time stream as a feed-activity event.")
    @ApiResponse(responseCode = "200", description = "A page of the feed")
    @ApiResponse(responseCode = "400", description = "page.invalid", content = @Content)
    public FeedPageResponse feed(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                 @RequestParam(required = false) Integer page,
                                 @RequestParam(required = false) Integer size) {
        var query = new PageQuery(page == null ? 0 : page, size == null ? PageQuery.DEFAULT_SIZE : size);
        return FeedPageResponse.of(feeds.feed(KeycloakJwt.userId(jwt), query));
    }

    @GetMapping(HIDDEN_PLAN)
    @Operation(summary = "Whether a plan is kept out of my activity")
    @ApiResponse(responseCode = "200", description = "Hidden or not")
    public HiddenPlanResponse hiddenPlan(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                         @PathVariable UUID planId) {
        return new HiddenPlanResponse(planId, hiddenPlans.isHidden(KeycloakJwt.userId(jwt), planId));
    }

    @PutMapping(HIDDEN_PLAN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Keep a plan out of my activity",
            description = "My followers stop seeing that I published, joined or completed it, and nothing new of it "
                    + "is kept. Doing it again changes nothing.")
    @ApiResponse(responseCode = "204", description = "Hidden")
    public void hidePlan(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable UUID planId) {
        hiddenPlans.hidePlan(KeycloakJwt.userId(jwt), planId);
    }

    @DeleteMapping(HIDDEN_PLAN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Show a plan in my activity again", description = "What was kept of it shows up again.")
    @ApiResponse(responseCode = "204", description = "Shown")
    public void showPlan(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable UUID planId) {
        hiddenPlans.showPlan(KeycloakJwt.userId(jwt), planId);
    }
}
