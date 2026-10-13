package es.upm.miw.oneleft.social.infrastructure.rest;

import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.port.in.FollowUseCase;
import es.upm.miw.oneleft.social.infrastructure.config.KeycloakJwt;
import es.upm.miw.oneleft.social.infrastructure.rest.SocialDtos.ConnectionResponse;
import es.upm.miw.oneleft.social.infrastructure.rest.SocialDtos.PageResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Following people, the requests to private profiles and my lists (HU-047). */
@RestController
@RequestMapping(SocialController.SOCIAL + SocialController.ME)
@Tag(name = "Social", description = "Following people: public and private profiles, requests and lists")
public class FollowController {

    public static final String FOLLOWING = "/following";
    public static final String FOLLOWED = FOLLOWING + "/{userId}";
    public static final String FOLLOWERS = "/followers";
    public static final String FOLLOWER = FOLLOWERS + "/{userId}";
    public static final String REQUESTS = "/requests";
    public static final String SENT_REQUESTS = REQUESTS + "/sent";
    public static final String REQUEST = REQUESTS + "/{userId}";
    public static final String ACCEPT = REQUEST + "/accept";

    private final FollowUseCase follows;

    public FollowController(FollowUseCase follows) {
        this.follows = follows;
    }

    private static UUID me(Jwt jwt) {
        return KeycloakJwt.userId(jwt);
    }

    private static PageQuery page(Integer page, Integer size) {
        return new PageQuery(page == null ? 0 : page, size == null ? PageQuery.DEFAULT_SIZE : size);
    }

    @PutMapping(FOLLOWED)
    @Operation(summary = "Follow someone",
            description = "At once if their profile is public (ACCEPTED); a request if it is private (PENDING). "
                    + "Doing it again changes nothing.")
    @ApiResponse(responseCode = "200", description = "The follow or the request")
    @ApiResponse(responseCode = "400", description = "follow.self", content = @Content)
    @ApiResponse(responseCode = "404", description = "person.notFound", content = @Content)
    @ApiResponse(responseCode = "503", description = "social.unavailable", content = @Content)
    public ConnectionResponse follow(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                     @PathVariable UUID userId) {
        return ConnectionResponse.of(follows.follow(KeycloakJwt.caller(jwt), userId));
    }

    @DeleteMapping(FOLLOWED)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Stop following someone, or take back my request")
    @ApiResponse(responseCode = "204", description = "Not following any more")
    public void unfollow(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
        follows.unfollow(me(jwt), userId);
    }

    @GetMapping(FOLLOWING)
    @Operation(summary = "The people I follow", description = "The latest first.")
    @ApiResponse(responseCode = "200", description = "A page of the list")
    @ApiResponse(responseCode = "400", description = "page.invalid", content = @Content)
    public PageResponse following(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                  @RequestParam(required = false) Integer page,
                                  @RequestParam(required = false) Integer size) {
        return PageResponse.of(follows.following(me(jwt), page(page, size)));
    }

    @GetMapping(FOLLOWERS)
    @Operation(summary = "My followers", description = "The latest first.")
    @ApiResponse(responseCode = "200", description = "A page of the list")
    @ApiResponse(responseCode = "400", description = "page.invalid", content = @Content)
    public PageResponse followers(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                  @RequestParam(required = false) Integer page,
                                  @RequestParam(required = false) Integer size) {
        return PageResponse.of(follows.followers(me(jwt), page(page, size)));
    }

    @DeleteMapping(FOLLOWER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a follower", description = "They can ask again.")
    @ApiResponse(responseCode = "204", description = "Not a follower any more")
    public void removeFollower(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                               @PathVariable UUID userId) {
        follows.removeFollower(me(jwt), userId);
    }

    @GetMapping(REQUESTS)
    @Operation(summary = "The requests waiting for my answer", description = "The latest first.")
    @ApiResponse(responseCode = "200", description = "A page of the list")
    @ApiResponse(responseCode = "400", description = "page.invalid", content = @Content)
    public PageResponse requests(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                 @RequestParam(required = false) Integer page,
                                 @RequestParam(required = false) Integer size) {
        return PageResponse.of(follows.requests(me(jwt), page(page, size)));
    }

    @GetMapping(SENT_REQUESTS)
    @Operation(summary = "My requests waiting for an answer", description = "The latest first.")
    @ApiResponse(responseCode = "200", description = "A page of the list")
    @ApiResponse(responseCode = "400", description = "page.invalid", content = @Content)
    public PageResponse sentRequests(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                     @RequestParam(required = false) Integer page,
                                     @RequestParam(required = false) Integer size) {
        return PageResponse.of(follows.sentRequests(me(jwt), page(page, size)));
    }

    @PostMapping(ACCEPT)
    @Operation(summary = "Accept a request", description = "Whoever asked becomes one of my followers.")
    @ApiResponse(responseCode = "200", description = "The new follower")
    @ApiResponse(responseCode = "404", description = "follow.requestNotFound", content = @Content)
    public ConnectionResponse accept(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                     @PathVariable UUID userId) {
        return ConnectionResponse.of(follows.accept(me(jwt), userId));
    }

    @DeleteMapping(REQUEST)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Decline a request")
    @ApiResponse(responseCode = "204", description = "No request any more")
    public void decline(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
        follows.decline(me(jwt), userId);
    }
}
