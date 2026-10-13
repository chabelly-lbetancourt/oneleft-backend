package es.upm.miw.oneleft.chat.infrastructure.plans;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** The plans service, inside the network of the services: short timeouts, the chat waits for nobody. */
@Configuration
public class PlansConfig {

    @Bean
    RestClient plansService(@Value("${oneleft.plans.url}") String url) {
        var timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(Duration.ofSeconds(2));
        timeouts.setReadTimeout(Duration.ofSeconds(3));
        return RestClient.builder().baseUrl(url).requestFactory(timeouts).build();
    }
}
