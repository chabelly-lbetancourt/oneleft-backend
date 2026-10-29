package es.upm.miw.oneleft.users.infrastructure.keycloak;

import es.upm.miw.oneleft.users.domain.model.AccountException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** HU-045: the account is deleted in Keycloak with the client of the service, and a missing one is not an error. */
class KeycloakIdentitiesTest {

    private static final String KEYCLOAK = "http://keycloak:8180";
    private static final String TOKEN = "/realms/oneleft/protocol/openid-connect/token";
    private final UUID userId = UUID.randomUUID();
    private final RestClient.Builder builder = RestClient.builder().baseUrl(KEYCLOAK);
    private final MockRestServiceServer keycloak = MockRestServiceServer.bindTo(builder).build();
    private final KeycloakIdentities identities =
            new KeycloakIdentities(builder.build(), "oneleft", "oneleft-accounts", "the-secret");

    private void givesAToken() {
        keycloak.expect(requestTo(KEYCLOAK + TOKEN)).andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("grant_type=client_credentials"),
                        org.hamcrest.Matchers.containsString("client_id=oneleft-accounts"))))
                .andRespond(withSuccess("{\"access_token\": \"admin-token\"}", MediaType.APPLICATION_JSON));
    }

    @Test
    void deletesTheAccountWithTheTokenOfItsOwnClient() {
        givesAToken();
        keycloak.expect(requestTo(KEYCLOAK + "/admin/realms/oneleft/users/" + userId))
                .andExpect(method(HttpMethod.DELETE)).andExpect(header("Authorization", "Bearer admin-token"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        identities.delete(userId);

        keycloak.verify();
    }

    @Test
    void anAccountThatIsAlreadyGoneIsNotAnError() {
        givesAToken();
        keycloak.expect(requestTo(KEYCLOAK + "/admin/realms/oneleft/users/" + userId))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatCode(() -> identities.delete(userId)).doesNotThrowAnyException();
    }

    @Test
    void keycloakNotAnsweringIsAnAccountErrorSoThatItCanBeTriedAgain() {
        keycloak.expect(requestTo(KEYCLOAK + TOKEN)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> identities.delete(userId)).isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("reason", AccountException.Reason.IDENTITY_UNAVAILABLE);
    }

    @Test
    void aTokenAnswerWithoutTokenIsAnError() {
        keycloak.expect(requestTo(KEYCLOAK + TOKEN)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> identities.delete(userId)).isInstanceOf(AccountException.class);
    }
}
