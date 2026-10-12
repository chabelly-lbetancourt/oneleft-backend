package es.upm.miw.oneleft.gateway.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-008: the chat WebSocket goes through the same security as the API. Without a valid token in its subprotocol the
 * handshake is rejected before reaching the relay; with it, the request gets to the WebSocket handler (which, without
 * the upgrade headers of a real browser, answers 400).
 */
@SpringBootTest(properties = "oneleft.rate-limit.enabled=false")
@AutoConfigureMockMvc
@Import(ChatRelaySecurityTest.Tokens.class)
class ChatRelaySecurityTest {

    private static final String VALID = "valid.token.value";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void withoutATokenTheChatIsNotOpened() throws Exception {
        mockMvc.perform(get(ChatRelayConfig.PATH).param("planId", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(ChatRelayConfig.PATH).header("Sec-WebSocket-Protocol", "oneleft.bearer, wrong.token.x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void withTheTokenInTheSubprotocolTheRequestReachesTheWebSocket() throws Exception {
        var result = mockMvc.perform(get(ChatRelayConfig.PATH).param("planId", UUID.randomUUID().toString())
                .header("Sec-WebSocket-Protocol", "oneleft.bearer, " + VALID)).andReturn();

        assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
        // Only the handshake (GET) is open
        mockMvc.perform(post(ChatRelayConfig.PATH).header("Sec-WebSocket-Protocol", "oneleft.bearer, " + VALID))
                .andExpect(status().isForbidden());
    }

    /** One valid token instead of Keycloak. */
    @TestConfiguration(proxyBeanMethods = false)
    static class Tokens {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                if (!VALID.equals(token)) {
                    throw new BadJwtException("Invalid token");
                }
                return Jwt.withTokenValue(token).header("alg", "none").subject(UUID.randomUUID().toString())
                        .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
            };
        }
    }
}
