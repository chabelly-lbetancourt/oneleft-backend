package es.upm.miw.oneleft.plans.infrastructure.social;

import es.upm.miw.oneleft.plans.domain.model.PersonActivityException;
import es.upm.miw.oneleft.plans.domain.port.out.ActivityAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

/**
 * Output adapter: asks {@code GET /api/v1/social/people/{id}} of the social service with the token of whoever is
 * asking, so that the social service sees the same as they do and says whether their plans may be seen.
 */
@Component
public class RestActivityAccess implements ActivityAccess {

    private static final Logger log = LoggerFactory.getLogger(RestActivityAccess.class);

    private final RestClient social;

    public RestActivityAccess(RestClient socialService) {
        this.social = socialService;
    }

    private static String currentToken() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken authentication) {
            return authentication.getToken().getTokenValue();
        }
        throw new IllegalStateException("The social service is only asked with a Keycloak token");
    }

    @Override
    public boolean visible(UUID userId) {
        try {
            var person = social.get().uri("/api/v1/social/people/{id}", userId)
                    .headers(headers -> headers.setBearerAuth(currentToken()))
                    .retrieve()
                    .body(PersonView.class);
            if (person == null) {
                throw new RestClientException("Empty answer");
            }
            return person.activityVisible();
        } catch (HttpClientErrorException.NotFound error) {
            throw new PersonActivityException(PersonActivityException.Reason.NOT_FOUND, "Nobody has that account");
        } catch (RestClientException error) {
            log.warn("The social service did not say whether the plans of {} may be seen: {}", userId,
                    error.toString());
            throw new PersonActivityException(PersonActivityException.Reason.UNAVAILABLE,
                    "The plans of someone cannot be shown right now");
        }
    }

    /** The part of the person that the plans service needs. */
    record PersonView(UUID userId, boolean activityVisible) {
    }
}
