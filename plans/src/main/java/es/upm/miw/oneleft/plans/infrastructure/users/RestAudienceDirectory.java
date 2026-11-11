package es.upm.miw.oneleft.plans.infrastructure.users;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.AudienceMember;
import es.upm.miw.oneleft.plans.domain.model.AudienceUnavailableException;
import es.upm.miw.oneleft.plans.domain.model.Level;
import es.upm.miw.oneleft.plans.domain.model.PersonGender;
import es.upm.miw.oneleft.plans.domain.port.out.AudienceDirectory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Output adapter: asks the users service for the gender and age of the person (HU-070), with their own token: a person
 * can only be asked about themselves, and the date of birth never travels. A person who has not set them gives 404.
 */
@Component
class RestAudienceDirectory implements AudienceDirectory {

    private final RestClient users;

    RestAudienceDirectory(RestClient usersService) {
        this.users = usersService;
    }

    @Override
    public Optional<AudienceMember> memberOf(UUID userId) {
        try {
            var audience = users.get().uri("/api/v1/users/me/demographics/audience")
                    .headers(headers -> headers.setBearerAuth(currentToken())).retrieve().body(Audience.class);
            if (audience == null || audience.gender() == null) {
                throw new RestClientException("Empty answer");
            }
            return Optional.of(new AudienceMember(audience.gender(), audience.age(), audience.ageBand(), levelsOf(audience.levels())));
        } catch (HttpClientErrorException error) {
            if (error.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw new AudienceUnavailableException("The users service refused the question", error);
        } catch (RestClientException error) {
            throw new AudienceUnavailableException("The users service did not answer", error);
        }
    }

    /** The levels of the profile by activity; what this service does not know is left out. */
    private static Map<Activity, Level> levelsOf(Map<String, String> levels) {
        var known = new EnumMap<Activity, Level>(Activity.class);
        if (levels != null) {
            levels.forEach((activity, level) -> {
                try {
                    known.put(Activity.valueOf(activity), Level.valueOf(level));
                } catch (IllegalArgumentException unknown) {
                    // an activity or level of a newer users service: not needed here
                }
            });
        }
        return known;
    }

    private static String currentToken() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken authentication) {
            return authentication.getToken().getTokenValue();
        }
        throw new AudienceUnavailableException("The users service is only asked with a Keycloak token", null);
    }

    /** What the users service answers: gender, age in years and age band. */
    record Audience(PersonGender gender, int age, String ageBand, Map<String, String> levels) {
    }
}
