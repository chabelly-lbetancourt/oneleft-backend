package es.upm.miw.oneleft.reputation.infrastructure;

import es.upm.miw.oneleft.reputation.TestcontainersConfiguration;
import es.upm.miw.oneleft.reputation.domain.model.AntiCheatRules;
import es.upm.miw.oneleft.reputation.infrastructure.rest.MyStatsController;
import es.upm.miw.oneleft.reputation.infrastructure.rest.ReputationController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.sql.Timestamp;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-043 with a real PostgreSQL: my statistics are read from the track record, by month in the time of Spain. The
 * dates are relative to today, and the lines created are erased at the end.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MyStatsIntegrationTest {

    private static final String MY_STATS = ReputationController.REPUTATION + MyStatsController.MY_STATS;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final ZonedDateTime now = ZonedDateTime.now(AntiCheatRules.ZONE);
    private final UUID ana = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();

    private static RequestPostProcessor user(UUID id) {
        return jwt().jwt(token -> token.subject(id.toString()));
    }

    @AfterEach
    void erase() {
        jdbc.update("DELETE FROM track_record WHERE user_id IN (?, ?)", ana, lucia);
    }

    private void plan(UUID userId, String activity, boolean organizer, boolean arrived, ZonedDateTime closedAt) {
        jdbc.update("INSERT INTO track_record (user_id, plan_id, activity, organizer, arrived, held, closed_at) "
                + "VALUES (?, ?, ?, ?, ?, true, ?)", userId, UUID.randomUUID(), activity, organizer, arrived,
                Timestamp.from(closedAt.toInstant()));
    }

    @Test
    void myStatisticsAreMineByMonthAndByActivity() throws Exception {
        var lastMonth = now.minusMonths(1);
        plan(ana, "PADEL", true, true, now);
        plan(ana, "PADEL", false, true, now);
        plan(ana, "RUNNING", false, true, lastMonth);
        plan(ana, "CHESS", false, true, now.minusMonths(14));
        plan(ana, "RUNNING", false, false, now);
        plan(lucia, "CHESS", true, true, now);

        mockMvc.perform(get(MY_STATS).param("months", "3").param("activities", "2").with(user(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plansThisMonth").value(2))
                .andExpect(jsonPath("$.plansCompleted").value(4))
                .andExpect(jsonPath("$.plansOrganized").value(1))
                .andExpect(jsonPath("$.plansJoined").value(3))
                .andExpect(jsonPath("$.favoriteActivity").value("PADEL"))
                .andExpect(jsonPath("$.activities.length()").value(2))
                .andExpect(jsonPath("$.activities[0].activity").value("PADEL"))
                .andExpect(jsonPath("$.activities[0].plans").value(2))
                .andExpect(jsonPath("$.activities[1].activity").value("RUNNING"))
                .andExpect(jsonPath("$.months.length()").value(3))
                .andExpect(jsonPath("$.months[0].plans").value(0))
                .andExpect(jsonPath("$.months[1].month").value(YearMonth.from(lastMonth).toString()))
                .andExpect(jsonPath("$.months[1].plans").value(1))
                .andExpect(jsonPath("$.months[1].joined").value(1))
                .andExpect(jsonPath("$.months[2].month").value(YearMonth.from(now).toString()))
                .andExpect(jsonPath("$.months[2].plans").value(2))
                .andExpect(jsonPath("$.months[2].organized").value(1))
                .andExpect(jsonPath("$.months[2].joined").value(1));
        // Someone else gets theirs, never mine: the person is the one of the token
        mockMvc.perform(get(MY_STATS).with(user(lucia)))
                .andExpect(jsonPath("$.plansCompleted").value(1))
                .andExpect(jsonPath("$.plansJoined").value(0))
                .andExpect(jsonPath("$.favoriteActivity").value("CHESS"))
                .andExpect(jsonPath("$.months.length()").value(6));
    }

    @Test
    void withoutPlansThereIsNoFavouriteAndOnlyWithAToken() throws Exception {
        mockMvc.perform(get(MY_STATS).with(user(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plansThisMonth").value(0))
                .andExpect(jsonPath("$.favoriteActivity").doesNotExist())
                .andExpect(jsonPath("$.activities.length()").value(0));
        mockMvc.perform(get(MY_STATS)).andExpect(status().isUnauthorized());
    }

    @Test
    void onlyAFewMonthsAndActivitiesCanBeAsked() throws Exception {
        mockMvc.perform(get(MY_STATS).param("months", "13").with(user(ana)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("stats.invalid"));
        mockMvc.perform(get(MY_STATS).param("activities", "0").with(user(ana)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("stats.invalid"));
    }
}
