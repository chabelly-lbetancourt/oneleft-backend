package es.upm.miw.oneleft.plans.infrastructure.social;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.CheckIn;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.Organizer;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.port.out.CheckInRepository;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import es.upm.miw.oneleft.plans.infrastructure.rest.PersonPlansController;
import es.upm.miw.oneleft.plans.infrastructure.rest.PlanController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-048 inside the service: the recent plans of someone from a real PostgreSQL, and the social service, simulated by
 * WireMock, asked with the token of the caller whether they may be seen. The plans created are deleted at the end.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PersonPlansIntegrationTest {

    @RegisterExtension
    static final WireMockExtension SOCIAL = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()).build();

    private static final String RECENT = PlanController.PLANS + PersonPlansController.RECENT;

    @DynamicPropertySource
    static void socialService(DynamicPropertyRegistry registry) {
        registry.add("oneleft.social.url", SOCIAL::baseUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlanRepository plans;

    @Autowired
    private CheckInRepository checkIns;

    @Autowired
    private JdbcTemplate jdbc;

    private final List<UUID> created = new ArrayList<>();
    private final UUID ana = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();
    private final Instant now = Instant.now();

    @BeforeEach
    void social() {
        SOCIAL.resetAll();
    }

    @AfterEach
    void deleteThePlans() {
        created.forEach(id -> jdbc.update("DELETE FROM plan WHERE id = ?", id));
    }

    /** A plan of Ana that Lucía joined, finished some hours ago */
    private Plan finished(String title, int hoursAgo) {
        var publishedAt = Clock.fixed(now.minus(Duration.ofHours(hoursAgo + 2L)), ZoneOffset.UTC);
        var plan = plans.save(Plan.publish(new Organizer(ana, "Ana"), Activity.RUNNING, title, null,
                        new MeetingPoint("Parque", 40.39, -3.62), now.minus(Duration.ofHours(hoursAgo)), 3, null,
                        publishedAt)
                .join(lucia, "Lucía", publishedAt));
        created.add(plan.id());
        jdbc.update("UPDATE plan SET status = 'FINISHED' WHERE id = ?", plan.id());
        return plan;
    }

    private void arrived(Plan plan, UUID who) {
        checkIns.save(new CheckIn(plan.id(), who, who.equals(ana) ? "Ana" : "Lucía", plan.startsAt()));
    }

    private ResultActions recentOf(UUID who, UUID asker) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(RECENT, who)
                .with(jwt().jwt(token -> token.subject(asker.toString()).tokenValue("token-of-" + asker))));
    }

    private void socialSays(UUID who, boolean visible) {
        SOCIAL.stubFor(get(urlPathEqualTo("/api/v1/social/people/" + who)).willReturn(okJson("""
                {"userId": "%s", "activityVisible": %s}""".formatted(who, visible))));
    }

    @Test
    void myLastPlansHeldTheLatestFirstWithoutWhere() throws Exception {
        for (var hoursAgo = 2; hoursAgo <= 7; hoursAgo++) {
            arrived(finished("Rodaje " + hoursAgo, hoursAgo), lucia);
        }
        // Not counted: Lucía did not arrive, or nobody arrived at a plan of Ana
        finished("Sin ir", 1);
        var nobody = finished("Nadie", 1);

        recentOf(lucia, lucia)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].title").value("Rodaje 2"))
                .andExpect(jsonPath("$[4].title").value("Rodaje 6"))
                .andExpect(jsonPath("$[0].role").value("ATTENDEE"))
                .andExpect(jsonPath("$[0].activity").value("RUNNING"))
                .andExpect(jsonPath("$[0].meetingPoint").doesNotExist());
        // Ana organized them all and someone arrived at the six; the one nobody went to does not count
        recentOf(ana, ana)
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].role").value("ORGANIZER"))
                .andExpect(jsonPath("$[*].planId").value(not(hasItem(nobody.id().toString()))));
        SOCIAL.verify(0, getRequestedFor(urlPathEqualTo("/api/v1/social/people/" + lucia)));
    }

    @Test
    void thePlansOfSomeoneElseOnlyIfTheSocialServiceSaysSo() throws Exception {
        arrived(finished("Rodaje", 3), lucia);

        socialSays(lucia, false);
        recentOf(lucia, ana).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("social.private"));
        SOCIAL.verify(getRequestedFor(urlPathEqualTo("/api/v1/social/people/" + lucia))
                .withHeader("Authorization", equalTo("Bearer token-of-" + ana)));

        socialSays(lucia, true);
        recentOf(lucia, ana).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void nobodyOrNoAnswerShowNothing() throws Exception {
        SOCIAL.stubFor(get(urlPathEqualTo("/api/v1/social/people/" + lucia))
                .willReturn(aResponse().withStatus(404)));
        recentOf(lucia, ana).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("person.notFound"));

        SOCIAL.stubFor(get(urlPathEqualTo("/api/v1/social/people/" + lucia))
                .willReturn(aResponse().withStatus(500)));
        recentOf(lucia, ana).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("social.unavailable"));
    }
}
