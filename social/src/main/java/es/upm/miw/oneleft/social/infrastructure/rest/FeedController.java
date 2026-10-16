package es.upm.miw.oneleft.social.infrastructure.rest;

import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.port.in.FeedUseCase;
import es.upm.miw.oneleft.social.infrastructure.config.KeycloakJwt;
import es.upm.miw.oneleft.social.infrastructure.rest.SocialDtos.FeedPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The activity of the people I follow (HU-051). */
@RestController
@RequestMapping(SocialController.SOCIAL + SocialController.ME)
@Tag(name = "Feed", description = "What the people I follow do in the plans")
public class FeedController {

    public static final String FEED = "/feed";

    private final FeedUseCase feeds;

    public FeedController(FeedUseCase feeds) {
        this.feeds = feeds;
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
}
