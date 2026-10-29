package es.upm.miw.oneleft.users.application;

import es.upm.miw.oneleft.users.domain.model.AccountException;
import es.upm.miw.oneleft.users.domain.model.Identity;
import es.upm.miw.oneleft.users.domain.model.PhotoKey;
import es.upm.miw.oneleft.users.domain.model.StoredPhoto;
import es.upm.miw.oneleft.users.domain.port.out.AccountEvents;
import es.upm.miw.oneleft.users.domain.port.out.Identities;
import es.upm.miw.oneleft.users.domain.port.out.PersonalData;
import es.upm.miw.oneleft.users.domain.port.out.PhotoStorage;
import es.upm.miw.oneleft.users.domain.port.out.ServicesData;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-045: the file with my data from every service, and the order in which my account is deleted. */
class AccountServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-30T09:00:00Z"), ZoneOffset.UTC);
    private final UUID userId = UUID.randomUUID();
    private final Identity identity = new Identity(userId.toString(), "Ana", "ana@oneleft.dev", Set.of("user"));
    private final List<String> steps = new ArrayList<>();
    private boolean keycloakFails;
    private boolean servicesFail;

    private final PersonalData data = new PersonalData() {
        @Override
        public Map<String, List<Map<String, Object>>> of(UUID id) {
            return Map.of("profile", List.of(Map.of("display_name", "Ana")));
        }

        @Override
        public void erase(UUID id) {
            steps.add("data erased");
        }
    };
    private final ServicesData services = id -> {
        if (servicesFail) {
            throw new AccountException(AccountException.Reason.DATA_UNAVAILABLE, "plans is down", null);
        }
        return Map.of("plans", Map.of("participations", List.of()));
    };
    private final PhotoStorage storage = new PhotoStorage() {
        @Override
        public URI signUpload(String key, String contentType, long size, Duration validity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public URI signDownload(String key, Duration validity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<StoredPhoto> find(String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteUnder(String prefix, String keep) {
            steps.add("photos deleted under " + prefix + (keep == null ? "" : " except " + keep));
        }
    };
    private final Identities identities = id -> {
        if (keycloakFails) {
            throw new AccountException(AccountException.Reason.IDENTITY_UNAVAILABLE, "Keycloak is down", null);
        }
        steps.add("keycloak deleted");
    };
    private final AccountEvents events = (id, at) -> steps.add("announced " + id + " at " + at);
    private final AccountService service = new AccountService(data, services, storage, identities, events, CLOCK);

    @Test
    void theFileHasMyAccountAndWhatEveryServiceKeeps() {
        var copy = service.exportMyData(identity);

        assertThat(copy).containsKeys("exportedAt", "account", "users", "plans");
        assertThat(copy.get("exportedAt")).isEqualTo(CLOCK.instant());
        assertThat(copy.get("account")).isEqualTo(Map.of("userId", userId.toString(), "name", "Ana",
                "email", "ana@oneleft.dev", "roles", Set.of("user")));
    }

    @Test
    void thereIsNoFileIfAServiceCannotGiveItsPart() {
        servicesFail = true;

        assertThatThrownBy(() -> service.exportMyData(identity)).isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("reason", AccountException.Reason.DATA_UNAVAILABLE);
    }

    @Test
    void deletingErasesHereThenInKeycloakAndOnlyThenAnnounces() {
        service.deleteMyAccount(identity);

        assertThat(steps).containsExactly("data erased", "photos deleted under " + PhotoKey.prefixOf(userId),
                "keycloak deleted", "announced " + userId + " at " + CLOCK.instant());
    }

    @Test
    void ifKeycloakFailsNothingIsAnnouncedAndItCanBeTriedAgain() {
        keycloakFails = true;

        assertThatThrownBy(() -> service.deleteMyAccount(identity)).isInstanceOf(AccountException.class)
                .hasFieldOrPropertyWithValue("reason", AccountException.Reason.IDENTITY_UNAVAILABLE);

        assertThat(steps).noneMatch(step -> step.startsWith("announced"));
        // The photos went before it, so a second attempt has less to do and nothing is left half done
        assertThat(steps).containsExactly("data erased", "photos deleted under " + PhotoKey.prefixOf(userId));
    }
}
