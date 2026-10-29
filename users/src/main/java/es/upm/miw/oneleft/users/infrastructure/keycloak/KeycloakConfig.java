package es.upm.miw.oneleft.users.infrastructure.keycloak;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** Keycloak inside the network of the services (HU-045): short timeouts. */
@Configuration
public class KeycloakConfig {

    @Bean
    RestClient keycloakAdmin(@Value("${oneleft.accounts.keycloak-url}") String url) {
        var timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(Duration.ofSeconds(2));
        timeouts.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().baseUrl(url).requestFactory(timeouts).build();
    }
}
