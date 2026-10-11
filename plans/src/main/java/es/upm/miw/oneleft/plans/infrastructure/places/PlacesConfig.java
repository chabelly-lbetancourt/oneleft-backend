package es.upm.miw.oneleft.plans.infrastructure.places;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Place suggestions (HU-071): client of Photon, the OpenStreetMap geocoder made for search as you type, with short
 * timeouts and a Redis cache. Places barely change, so the same search around the same place is answered for a day
 * without asking again, which also respects the fair use of the public Photon.
 */
@Configuration
public class PlacesConfig {

    static final String CACHE = "places";
    private static final Duration TTL = Duration.ofHours(24);

    @Bean
    RestClient photon(@Value("${oneleft.places.url:https://photon.komoot.io}") String url) {
        var timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(Duration.ofSeconds(2));
        timeouts.setReadTimeout(Duration.ofSeconds(3));
        return RestClient.builder().baseUrl(url).requestFactory(timeouts)
                .defaultHeader(HttpHeaders.USER_AGENT, "OneLeft/1.0 (https://github.com/chabelly-lbetancourt)")
                .build();
    }

    @Bean
    RedisCacheManagerBuilderCustomizer placesCache() {
        return builder -> builder.withCacheConfiguration(CACHE, RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(TTL).prefixCacheNameWith("oneleft:"));
    }
}
