package es.upm.miw.oneleft.gateway.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.time.Duration;
import java.util.List;

/**
 * Rate limit rules. The first rule that matches a request applies; each user (or IP, without a session) has its
 * own bucket per rule.
 *
 * @param enabled false turns rate limiting off (for example, in tests that do not need Redis)
 */
@ConfigurationProperties("oneleft.rate-limit")
public record RateLimitProperties(boolean enabled, List<Rule> rules) {

    public RateLimitProperties {
        rules = rules == null ? List.of() : List.copyOf(rules);
    }

    /**
     * @param method HTTP method, or null for any
     * @param path   path pattern, for example {@code /api/**}
     */
    public record Rule(String name, String method, String path, long capacity, Duration period) {

        private static final PathPatternParser PARSER = new PathPatternParser();

        public Rule {
            if (name == null || path == null || capacity < 1 || period == null || period.isZero()) {
                throw new IllegalArgumentException("Invalid rate limit rule " + name);
            }
        }

        boolean matches(String requestMethod, String requestPath) {
            return (method == null || method.equalsIgnoreCase(requestMethod))
                    && pattern().matches(PathContainer.parsePath(requestPath));
        }

        private PathPattern pattern() {
            return PARSER.parse(path);
        }
    }
}
