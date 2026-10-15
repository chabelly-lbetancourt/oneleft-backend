package es.upm.miw.oneleft.social.infrastructure;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import es.upm.miw.oneleft.social.infrastructure.rest.FollowController;
import es.upm.miw.oneleft.social.infrastructure.rest.SocialController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-047 end to end inside the service: the REST API, a real PostgreSQL (the lists with names in one query, the counts
 * and the bulk acceptance) and the users service simulated by WireMock. The people and follows created are deleted.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SocialIntegrationTest {

    @RegisterExtension
    static final WireMockExtension USERS = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()).build();

    private static final String ME = SocialController.SOCIAL + SocialController.ME;
    private static final String VISIBILITY = SocialController.SOCIAL + SocialController.MY_VISIBILITY;

    @DynamicPropertySource
    static void usersService(DynamicPropertyRegistry registry) {
        registry.add("oneleft.users.url", USERS::baseUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final UUID ana = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();
    private final UUID diego = UUID.randomUUID();

    private static RequestPostProcessor user(UUID id, String name) {
        return jwt().jwt(token -> token.subject(id.toString()).claim("name", name));
    }

    private ResultActions as(UUID id, String name, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(user(id, name)));
    }

    private void usersKnows(UUID id, String name) {
        USERS.stubFor(WireMock.get(urlPathEqualTo(
                "/api/v1/users/" + id + "/profile")).willReturn(okJson("""
                {"userId": "%s", "displayName": "%s", "zoneName": "Vallecas", "hobbies": []}""".formatted(id, name))));
    }

    @BeforeEach
    void users() {
        USERS.resetAll();
    }

    /** The test leaves no person or follow behind (the follows go with their people). */
    @AfterEach
    void deleteWhatWasCreated() {
        jdbc.update("DELETE FROM person WHERE user_id IN (?, ?, ?)", ana, lucia, diego);
    }

    @Test
    void followingAPrivateProfileIsARequestUntilItsOwnerAcceptsIt() throws Exception {
        usersKnows(ana, "Ana");

        as(lucia, "Lucía", put(ME + FollowController.FOLLOWED, ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana"))
                .andExpect(jsonPath("$.status").value("PENDING"));
        // Ana was looked up once, with the token of whoever follows her
        USERS.verify(1, getRequestedFor(urlPathEqualTo("/api/v1/users/" + ana + "/profile"))
                .withHeader("Authorization", equalTo("Bearer token")));
        as(lucia, "Lucía", get(ME + FollowController.SENT_REQUESTS))
                .andExpect(jsonPath("$.items", hasSize(1))).andExpect(jsonPath("$.total").value(1));
        as(ana, "Ana", get(ME + FollowController.REQUESTS))
                .andExpect(jsonPath("$.items[0].name").value("Lucía"))
                .andExpect(jsonPath("$.items[0].status").value("PENDING"));
        as(ana, "Ana", get(ME)).andExpect(jsonPath("$.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.requests").value(1));

        as(ana, "Ana", post(ME + FollowController.ACCEPT, lucia))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED"));

        as(ana, "Ana", get(ME + FollowController.FOLLOWERS)).andExpect(jsonPath("$.items[0].name").value("Lucía"));
        as(lucia, "Lucía", get(ME + FollowController.FOLLOWING)).andExpect(jsonPath("$.items[0].name").value("Ana"));
        as(lucia, "Lucía", get(SocialController.SOCIAL + SocialController.PERSON, ana))
                .andExpect(jsonPath("$.relation").value("FOLLOWING"))
                .andExpect(jsonPath("$.followers").value(1));
        as(ana, "Ana", get(SocialController.SOCIAL + SocialController.PERSON, lucia))
                .andExpect(jsonPath("$.followsMe").value(true))
                .andExpect(jsonPath("$.relation").value("NONE"));
        USERS.verify(1, getRequestedFor(urlPathEqualTo("/api/v1/users/" + ana + "/profile")));
    }

    @Test
    void theNetworkOfAPrivateProfileIsOnlyForItsFollowers() throws Exception {
        usersKnows(ana, "Ana");
        as(ana, "Ana", get(ME));
        as(lucia, "Lucía", put(ME + FollowController.FOLLOWED, ana));
        var followers = SocialController.SOCIAL + SocialController.PERSON_FOLLOWERS;
        var following = SocialController.SOCIAL + SocialController.PERSON_FOLLOWING;

        as(lucia, "Lucía", get(SocialController.SOCIAL + SocialController.PERSON, ana))
                .andExpect(jsonPath("$.activityVisible").value(false));
        as(lucia, "Lucía", get(followers, ana))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("social.private"));
        as(diego, "Diego", get(following, ana)).andExpect(status().isForbidden());

        as(ana, "Ana", post(ME + FollowController.ACCEPT, lucia));

        as(lucia, "Lucía", get(SocialController.SOCIAL + SocialController.PERSON, ana))
                .andExpect(jsonPath("$.activityVisible").value(true));
        as(lucia, "Lucía", get(followers, ana))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].name").value("Lucía"));
        as(ana, "Ana", get(following, ana)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void aPublicProfileIsFollowedAtOnceAndOpeningOneAcceptsTheRequestsWaiting() throws Exception {
        as(ana, "Ana", put(VISIBILITY).contentType(MediaType.APPLICATION_JSON)
                .content("{\"visibility\": \"PUBLIC\"}")).andExpect(jsonPath("$.visibility").value("PUBLIC"));
        as(lucia, "Lucía", put(ME + FollowController.FOLLOWED, ana)).andExpect(jsonPath("$.status").value("ACCEPTED"));

        as(diego, "Diego", get(ME)).andExpect(status().isOk());
        as(ana, "Ana", put(ME + FollowController.FOLLOWED, diego)).andExpect(jsonPath("$.status").value("PENDING"));
        as(diego, "Diego", put(VISIBILITY).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\": \"PUBLIC\"}"))
                .andExpect(jsonPath("$.followers").value(1))
                .andExpect(jsonPath("$.requests").value(0));
        as(ana, "Ana", get(ME))
                .andExpect(jsonPath("$.followers").value(1))
                .andExpect(jsonPath("$.following").value(1))
                .andExpect(jsonPath("$.sentRequests").value(0));
    }

    @Test
    void takingBackDecliningAndRemovingLeaveNothingBehind() throws Exception {
        usersKnows(ana, "Ana");
        as(lucia, "Lucía", put(ME + FollowController.FOLLOWED, ana));
        as(lucia, "Lucía", delete(ME + FollowController.FOLLOWED, ana)).andExpect(status().isNoContent());
        as(ana, "Ana", get(ME + FollowController.REQUESTS)).andExpect(jsonPath("$.total").value(0));

        as(lucia, "Lucía", put(ME + FollowController.FOLLOWED, ana));
        as(ana, "Ana", delete(ME + FollowController.REQUEST, lucia)).andExpect(status().isNoContent());
        as(ana, "Ana", post(ME + FollowController.ACCEPT, lucia))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("follow.requestNotFound"));

        as(lucia, "Lucía", put(ME + FollowController.FOLLOWED, ana));
        as(ana, "Ana", post(ME + FollowController.ACCEPT, lucia));
        as(ana, "Ana", delete(ME + FollowController.FOLLOWER, lucia)).andExpect(status().isNoContent());
        as(ana, "Ana", get(ME + FollowController.FOLLOWERS)).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void theErrorsCarryAStableCode() throws Exception {
        USERS.stubFor(WireMock.get(urlPathEqualTo(
                "/api/v1/users/" + diego + "/profile")).willReturn(aResponse().withStatus(404)));
        as(lucia, "Lucía", put(ME + FollowController.FOLLOWED, diego))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("person.notFound"));
        USERS.stubFor(WireMock.get(urlPathEqualTo(
                "/api/v1/users/" + ana + "/profile")).willReturn(aResponse().withStatus(500)));
        as(lucia, "Lucía", put(ME + FollowController.FOLLOWED, ana))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("social.unavailable"));
        as(lucia, "Lucía", put(ME + FollowController.FOLLOWED, lucia))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("follow.self"));
        as(lucia, "Lucía", get(ME + FollowController.FOLLOWERS).param("size", "51"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("page.invalid"));
        as(lucia, "Lucía", put(VISIBILITY).contentType(MediaType.APPLICATION_JSON)
                .content("{\"visibility\": \"FRIENDS\"}")).andExpect(status().isBadRequest());
        as(lucia, "Lucía", put(VISIBILITY).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("validation"));
        mockMvc.perform(get(ME)).andExpect(status().isUnauthorized());
    }
}
