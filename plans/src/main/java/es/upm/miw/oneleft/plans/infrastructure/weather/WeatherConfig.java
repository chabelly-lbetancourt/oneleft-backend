package es.upm.miw.oneleft.plans.infrastructure.weather;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Weather forecast (HU-026): Open-Meteo client with short timeouts and its Redis cache. A forecast barely changes in
 * half an hour, and the plans start within the next 12 hours.
 */
@Configuration
@EnableCaching
public class WeatherConfig {

    static final String CACHE = "weather";
    private static final Duration TTL = Duration.ofMinutes(30);

    @Bean
    RestClient openMeteo(@Value("${oneleft.weather.url:https://api.open-meteo.com}") String url) {
        var timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(Duration.ofSeconds(2));
        timeouts.setReadTimeout(Duration.ofSeconds(3));
        return RestClient.builder().baseUrl(url).requestFactory(timeouts).build();
    }

    @Bean
    RedisCacheManagerBuilderCustomizer weatherCache() {
        return builder -> builder.withCacheConfiguration(CACHE, RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(TTL).prefixCacheNameWith("oneleft:"));
    }
}
