package es.upm.miw.oneleft.plans.infrastructure.reputation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** The reputation service inside the network of the services (HU-063): short timeouts, a slow answer is as good as none. */
@Configuration
class ReputationConfig {

    @Bean
    RestClient reputationService(@Value("${oneleft.reputation.url}") String url) {
        var timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(Duration.ofSeconds(1));
        timeouts.setReadTimeout(Duration.ofSeconds(2));
        return RestClient.builder().baseUrl(url).requestFactory(timeouts).build();
    }
}
