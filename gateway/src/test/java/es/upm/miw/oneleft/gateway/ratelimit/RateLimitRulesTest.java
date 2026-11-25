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

    /** HU-034: the public reading of a trusted link is stricter than the general limit, which would apply first. */
    @Test
    void trustedLinksHaveTheirOwnLimitBeforeTheGeneralOne() throws IOException {
        var properties = configured();

        var trusted = ruleFor(properties, "GET", "/api/v1/public/plans/trusted-links/some-token");
        assertThat(trusted.name()).isEqualTo("trusted-links");
        assertThat(trusted.capacity()).isEqualTo(30);
        assertThat(trusted.period()).isEqualTo(Duration.ofMinutes(1));
        // The shared plans of HU-024 and the links I manage with my session keep the general limit
        assertThat(ruleFor(properties, "GET", "/api/v1/public/plans/0b0f0c0e-0000-4000-8000-000000000000").name())
                .isEqualTo("api");
        assertThat(ruleFor(properties, "GET", "/api/v1/plans/0b0f0c0e-0000-4000-8000-000000000000/trusted-links")
                .name()).isEqualTo("api");
    }
}
