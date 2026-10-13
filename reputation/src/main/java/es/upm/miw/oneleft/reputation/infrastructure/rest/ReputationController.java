package es.upm.miw.oneleft.reputation.infrastructure.rest;

import es.upm.miw.oneleft.reputation.domain.model.PageQuery;
import es.upm.miw.oneleft.reputation.domain.port.in.RebuildScoresUseCase;
import es.upm.miw.oneleft.reputation.domain.port.in.ReputationQueryUseCase;
import es.upm.miw.oneleft.reputation.infrastructure.config.KeycloakJwt;
import es.upm.miw.oneleft.reputation.infrastructure.rest.ReputationDtos.LedgerResponse;
import es.upm.miw.oneleft.reputation.infrastructure.rest.ReputationDtos.RebuildResponse;
import es.upm.miw.oneleft.reputation.infrastructure.rest.ReputationDtos.ReputationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/** My reputation and its ledger, and the reputation of others (HU-009). */
@RestController
@RequestMapping(ReputationController.REPUTATION)
@Tag(name = "Reputation", description = "The score of each person and the ledger that explains it")
public class ReputationController {

    public static final String REPUTATION = "/api/v1/reputation";
    public static final String ME = "/me";
    public static final String MY_LEDGER = ME + "/ledger";
    public static final String PERSON = "/people/{userId}";
    public static final String SCORES = "/scores";
    public static final String REBUILD = SCORES + "/rebuild";

    private final ReputationQueryUseCase reputation;
    private final RebuildScoresUseCase rebuild;

    public ReputationController(ReputationQueryUseCase reputation, RebuildScoresUseCase rebuild) {
        this.reputation = reputation;
        this.rebuild = rebuild;
    }

    @GetMapping(ME)
    @Operation(summary = "My reputation")
    @ApiResponse(responseCode = "200", description = "My score")
    public ReputationResponse mine(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ReputationResponse.of(reputation.reputationOf(KeycloakJwt.userId(jwt)));
    }

    @GetMapping(MY_LEDGER)
    @Operation(summary = "Why my reputation went up or down",
            description = "The entries of my ledger, the latest first: reason, points, plan and time.")
    @ApiResponse(responseCode = "200", description = "A page of the ledger")
    @ApiResponse(responseCode = "400", description = "page.invalid", content = @Content)
    public LedgerResponse myLedger(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                   @RequestParam(required = false) Integer page,
                                   @RequestParam(required = false) Integer size) {
        var query = new PageQuery(page == null ? 0 : page, size == null ? PageQuery.DEFAULT_SIZE : size);
        return LedgerResponse.of(reputation.ledgerOf(KeycloakJwt.userId(jwt), query));
    }

    @GetMapping(PERSON)
    @Operation(summary = "The reputation of someone", description = "Their score; their ledger is only theirs.")
    @ApiResponse(responseCode = "200", description = "The score")
    public ReputationResponse of(@PathVariable UUID userId) {
        return ReputationResponse.of(reputation.reputationOf(userId));
    }

    @GetMapping(SCORES)
    @Operation(summary = "The reputation of several people", description = "For lists of plans, in the order asked.")
    @ApiResponse(responseCode = "200", description = "The scores")
    @ApiResponse(responseCode = "400", description = "reputation.people: 1 to 50 people", content = @Content)
    public List<ReputationResponse> scores(@RequestParam List<UUID> ids) {
        return reputation.reputationsOf(ids).stream().map(ReputationResponse::of).toList();
    }

    @PostMapping(REBUILD)
    @Operation(summary = "Rebuild every score from the ledger (administrators)",
            description = "The scores are a projection of the ledger: they are computed again as sums of entries.")
    @ApiResponse(responseCode = "200", description = "How many people have a score")
    @ApiResponse(responseCode = "403", description = "Only administrators", content = @Content)
    public RebuildResponse rebuild(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        if (!KeycloakJwt.isAdmin(jwt)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only administrators rebuild the scores");
        }
        return new RebuildResponse(rebuild.rebuildScores());
    }
}
