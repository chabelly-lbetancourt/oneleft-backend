package es.upm.miw.oneleft.social.infrastructure.users;

import es.upm.miw.oneleft.social.domain.model.SocialException;
import es.upm.miw.oneleft.social.domain.port.out.Directory;
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
 * Output adapter: the display name of someone from {@code GET /api/v1/users/{id}/profile} of the users service. The
 * request carries the token of whoever follows them: the users service sees the same as they do.
 */
@Component
public class RestDirectory implements Directory {

    private static final Logger log = LoggerFactory.getLogger(RestDirectory.class);

    private final RestClient users;

    public RestDirectory(RestClient usersService) {
        this.users = usersService;
    }

    private static String currentToken() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken authentication) {
            return authentication.getToken().getTokenValue();
        }
        throw new IllegalStateException("The users service is only asked with a Keycloak token");
    }

    @Override
    public String nameOf(UUID userId) {
        try {
            var profile = users.get().uri("/api/v1/users/{id}/profile", userId)
                    .headers(headers -> headers.setBearerAuth(currentToken()))
                    .retrieve()
                    .body(ProfileView.class);
            if (profile == null || profile.displayName() == null) {
                throw new RestClientException("Empty answer");
            }
            return profile.displayName();
        } catch (HttpClientErrorException.NotFound error) {
            throw new SocialException(SocialException.Reason.PERSON_NOT_FOUND, "Nobody has that account");
        } catch (RestClientException error) {
            log.warn("The users service did not say who {} is: {}", userId, error.toString());
            throw new SocialException(SocialException.Reason.UNAVAILABLE, "People cannot be followed right now");
        }
    }

    /** The part of the public profile that the social service needs. */
    record ProfileView(UUID userId, String displayName) {
    }
}
