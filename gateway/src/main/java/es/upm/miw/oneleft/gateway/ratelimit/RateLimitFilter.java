package es.upm.miw.oneleft.gateway.ratelimit;

import es.upm.miw.oneleft.gateway.ratelimit.RateLimitProperties.Rule;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Applies the rate limit rules after authentication, so each signed-in user has their own buckets; requests without
 * a token are limited by IP. Rejected requests get 429 as Problem Details with a translatable {@code code}.
 *
 * <p>Fail-open: if Redis is not available the request goes through (and a metric records it). A broken limiter must
 * not take the API down.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    public static final String CODE = "rate.limited";
    static final String REMAINING = "X-RateLimit-Remaining";

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final List<Rule> rules;
    private final ProxyManager<String> buckets;
    private final MeterRegistry meters;
    private final JsonMapper json = JsonMapper.builder().build();

    public RateLimitFilter(List<Rule> rules, ProxyManager<String> buckets, MeterRegistry meters) {
        this.rules = List.copyOf(rules);
        this.buckets = buckets;
        this.meters = meters;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var rule = rules.stream().filter(r -> r.matches(request.getMethod(), request.getRequestURI())).findFirst();
        if (rule.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        var limit = rule.get();
        long remaining;
        long waitNanos;
        try {
            var bucket = buckets.builder().build(limit.name() + ":" + identity(request), () -> configuration(limit));
            var probe = bucket.tryConsumeAndReturnRemaining(1);
            remaining = probe.getRemainingTokens();
            waitNanos = probe.isConsumed() ? 0 : probe.getNanosToWaitForRefill();
        } catch (RuntimeException unavailable) {
            log.warn("Rate limiting skipped, the bucket store is not available: {}", unavailable.getMessage());
            meters.counter("oneleft.gateway.ratelimit.unavailable").increment();
            chain.doFilter(request, response);
            return;
        }
        response.setHeader(REMAINING, Long.toString(remaining));
        if (waitNanos == 0) {
            chain.doFilter(request, response);
            return;
        }
        meters.counter("oneleft.gateway.ratelimit.rejected", "rule", limit.name()).increment();
        reject(request, response, Math.max(1, TimeUnit.NANOSECONDS.toSeconds(waitNanos) + 1));
    }

    private static BucketConfiguration configuration(Rule rule) {
        return BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(rule.capacity()).refillGreedy(rule.capacity(), rule.period()))
                .build();
    }

    /** The signed-in user (Keycloak subject) or, without a session, the client IP. */
    private static String identity(HttpServletRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof JwtAuthenticationToken token
                ? "user:" + token.getName()
                : "ip:" + request.getRemoteAddr();
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, long retryAfterSeconds)
            throws IOException {
        // Same shape as the Problem Details of the services (RFC 9457 with a top-level code)
        var problem = new LinkedHashMap<String, Object>();
        problem.put("type", "about:blank");
        problem.put("title", HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
        problem.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        problem.put("detail", "Too many requests, try again in " + retryAfterSeconds + " seconds");
        problem.put("instance", request.getRequestURI());
        problem.put("code", CODE);
        problem.put("retryAfterSeconds", retryAfterSeconds);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        json.writeValue(response.getOutputStream(), problem);
    }
}
