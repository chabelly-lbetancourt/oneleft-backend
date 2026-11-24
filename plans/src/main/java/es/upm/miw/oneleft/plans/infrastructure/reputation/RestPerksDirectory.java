package es.upm.miw.oneleft.plans.infrastructure.reputation;

import es.upm.miw.oneleft.plans.domain.model.ReputationPerks;
import es.upm.miw.oneleft.plans.domain.port.out.PerksDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.UUID;

/**
 * Output adapter: asks the reputation service what the level of the person is worth (HU-063), with their own token: a
 * person can only be asked about themselves. A perk is an extra, so when it cannot be known (no token, as in the seed,
 * or the service does not answer) the person has what everyone has and nothing fails.
 */
@Component
class RestPerksDirectory implements PerksDirectory {

    private static final Logger log = LoggerFactory.getLogger(RestPerksDirectory.class);

    private final RestClient reputation;

    RestPerksDirectory(RestClient reputationService) {
        this.reputation = reputationService;
    }

    @Override
    public ReputationPerks perksOf(UUID userId) {
        var token = currentToken();
        if (token.isEmpty()) {
            return ReputationPerks.BASE;
        }
        try {
            var perks = reputation.get().uri("/api/v1/reputation/me/perks")
                    .headers(headers -> headers.setBearerAuth(token.get())).retrieve().body(Perks.class);
            return perks == null ? ReputationPerks.BASE
                    : new ReputationPerks(perks.waitlistPriority(), perks.maxActivePlans());
        } catch (RestClientException error) {
            log.warn("The reputation service did not say the perks of {}: they have the ones of everyone ({})",
                    userId, error.getMessage());
            return ReputationPerks.BASE;
        }
    }

    private static Optional<String> currentToken() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken authentication) {
            return Optional.of(authentication.getToken().getTokenValue());
        }
        return Optional.empty();
    }

    /** What the reputation service answers: the part this service reads. */
    record Perks(int waitlistPriority, int maxActivePlans) {
    }
}
