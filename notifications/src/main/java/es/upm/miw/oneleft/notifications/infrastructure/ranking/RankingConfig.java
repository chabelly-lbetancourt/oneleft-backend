package es.upm.miw.oneleft.notifications.infrastructure.ranking;

import es.upm.miw.oneleft.notifications.domain.model.RankingSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** Settings of the ranking and the client of the ranker service, with short timeouts: a slow model never holds back notices. */
@Configuration
@EnableScheduling
class RankingConfig {

    @Bean
    RankingSettings rankingSettings(RankingProperties properties) {
        return new RankingSettings(properties.byModel(), properties.batchSize(), properties.interval(),
                properties.cutoff());
    }

    @Bean
    RestClient rankerRestClient(RankingProperties properties) {
        var timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(Duration.ofSeconds(1));
        timeouts.setReadTimeout(Duration.ofSeconds(2));
        var builder = RestClient.builder().requestFactory(timeouts);
        return properties.url() == null || properties.url().isBlank() ? builder.build()
                : builder.baseUrl(properties.url()).build();
    }
}
