package es.upm.miw.oneleft.users.infrastructure.services;

import es.upm.miw.oneleft.users.domain.model.AccountException;
import es.upm.miw.oneleft.users.domain.port.out.ServicesData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Output adapter: asks every other service for what it keeps of the person (HU-045), at its internal address and
 * with the token of whoever is asking, so that each one gives only what is theirs. If one does not answer, there is no
 * file: an incomplete copy would look complete.
 */
@Component
public class ServicesRestData implements ServicesData {

    /** The path of {@code GET me/data} of each service, by the name it has in the file */
    private static final Map<String, String> PATHS = Map.of(
            "plans", "/api/v1/plans/me/data",
            "social", "/api/v1/social/me/data",
            "reputation", "/api/v1/reputation/me/data",
            "notifications", "/api/v1/notifications/me/data",
            "chat", "/api/v1/chats/me/data");

    private final Map<String, RestClient> clients = new LinkedHashMap<>();

    /** For the tests: each service with its own client */
    ServicesRestData(Map<String, RestClient> clients) {
        this.clients.putAll(clients);
    }

    @Autowired
    public ServicesRestData(@Value("${oneleft.services.plans}") String plans,
                            @Value("${oneleft.services.social}") String social,
                            @Value("${oneleft.services.reputation}") String reputation,
                            @Value("${oneleft.services.notifications}") String notifications,
                            @Value("${oneleft.services.chat}") String chat) {
        Map.of("plans", plans, "social", social, "reputation", reputation, "notifications", notifications,
                "chat", chat).forEach((name, url) -> clients.put(name, client(url)));
    }

    private static RestClient client(String url) {
        var timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(Duration.ofSeconds(2));
        timeouts.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().baseUrl(url).requestFactory(timeouts).build();
    }

    @Override
    public Map<String, Object> of(UUID userId) {
        var token = currentToken();
        var data = new LinkedHashMap<String, Object>();
        for (var name : new String[] {"plans", "social", "reputation", "notifications", "chat"}) {
            try {
                data.put(name, clients.get(name).get().uri(PATHS.get(name))
                        .headers(headers -> headers.setBearerAuth(token))
                        .retrieve().body(new ParameterizedTypeReference<Map<String, Object>>() {
                        }));
            } catch (RestClientException error) {
                throw new AccountException(AccountException.Reason.DATA_UNAVAILABLE,
                        "The data of " + name + " cannot be given right now", error);
            }
        }
        return data;
    }

    private static String currentToken() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken authentication) {
            return authentication.getToken().getTokenValue();
        }
        throw new IllegalStateException("The other services are only asked with a Keycloak token");
    }
}
