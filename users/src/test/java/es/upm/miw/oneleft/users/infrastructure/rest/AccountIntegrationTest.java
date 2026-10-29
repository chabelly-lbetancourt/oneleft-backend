package es.upm.miw.oneleft.users.infrastructure.rest;

import es.upm.miw.oneleft.users.StorageContainerConfiguration;
import es.upm.miw.oneleft.users.domain.model.AccountException;
import es.upm.miw.oneleft.users.domain.model.PrivacyPolicy;
import es.upm.miw.oneleft.users.domain.port.out.AccountEvents;
import es.upm.miw.oneleft.users.domain.port.out.Identities;
import es.upm.miw.oneleft.users.domain.port.out.ServicesData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-045 in the users service with a real PostgreSQL and storage: the file of my data, the consent, and the deletion
 * of my account end to end. Keycloak, the other services and the broker are simulated, and what is created is removed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(StorageContainerConfiguration.class)
class AccountIntegrationTest {

    private static final String ME = UserController.USERS + "/me";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private Identities identities;

    @MockitoBean
    private ServicesData services;

    @MockitoBean
    private AccountEvents events;

    private final UUID userId = UUID.randomUUID();

    private RequestPostProcessor me() {
        return jwt().jwt(token -> token.subject(userId.toString()).claim("name", "Lucía")
                .claim("email", "lucia@oneleft.dev"));
    }

    @BeforeEach
    void seed() {
        when(services.of(any())).thenReturn(Map.of("plans", Map.of("participations", java.util.List.of())));
        jdbc.update("INSERT INTO profile (user_id, display_name) VALUES (?, 'Lucía')", userId);
        jdbc.update("INSERT INTO profile_hobby (user_id, activity, level) VALUES (?, 'PADEL', 'INTERMEDIATE')", userId);
        jdbc.update("INSERT INTO profile_photo (user_id, object_key, updated_at) VALUES (?, ?, now())", userId,
                "photos/" + userId + "/a.png");
    }

    @AfterEach
    void clean() {
        jdbc.update("DELETE FROM profile WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM profile_photo WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM consent WHERE user_id = ?", userId);
    }

    private long rows(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE user_id = ?", Long.class, userId);
    }

    @Test
    void myDataIsOneFileWithEveryServiceAndAnnouncesNothing() throws Exception {
        mockMvc.perform(get(ME + AccountController.MY_DATA).with(me()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("oneleft-my-data.json")))
                .andExpect(jsonPath("$.account.userId").value(userId.toString()))
                .andExpect(jsonPath("$.account.email").value("lucia@oneleft.dev"))
                .andExpect(jsonPath("$.users.profile[0].display_name").value("Lucía"))
                .andExpect(jsonPath("$.users.hobbies", hasSize(1)))
                .andExpect(jsonPath("$.users.photo", hasSize(1)))
                .andExpect(jsonPath("$.plans.participations", hasSize(0)));

        verify(events, never()).deleted(any(), any());
    }

    @Test
    void thereIsNoFileIfAServiceCannotGiveItsPart() throws Exception {
        when(services.of(any())).thenThrow(
                new AccountException(AccountException.Reason.DATA_UNAVAILABLE, "plans is down", null));

        mockMvc.perform(get(ME + AccountController.MY_DATA).with(me()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("data.unavailable"));
    }

    @Test
    void consentIsRecordedForTheVersionInForceAndAnOtherOneIsRefused() throws Exception {
        mockMvc.perform(get(ME + AccountController.MY_CONSENT).with(me()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accepted").value(false))
                .andExpect(jsonPath("$.currentVersion").value(PrivacyPolicy.CURRENT_VERSION));

        mockMvc.perform(put(ME + AccountController.MY_CONSENT).with(me()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": \"1999-01-01\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("consent.version"));
        mockMvc.perform(put(ME + AccountController.MY_CONSENT).with(me()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": \"" + PrivacyPolicy.CURRENT_VERSION + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.acceptedVersion").value(PrivacyPolicy.CURRENT_VERSION));

        assertThat(rows("consent")).isEqualTo(1);
        // Accepting again changes the date, not the number of rows
        mockMvc.perform(put(ME + AccountController.MY_CONSENT).with(me()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": \"" + PrivacyPolicy.CURRENT_VERSION + "\"}")).andExpect(status().isOk());
        assertThat(rows("consent")).isEqualTo(1);
    }

    @Test
    void deletingTheAccountErasesItHereAndInKeycloakAndAnnouncesIt() throws Exception {
        mockMvc.perform(put(ME + AccountController.MY_CONSENT).with(me()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": \"" + PrivacyPolicy.CURRENT_VERSION + "\"}")).andExpect(status().isOk());

        mockMvc.perform(delete(ME + AccountController.MY_ACCOUNT).with(me())).andExpect(status().isNoContent());

        assertThat(rows("profile")).isZero();
        assertThat(rows("profile_hobby")).isZero();
        assertThat(rows("profile_photo")).isZero();
        assertThat(rows("consent")).isZero();
        verify(identities).delete(userId);
        verify(events).deleted(eq(userId), any());
    }

    @Test
    void ifKeycloakDoesNotAnswerNothingIsErasedAndNothingIsAnnounced() throws Exception {
        doThrow(new AccountException(AccountException.Reason.IDENTITY_UNAVAILABLE, "Keycloak is down", null))
                .when(identities).delete(userId);

        mockMvc.perform(delete(ME + AccountController.MY_ACCOUNT).with(me()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("account.unavailable"));

        // The rows were rolled back, so the person can try again
        assertThat(rows("profile")).isEqualTo(1);
        verify(events, never()).deleted(any(), any());
        Mockito.reset(identities);
    }
}
