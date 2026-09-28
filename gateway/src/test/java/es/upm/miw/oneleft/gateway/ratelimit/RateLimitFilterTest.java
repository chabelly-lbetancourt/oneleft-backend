package es.upm.miw.oneleft.gateway.ratelimit;

import es.upm.miw.oneleft.gateway.ratelimit.RateLimitProperties.Rule;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitFilterTest {

    private final Rule api = new Rule("api", null, "/api/**", 10, Duration.ofMinutes(1));

    @Test
    @SuppressWarnings("unchecked")
    void failsOpenWhenRedisIsNotAvailable() throws Exception {
        ProxyManager<String> buckets = mock(ProxyManager.class);
        when(buckets.builder()).thenThrow(new IllegalStateException("Redis is down"));
        var meters = new SimpleMeterRegistry();
        var chain = new MockFilterChain();

        new RateLimitFilter(List.of(api), buckets, meters)
                .doFilter(new MockHttpServletRequest("GET", "/api/v1/plans/mine"), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(meters.counter("oneleft.gateway.ratelimit.unavailable").count()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void requestsOutsideTheRulesAreNotLimited() throws Exception {
        ProxyManager<String> buckets = mock(ProxyManager.class);
        var chain = new MockFilterChain();

        new RateLimitFilter(List.of(api), buckets, new SimpleMeterRegistry())
                .doFilter(new MockHttpServletRequest("GET", "/swagger-ui.html"), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void rulesMatchMethodAndPathAndRejectInvalidValues() {
        var publish = new Rule("publish", "POST", "/api/v1/plans", 5, Duration.ofHours(1));
        assertThat(publish.matches("POST", "/api/v1/plans")).isTrue();
        assertThat(publish.matches("post", "/api/v1/plans")).isTrue();
        assertThat(publish.matches("GET", "/api/v1/plans")).isFalse();
        assertThat(publish.matches("POST", "/api/v1/plans/x/participants")).isFalse();
        assertThat(api.matches("DELETE", "/api/v1/users/me/profile")).isTrue();
        assertThatThrownBy(() -> new Rule("bad", null, "/api/**", 0, Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Rule("bad", null, null, 1, Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new RateLimitProperties(true, null).rules()).isEmpty();
    }
}
