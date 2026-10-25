package es.upm.miw.oneleft.social.infrastructure.rest;

import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.port.in.BlockUseCase;
import es.upm.miw.oneleft.social.infrastructure.config.KeycloakJwt;
import es.upm.miw.oneleft.social.infrastructure.rest.SafetyDtos.BlockedPageResponse;
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

/** Blocking people (HU-033). */
@RestController
@RequestMapping(SocialController.SOCIAL + SocialController.ME)
@Tag(name = "Safety", description = "Blocking and reporting people and plans")
public class BlockController {

    public static final String BLOCKED = "/blocked";
    public static final String BLOCKED_PERSON = BLOCKED + "/{userId}";

    private final BlockUseCase blocks;

    public BlockController(BlockUseCase blocks) {
        this.blocks = blocks;
    }

    @PutMapping(BLOCKED_PERSON)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Block someone",
            description = "We stop following each other, neither of us sees the plans of the other one and they "
                    + "cannot join mine. They are not told. Doing it again changes nothing.")
    @ApiResponse(responseCode = "204", description = "Blocked")
    @ApiResponse(responseCode = "400", description = "block.self", content = @Content)
    @ApiResponse(responseCode = "404", description = "person.notFound", content = @Content)
    @ApiResponse(responseCode = "503", description = "social.unavailable", content = @Content)
    public void block(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
        blocks.block(KeycloakJwt.caller(jwt), userId);
    }

    @DeleteMapping(BLOCKED_PERSON)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Unblock someone", description = "We can follow each other again, by asking again.")
    @ApiResponse(responseCode = "204", description = "Not blocked any more")
    public void unblock(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
        blocks.unblock(KeycloakJwt.userId(jwt), userId);
    }

    @GetMapping(BLOCKED)
    @Operation(summary = "The people I blocked", description = "The latest first.")
    @ApiResponse(responseCode = "200", description = "A page of the list")
    @ApiResponse(responseCode = "400", description = "page.invalid", content = @Content)
    public BlockedPageResponse blocked(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                       @RequestParam(required = false) Integer page,
                                       @RequestParam(required = false) Integer size) {
        return BlockedPageResponse.of(blocks.blocked(KeycloakJwt.userId(jwt),
                new PageQuery(page == null ? 0 : page, size == null ? PageQuery.DEFAULT_SIZE : size)));
    }
}
