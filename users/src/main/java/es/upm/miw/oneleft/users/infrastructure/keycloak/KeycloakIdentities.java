package es.upm.miw.oneleft.users.infrastructure.keycloak;

import es.upm.miw.oneleft.users.domain.model.AccountException;
import es.upm.miw.oneleft.users.domain.port.out.Identities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.UUID;

/**
 * Output adapter: deletes the account in Keycloak (HU-045) with the admin API. The service has a client of its own
 * ({@code oneleft-accounts}, whose service account may only manage users), and asks for a token each time: deleting
 * an account is rare.
 */
@Component
public class KeycloakIdentities implements Identities {

    private static final Logger log = LoggerFactory.getLogger(KeycloakIdentities.class);

    private final RestClient keycloak;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    public KeycloakIdentities(RestClient keycloakAdmin, @Value("${oneleft.accounts.realm}") String realm,
                              @Value("${oneleft.accounts.client-id}") String clientId,
                              @Value("${oneleft.accounts.client-secret}") String clientSecret) {
        this.keycloak = keycloakAdmin;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public void delete(UUID userId) {
        try {
            keycloak.delete().uri("/admin/realms/{realm}/users/{id}", realm, userId)
                    .headers(headers -> headers.setBearerAuth(token()))
                    .retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound gone) {
            log.info("The account {} was already gone from Keycloak", userId);
        } catch (RestClientException error) {
            throw new AccountException(AccountException.Reason.IDENTITY_UNAVAILABLE,
                    "The account could not be deleted right now", error);
        }
    }

    private String token() {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        var answer = keycloak.post().uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
                .retrieve().body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                });
        if (answer == null || !(answer.get("access_token") instanceof String token)) {
            throw new RestClientException("Keycloak gave no token");
        }
        return token;
    }
}
