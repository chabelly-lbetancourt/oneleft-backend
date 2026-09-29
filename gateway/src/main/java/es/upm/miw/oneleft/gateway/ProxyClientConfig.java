package es.upm.miw.oneleft.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.HttpComponentsClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.autoconfigure.ClientHttpRequestFactoryBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Connection pool of the HTTP client the gateway uses to forward requests to the services (Apache HttpClient, #90).
 * <p>
 * By default it allows only 5 connections per service. Real-time streams (Server-Sent Events) keep their connection
 * to the plans service open while the app is open, so with 5 people using the app every other request to plans
 * waited for a free connection (#93). The pool is sized for the streams instead.
 */
@Configuration
public class ProxyClientConfig {

    @Bean
    ClientHttpRequestFactoryBuilderCustomizer<HttpComponentsClientHttpRequestFactoryBuilder> proxyConnectionPool(
            @Value("${oneleft.proxy.max-connections}") int maxConnections) {
        return builder -> builder.withConnectionManagerCustomizer(manager -> manager
                .setMaxConnTotal(maxConnections)
                // Only a few services behind the gateway: any of them may use the whole pool
                .setMaxConnPerRoute(maxConnections));
    }
}
