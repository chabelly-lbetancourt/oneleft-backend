package es.upm.miw.oneleft.social.infrastructure.rest;

import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.port.in.FollowUseCase;
import es.upm.miw.oneleft.social.domain.port.in.SocialAccountUseCase;
import es.upm.miw.oneleft.social.infrastructure.config.KeycloakJwt;
import es.upm.miw.oneleft.social.infrastructure.rest.SocialDtos.AccountResponse;
import es.upm.miw.oneleft.social.infrastructure.rest.SocialDtos.PageResponse;
import es.upm.miw.oneleft.social.infrastructure.rest.SocialDtos.PersonResponse;
import es.upm.miw.oneleft.social.infrastructure.rest.SocialDtos.VisibilityRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** My social profile and how I see someone else (HU-047). */
@RestController
@RequestMapping(SocialController.SOCIAL)
@Tag(name = "Social", description = "Following people: public and private profiles, requests and lists")
public class SocialController {

    public static final String SOCIAL = "/api/v1/social";
    public static final String ME = "/me";
    public static final String MY_VISIBILITY = ME + "/visibility";
    public static final String PERSON = "/people/{userId}";
    public static final String PERSON_FOLLOWERS = PERSON + "/followers";
    public static final String PERSON_FOLLOWING = PERSON + "/following";

    private final SocialAccountUseCase accounts;
    private final FollowUseCase follows;

    public SocialController(SocialAccountUseCase accounts, FollowUseCase follows) {
        this.accounts = accounts;
        this.follows = follows;
    }

    @GetMapping(ME)
    @Operation(summary = "My social profile",
            description = "Created private, with the name of my account, the first time I use the service.")
    @ApiResponse(responseCode = "200", description = "My profile and my counts")
    public AccountResponse myAccount(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return AccountResponse.of(accounts.myAccount(KeycloakJwt.caller(jwt)));
    }

    @PutMapping(MY_VISIBILITY)
    @Operation(summary = "Public or private profile",
            description = "Anyone follows a public profile at once; a private one accepts each request. Opening a "
                    + "private profile accepts the requests that were waiting; closing it keeps the followers.")
    @ApiResponse(responseCode = "200", description = "My profile and my counts")
    @ApiResponse(responseCode = "400", description = "Missing or unknown visibility", content = @Content)
    public AccountResponse changeVisibility(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                            @Valid @RequestBody VisibilityRequest request) {
        return AccountResponse.of(accounts.changeVisibility(KeycloakJwt.caller(jwt), request.visibility()));
    }

    @GetMapping(PERSON)
    @Operation(summary = "Someone as I see them",
            description = "Their name, counts, whether I follow them (or asked to) and whether they follow me.")
    @ApiResponse(responseCode = "200", description = "The person")
    @ApiResponse(responseCode = "404", description = "person.notFound", content = @Content)
    @ApiResponse(responseCode = "503", description = "social.unavailable", content = @Content)
    public PersonResponse person(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                 @PathVariable UUID userId) {
        return PersonResponse.of(accounts.person(KeycloakJwt.caller(jwt), userId));
    }

    @GetMapping(PERSON_FOLLOWERS)
    @Operation(summary = "The followers of someone (HU-048)",
            description = "The latest first. Only if their profile is public, it is mine or I follow them.")
    @ApiResponse(responseCode = "200", description = "A page of the list")
    @ApiResponse(responseCode = "403", description = "social.private", content = @Content)
    @ApiResponse(responseCode = "404", description = "person.notFound", content = @Content)
    public PageResponse followersOf(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                    @PathVariable UUID userId,
                                    @RequestParam(required = false) Integer page,
                                    @RequestParam(required = false) Integer size) {
        return PageResponse.of(follows.followersOf(KeycloakJwt.caller(jwt), userId, page(page, size)));
    }

    @GetMapping(PERSON_FOLLOWING)
    @Operation(summary = "Who someone follows (HU-048)",
            description = "The latest first. Only if their profile is public, it is mine or I follow them.")
    @ApiResponse(responseCode = "200", description = "A page of the list")
    @ApiResponse(responseCode = "403", description = "social.private", content = @Content)
    @ApiResponse(responseCode = "404", description = "person.notFound", content = @Content)
    public PageResponse followingOf(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                    @PathVariable UUID userId,
                                    @RequestParam(required = false) Integer page,
                                    @RequestParam(required = false) Integer size) {
        return PageResponse.of(follows.followingOf(KeycloakJwt.caller(jwt), userId, page(page, size)));
    }

    private static PageQuery page(Integer page, Integer size) {
        return new PageQuery(page == null ? 0 : page, size == null ? PageQuery.DEFAULT_SIZE : size);
    }
}
