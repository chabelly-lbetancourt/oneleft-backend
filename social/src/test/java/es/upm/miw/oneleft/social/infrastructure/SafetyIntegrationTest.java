package es.upm.miw.oneleft.social.infrastructure;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import es.upm.miw.oneleft.social.TestcontainersConfiguration;
import es.upm.miw.oneleft.social.infrastructure.messaging.RabbitConfig;
import es.upm.miw.oneleft.social.infrastructure.rest.BlockController;
import es.upm.miw.oneleft.social.infrastructure.rest.FollowController;
import es.upm.miw.oneleft.social.infrastructure.rest.ReportController;
import es.upm.miw.oneleft.social.infrastructure.rest.SocialController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-033 end to end inside the service: the REST API, a real PostgreSQL and the events on RabbitMQ, read from a
 * temporary queue of the test. The people, blocks and reports created are deleted (the blocks and reports go with
 * their people), and so is the queue.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SafetyIntegrationTest {

    @RegisterExtension
    static final WireMockExtension USERS = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()).build();

    private static final String ME = SocialController.SOCIAL + SocialController.ME;
    private static final String PERSON = SocialController.SOCIAL + SocialController.PERSON;
    private static final String EVENTS = "test.social-safety";

    @DynamicPropertySource
    static void usersService(DynamicPropertyRegistry registry) {
        registry.add("oneleft.users.url", USERS::baseUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AmqpAdmin admin;

    @Autowired
    private RabbitTemplate rabbit;

    private final UUID ana = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();

    private static RequestPostProcessor user(UUID id, String name) {
        return jwt().jwt(token -> token.subject(id.toString()).claim("name", name));
    }

    private ResultActions as(UUID id, String name, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(user(id, name)));
    }

    private void usersKnows(UUID id, String name) {
        USERS.stubFor(WireMock.get(urlPathEqualTo("/api/v1/users/" + id + "/profile")).willReturn(okJson("""
                {"userId": "%s", "displayName": "%s", "zoneName": "Vallecas", "hobbies": []}""".formatted(id, name))));
    }

    /** The next event on the queue of the test: its routing key and its body. */
    private String nextEvent() {
        Message message = rabbit.receive(EVENTS, 5000);
        assertThat(message).as("an event on " + EVENTS).isNotNull();
        return message.getMessageProperties().getReceivedRoutingKey() + " "
                + new String(message.getBody(), StandardCharsets.UTF_8);
    }

    @BeforeEach
    void listenToTheEvents() {
        USERS.resetAll();
        var social = new TopicExchange(RabbitConfig.SOCIAL_EXCHANGE, true, false);
        var reports = new TopicExchange(RabbitConfig.REPORTS_EXCHANGE, true, false);
        admin.declareExchange(social);
        admin.declareExchange(reports);
        // Durable and shared (RabbitMQ 4 has no transient shared queues): it lasts the whole test, and is deleted
        // after it
        admin.declareQueue(new Queue(EVENTS, true, false, false));
        admin.declareBinding(new Binding(EVENTS, Binding.DestinationType.QUEUE, social.getName(), "user.*", null));
        admin.declareBinding(new Binding(EVENTS, Binding.DestinationType.QUEUE, reports.getName(),
                RabbitConfig.REPORT_CREATED, null));
    }

    /** The test leaves no person, block, report or queue behind. */
    @AfterEach
    void deleteWhatWasCreated() {
        jdbc.update("DELETE FROM person WHERE user_id IN (?, ?)", ana, lucia);
        admin.deleteQueue(EVENTS);
    }

    @Test
    void aBlockIsPrivateAndAnnouncedToTheOtherServices() throws Exception {
        usersKnows(ana, "Ana");
        as(lucia, "Lucía", put(ME + FollowController.FOLLOWED, ana)).andExpect(status().isOk());

        as(lucia, "Lucía", put(ME + BlockController.BLOCKED_PERSON, ana)).andExpect(status().isNoContent());

        assertThat(nextEvent()).startsWith(RabbitConfig.USER_BLOCKED).contains(lucia.toString(), ana.toString());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM follow WHERE ? IN (follower_id, followee_id)",
                Long.class, lucia)).isZero();
        as(lucia, "Lucía", get(ME + BlockController.BLOCKED))
                .andExpect(jsonPath("$.items[0].userId").value(ana.toString()))
                .andExpect(jsonPath("$.items[0].name").value("Ana"))
                .andExpect(jsonPath("$.total").value(1));
        as(lucia, "Lucía", get(PERSON, ana)).andExpect(jsonPath("$.relation").value("BLOCKED"))
                .andExpect(jsonPath("$.activityVisible").value(false));
        // For Ana, Lucía has no account; following her is refused the same way
        as(ana, "Ana", get(PERSON, lucia)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("person.notFound"));
        as(ana, "Ana", put(ME + FollowController.FOLLOWED, lucia)).andExpect(status().isNotFound());
        as(lucia, "Lucía", put(ME + FollowController.FOLLOWED, ana)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("social.blocked"));

        as(lucia, "Lucía", delete(ME + BlockController.BLOCKED_PERSON, ana)).andExpect(status().isNoContent());

        assertThat(nextEvent()).startsWith(RabbitConfig.USER_UNBLOCKED).contains(lucia.toString(), ana.toString());
        as(ana, "Ana", get(PERSON, lucia)).andExpect(status().isOk()).andExpect(jsonPath("$.relation").value("NONE"));
        as(lucia, "Lucía", put(ME + BlockController.BLOCKED_PERSON, lucia)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("block.self"));
    }

    @Test
    void theReportsWaitForTheModerationAndOnlyAdminsSeeThem() throws Exception {
        usersKnows(ana, "Ana");
        var body = """
                {"target": "PERSON", "targetId": "%s", "reason": "HARASSMENT", "comment": "Insults in the chat"}"""
                .formatted(ana);

        var created = as(lucia, "Lucía", post(ReportController.REPORTS).contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        var id = created.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        assertThat(nextEvent()).startsWith(RabbitConfig.REPORT_CREATED).contains(id, ana.toString(), "HARASSMENT");
        // Reporting it again gives the same report back
        as(lucia, "Lucía", post(ReportController.REPORTS).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.id").value(id));
        as(lucia, "Lucía", get(ReportController.REPORTS)).andExpect(status().isForbidden());
        mockMvc.perform(get(ReportController.REPORTS).param("size", "50")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", hasItem(id)));
        as(lucia, "Lucía", post(ReportController.REPORTS).contentType(MediaType.APPLICATION_JSON).content("""
                {"target": "PERSON", "targetId": "%s", "reason": "OTHER"}""".formatted(ana)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("report.comment"));
    }
}
