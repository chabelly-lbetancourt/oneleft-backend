package es.upm.miw.oneleft.gateway.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** The rules of application.yaml: which one applies to each request, with the default values of each environment. */
class RateLimitRulesTest {

    private static RateLimitProperties configured() throws IOException {
        var environment = new StandardEnvironment();
        new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yaml"))
                .forEach(source -> environment.getPropertySources().addLast(source));
        return Binder.get(environment).bind("oneleft.rate-limit", RateLimitProperties.class).get();
    }

    private static RateLimitProperties.Rule ruleFor(RateLimitProperties properties, String method, String path) {
        return properties.rules().stream().filter(rule -> rule.matches(method, path)).findFirst().orElseThrow();
    }

    @Test
    void placeSuggestionsHaveTheirOwnLimitBeforeTheGeneralOne() throws IOException {
        var properties = configured();

        var places = ruleFor(properties, "GET", "/api/v1/places");
        assertThat(places.name()).isEqualTo("places");
        assertThat(places.capacity()).isEqualTo(30);
        assertThat(places.period()).isEqualTo(Duration.ofMinutes(1));
        assertThat(ruleFor(properties, "POST", "/api/v1/plans").name()).isEqualTo("publish-plans");
        assertThat(ruleFor(properties, "GET", "/api/v1/plans/nearby").name()).isEqualTo("api");
    }
}
