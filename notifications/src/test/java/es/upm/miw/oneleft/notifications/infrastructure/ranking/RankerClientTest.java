package es.upm.miw.oneleft.notifications.infrastructure.ranking;

import es.upm.miw.oneleft.notifications.domain.model.Activity;
import es.upm.miw.oneleft.notifications.domain.model.Candidate;
import es.upm.miw.oneleft.notifications.domain.model.NoticeHistory;
import es.upm.miw.oneleft.notifications.domain.model.PublishedPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RankerClientTest {

    /** Monday 16/11/2026 at 17:00 in Madrid */
    private static final ZonedDateTime NOW = ZonedDateTime.ofInstant(Instant.parse("2026-11-16T16:00:00Z"),
            ZoneId.of("Europe/Madrid"));
    private static final String URL = "http://ranker:8086";

    private final UUID ana = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();
    private final PublishedPlan plan = new PublishedPlan(UUID.randomUUID(), ana, Activity.PADEL, "Pádel", "Pistas",
            40.39, -3.63, NOW.toInstant().plus(Duration.ofMinutes(90)), 2);
    private final Candidate candidate = new Candidate(lucia, 2_500, 5_000, true, new NoticeHistory(8, 3, 1));
    private MockRestServiceServer server;
    private RankerClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl(URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RankerClient(builder.build(), new RankingProperties("model", URL, 5, null, null, true));
    }

    @Test
    void sendsTheFeaturesTheRankerExpectsAndReadsTheProbabilities() {
        server.expect(requestTo(URL + "/v1/score")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.candidates[0].id").value(lucia.toString()))
                .andExpect(jsonPath("$.candidates[0].distanceKm").value(2.5))
                .andExpect(jsonPath("$.candidates[0].distanceRatio").value(0.5))
                .andExpect(jsonPath("$.candidates[0].minutesToStart").value(90))
                .andExpect(jsonPath("$.candidates[0].localHour").value(17))
                .andExpect(jsonPath("$.candidates[0].weekday").value(0))
                .andExpect(jsonPath("$.candidates[0].explicitInterest").value(1))
                .andExpect(jsonPath("$.candidates[0].viaAlert").value(0))
                .andExpect(jsonPath("$.candidates[0].pastRate").value(4 / 12.0))
                .andExpect(jsonPath("$.candidates[0].pastSent").value(8))
                .andExpect(jsonPath("$.candidates[0].sentToday").value(1))
                .andExpect(jsonPath("$.candidates[0].freeSpots").value(2))
                .andRespond(withSuccess("{\"modelVersion\":\"gbm-1\",\"scores\":[{\"id\":\"" + lucia
                        + "\",\"probability\":0.42}]}", MediaType.APPLICATION_JSON));

        var scores = client.score(plan, List.of(candidate), NOW);

        assertThat(scores).hasValueSatisfying(map -> assertThat(map).containsOnly(java.util.Map.entry(lucia, 0.42)));
        server.verify();
    }

    @Test
    void ifTheRankerFailsThereIsNoAnswerAndNoticesDoNotDependOnIt() {
        server.expect(requestTo(URL + "/v1/score")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThat(client.score(plan, List.of(candidate), NOW)).isEmpty();
    }

    @Test
    void withoutAnAddressTheRankerIsNeverCalled() {
        var unconfigured = new RankerClient(RestClient.create(), new RankingProperties("model", "", 5, null, null,
                true));

        assertThat(unconfigured.score(plan, List.of(candidate), NOW)).isEmpty();
    }

    @Test
    void aPlanThatHasAlreadyStartedHasZeroMinutesToStart() {
        var started = new PublishedPlan(plan.planId(), ana, Activity.PADEL, "Pádel", "Pistas", 40.39, -3.63,
                NOW.toInstant().minusSeconds(60), 1);

        assertThat(RankerClient.features(started, candidate, NOW).minutesToStart()).isZero();
    }

    @Test
    void theDefaultsAreFiveEveryThreeMinutesByModel() {
        var properties = new RankingProperties(null, null, 0, null, null, true);

        assertThat(properties.byModel()).isTrue();
        assertThat(properties.batchSize()).isEqualTo(5);
        assertThat(properties.interval()).isEqualTo(Duration.ofMinutes(3));
        assertThat(properties.cutoff()).isEqualTo(Duration.ofMinutes(15));
        assertThat(new RankingProperties("distance", null, 5, null, null, true).byModel()).isFalse();
    }
}
