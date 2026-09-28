package es.upm.miw.oneleft.gateway.ratelimit;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.GenericContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rate limiting with a real Redis. The routed services are not running, so allowed requests end in a proxy error;
 * what matters here is whether the gateway answers 429 before proxying.
 */
@SpringBootTest(properties = {
        "oneleft.rate-limit.enabled=true",
        "oneleft.rate-limit.rules[0].name=publish-plans",
        "oneleft.rate-limit.rules[0].method=POST",
        "oneleft.rate-limit.rules[0].path=/api/v1/plans",
        "oneleft.rate-limit.rules[0].capacity=2",
        "oneleft.rate-limit.rules[0].period=PT1H",
        "oneleft.rate-limit.rules[1].name=api",
        "oneleft.rate-limit.rules[1].path=/api/**",
        "oneleft.rate-limit.rules[1].capacity=3",
        "oneleft.rate-limit.rules[1].period=PT1M"})
@AutoConfigureMockMvc
@Import(RateLimitTest.Redis.class)
class RateLimitTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class Redis {
        @Bean
        @ServiceConnection(name = "redis")
        GenericContainer<?> redis() {
            return new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry meters;

    private static RequestPostProcessor user(String subject) {
        return jwt().jwt(token -> token.subject(subject));
    }

    @Test
    void publishingIsLimitedPerUserWithATranslatableProblem() throws Exception {
        var ana = UUID.randomUUID().toString();
        for (var i = 0; i < 2; i++) {
            var status = mockMvc.perform(post("/api/v1/plans").with(user(ana))).andReturn().getResponse().getStatus();
            assertThat(status).isNotEqualTo(429);
        }

        mockMvc.perform(post("/api/v1/plans").with(user(ana)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(header().string(RateLimitFilter.REMAINING, "0"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.code").value(RateLimitFilter.CODE))
                .andExpect(jsonPath("$.instance").value("/api/v1/plans"))
                .andExpect(jsonPath("$.retryAfterSeconds").isNumber());

        // Another person has their own bucket, and reading is a different rule
        assertThat(mockMvc.perform(post("/api/v1/plans").with(user(UUID.randomUUID().toString())))
                .andReturn().getResponse().getStatus()).isNotEqualTo(429);
        assertThat(mockMvc.perform(get("/api/v1/plans/mine").with(user(ana)))
                .andReturn().getResponse().getStatus()).isNotEqualTo(429);
        assertThat(meters.counter("oneleft.gateway.ratelimit.rejected", "rule", "publish-plans").count())
                .isGreaterThanOrEqualTo(1);
    }

    @Test
    void theRestOfTheApiHasAGeneralLimitAndCountsTheRemainingRequests() throws Exception {
        var diego = UUID.randomUUID().toString();
        mockMvc.perform(get("/api/v1/users/me").with(user(diego)))
                .andExpect(header().string(RateLimitFilter.REMAINING, "2"));
        mockMvc.perform(get("/api/v1/users/me").with(user(diego)));
        mockMvc.perform(get("/api/v1/users/me").with(user(diego)));
        mockMvc.perform(get("/api/v1/users/me").with(user(diego))).andExpect(status().isTooManyRequests());
    }

    @Test
    void withoutASessionTheLimitIsPerIpAndDocumentationIsNotLimited() throws Exception {
        for (var i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/v1/users/me").with(request -> {
                request.setRemoteAddr("203.0.113.7");
                return request;
            })).andExpect(status().isUnauthorized());
        }
        mockMvc.perform(get("/api/v1/users/me").with(request -> {
            request.setRemoteAddr("203.0.113.7");
            return request;
        })).andExpect(status().isTooManyRequests());
        for (var i = 0; i < 5; i++) {
            mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        }
    }
}
