package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.Organizer;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.TrustedLinkToken;
import es.upm.miw.oneleft.plans.domain.port.in.TrustedLinksUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import es.upm.miw.oneleft.plans.infrastructure.messaging.AccountDeletedListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-034 end to end inside the service, with a real PostgreSQL: who goes to a plan creates, lists and revokes their
 * links with their session, and the contact opens one without any. The plans created are removed at the end, and
 * their links with them.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TrustedLinkIntegrationTest {

    private static final String LINKS = TrustedLinkController.TRUSTED_LINKS;
    private static final String LINK = TrustedLinkController.TRUSTED_LINK;
    private static final String OPEN = TrustedLinkController.PUBLIC_TRUSTED_LINK;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper json;

    @Autowired
    private PlanRepository plans;

    @Autowired
    private TrustedLinksUseCase links;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AccountDeletedListener accountDeleted;

    private final List<UUID> created = new ArrayList<>();
    private final UUID ana = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();
    private final UUID diego = UUID.randomUUID();
    private final UUID marta = UUID.randomUUID();

    private static RequestPostProcessor user(UUID id) {
        return jwt().jwt(token -> token.subject(id.toString()).claim("name", "Test"));
    }

    @AfterEach
    void removePlans() {
        // The links go with the plan (ON DELETE CASCADE)
        created.forEach(id -> jdbc.update("DELETE FROM plan WHERE id = ?", id));
    }

    /** A plan of Ana with Lucía and Diego in it, that starts in that long (negative: it started that long ago) */
    private Plan planStartingIn(Duration startsIn) {
        var then = Clock.fixed(Instant.now().plus(startsIn).minus(Duration.ofMinutes(10)), ZoneOffset.UTC);
        var plan = plans.save(Plan.publish(new Organizer(ana, "Ana"), Activity.PADEL, "Padel 2 vs 2", "Bring water",
                new MeetingPoint("Albufera courts", 40.396417, -3.629712), then.instant().plus(Duration.ofMinutes(10)),
                3, null, then).join(lucia, "Lucía", then).join(diego, "Diego", then));
        created.add(plan.id());
        return plan;
    }

    private JsonNode create(Plan plan, UUID who) throws Exception {
        return body(mockMvc.perform(post(LINKS, plan.id()).with(user(who))).andExpect(status().isCreated()));
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    /** What a contact gets for a link that shows nothing, without the address asked for, which is the only difference */
    private String notFound(String token) throws Exception {
        var response = mockMvc.perform(get(OPEN, token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("trustedLink.notFound"))
                .andReturn().getResponse();
        var problem = (ObjectNode) json.readTree(response.getContentAsString());
        problem.remove("instance");
        return response.getStatus() + " " + response.getContentType() + " " + problem;
    }

    @Test
    void aParticipantSharesThePlanAndTheContactSeesItWithoutASession() throws Exception {
        var plan = planStartingIn(Duration.ofHours(1));

        var result = mockMvc.perform(post(LINKS, plan.id()).with(user(lucia)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.planId").value(plan.id().toString()));
        var link = body(result);
        var token = link.get("token").asString();

        assertThat(TrustedLinkToken.isWellFormed(token)).isTrue();
        assertThat(link.get("url").asString()).isEqualTo("http://localhost:4200/trusted/" + token);
        // PostgreSQL keeps microseconds
        assertThat(Instant.parse(link.get("expiresAt").asString())).isCloseTo(plan.endsAt(), within(1, ChronoUnit.MILLIS));
        // Only the hash of the secret is in the database
        assertThat(jdbc.queryForObject("SELECT token_hash FROM plan_trusted_link WHERE id = ?::uuid", String.class,
                link.get("id").asString())).isEqualTo(TrustedLinkToken.hash(token)).isNotEqualTo(token);

        var seen = mockMvc.perform(get(OPEN, token))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("X-Robots-Tag", "noindex, nofollow"))
                .andExpect(jsonPath("$.title").value("Padel 2 vs 2"))
                .andExpect(jsonPath("$.activity").value("PADEL"))
                .andExpect(jsonPath("$.meetingPoint.name").value("Albufera courts"))
                .andExpect(jsonPath("$.meetingPoint.latitude").value(40.396417))
                .andExpect(jsonPath("$.meetingPoint.longitude").value(-3.629712))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.organizerName").value("Ana"))
                .andExpect(jsonPath("$.sharedBy").value("Lucía"))
                .andReturn().getResponse().getContentAsString();
        assertThat(Instant.parse(json.readTree(seen).get("startsAt").asString()))
                .isCloseTo(plan.startsAt(), within(1, ChronoUnit.MILLIS));
        assertThat(Instant.parse(json.readTree(seen).get("endsAt").asString()))
                .isCloseTo(plan.endsAt(), within(1, ChronoUnit.MILLIS));
        // Nobody else of the group, no identifiers, not the description
        assertThat(seen).doesNotContain("Diego", "Bring water", plan.id().toString(), ana.toString(), lucia.toString(),
                diego.toString());
    }

    @Test
    void creatingListingAndRevokingNeedASessionAndTheLinkOnlyAllowsReading() throws Exception {
        var plan = planStartingIn(Duration.ofHours(1));
        var link = create(plan, lucia);

        mockMvc.perform(post(LINKS, plan.id())).andExpect(status().isUnauthorized());
        mockMvc.perform(get(LINKS, plan.id())).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(LINK, plan.id(), link.get("id").asString())).andExpect(status().isUnauthorized());
        // Whoever has the link can only read with it
        mockMvc.perform(post(OPEN, link.get("token").asString())).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(OPEN, link.get("token").asString())).andExpect(status().isUnauthorized());
        mockMvc.perform(get(OPEN, link.get("token").asString())).andExpect(status().isOk());
    }

    @Test
    void onlyWhoGoesToThePlanSharesItAndOnlyWhileItIsNotOver() throws Exception {
        var plan = planStartingIn(Duration.ofHours(1));
        var over = planStartingIn(Duration.ofHours(-4));

        mockMvc.perform(post(LINKS, plan.id()).with(user(marta)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("plan.notParticipant"));
        mockMvc.perform(post(LINKS, UUID.randomUUID()).with(user(lucia)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("plan.notFound"));
        mockMvc.perform(post(LINKS, over.id()).with(user(lucia)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("trustedLink.planOver"));
        mockMvc.perform(post(LINKS, plan.id()).with(user(ana))).andExpect(status().isCreated());
    }

    @Test
    void eachPersonHasUpToFiveLinksPerPlan() throws Exception {
        var plan = planStartingIn(Duration.ofHours(1));
        for (var i = 0; i < 5; i++) {
            create(plan, lucia);
        }

        mockMvc.perform(post(LINKS, plan.id()).with(user(lucia)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("trustedLink.limit"));
        mockMvc.perform(get(LINKS, plan.id()).with(user(lucia)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(5)));
    }

    @Test
    void iSeeAndRevokeOnlyMyLinksAndNeverTheirAddress() throws Exception {
        var plan = planStartingIn(Duration.ofHours(1));
        var mine = create(plan, lucia);
        var theirs = create(plan, diego);

        var list = mockMvc.perform(get(LINKS, plan.id()).with(user(lucia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(mine.get("id").asString()))
                .andExpect(jsonPath("$.items[0].token").doesNotExist())
                .andExpect(jsonPath("$.items[0].url").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertThat(list).doesNotContain(mine.get("token").asString(),
                TrustedLinkToken.hash(mine.get("token").asString()));
        mockMvc.perform(get(LINKS, plan.id()).with(user(marta)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(0)));

        // Lucía cannot revoke the link of Diego: the same answer, and it keeps working
        mockMvc.perform(delete(LINK, plan.id(), theirs.get("id").asString()).with(user(lucia)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(OPEN, theirs.get("token").asString())).andExpect(status().isOk());

        mockMvc.perform(delete(LINK, plan.id(), mine.get("id").asString()).with(user(lucia)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete(LINK, plan.id(), mine.get("id").asString()).with(user(lucia)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(OPEN, mine.get("token").asString())).andExpect(status().isNotFound());
        mockMvc.perform(get(LINKS, plan.id()).with(user(lucia))).andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void aLinkThatDoesNotExistExpiredWasRevokedOrIsOfSomeoneWhoLeftGetsExactlyTheSameAnswer() throws Exception {
        var plan = planStartingIn(Duration.ofHours(1));
        var revoked = create(plan, lucia);
        mockMvc.perform(delete(LINK, plan.id(), revoked.get("id").asString()).with(user(lucia)))
                .andExpect(status().isNoContent());
        var expired = create(plan, lucia);
        jdbc.update("UPDATE plan_trusted_link SET created_at = ?, expires_at = ? WHERE id = ?::uuid",
                Timestamp.from(Instant.now().minus(Duration.ofHours(5))),
                Timestamp.from(Instant.now().minus(Duration.ofSeconds(1))), expired.get("id").asString());
        var left = create(plan, diego);
        mockMvc.perform(delete(PlanController.PLANS + PlanController.PARTICIPANTS + PlanController.ME, plan.id())
                        .with(user(diego)))
                .andExpect(status().is2xxSuccessful());

        var missing = notFound(TrustedLinkToken.random());

        assertThat(notFound(revoked.get("token").asString())).isEqualTo(missing);
        assertThat(notFound(expired.get("token").asString())).isEqualTo(missing);
        assertThat(notFound(left.get("token").asString())).isEqualTo(missing);
        assertThat(notFound("not-a-token")).isEqualTo(missing);
        assertThat(missing).startsWith("404 application/problem+json");
    }

    @Test
    void expiredLinksAreRemoved() throws Exception {
        var plan = planStartingIn(Duration.ofHours(1));
        var expired = create(plan, lucia);
        var alive = create(plan, lucia);
        jdbc.update("UPDATE plan_trusted_link SET created_at = ?, expires_at = ? WHERE id = ?::uuid",
                Timestamp.from(Instant.now().minus(Duration.ofHours(5))),
                Timestamp.from(Instant.now().minus(Duration.ofSeconds(1))), expired.get("id").asString());

        assertThat(links.purgeExpired()).isGreaterThanOrEqualTo(1);

        assertThat(jdbc.queryForList("SELECT id::text FROM plan_trusted_link WHERE plan_id = ?", String.class,
                plan.id())).containsExactly(alive.get("id").asString());
    }

    @Test
    void myLinksAreInMyDataWithoutTheirSecretAndGoWithMyAccount() throws Exception {
        var plan = planStartingIn(Duration.ofHours(1));
        var mine = create(plan, lucia);
        var theirs = create(plan, diego);

        var data = mockMvc.perform(get(MyDataController.MY_DATA).with(user(lucia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustedLinks", hasSize(1)))
                .andExpect(jsonPath("$.trustedLinks[0].id").value(mine.get("id").asString()))
                .andExpect(jsonPath("$.trustedLinks[0].token_hash").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertThat(data).doesNotContain(TrustedLinkToken.hash(mine.get("token").asString()));

        accountDeleted.onDeleted(new AccountDeletedListener.AccountDeleted(lucia, Instant.now()));
        accountDeleted.onDeleted(new AccountDeletedListener.AccountDeleted(lucia, Instant.now()));

        mockMvc.perform(get(OPEN, mine.get("token").asString())).andExpect(status().isNotFound());
        mockMvc.perform(get(OPEN, theirs.get("token").asString())).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM plan_trusted_link WHERE user_id = ?", Long.class, lucia))
                .isZero();
    }
}
