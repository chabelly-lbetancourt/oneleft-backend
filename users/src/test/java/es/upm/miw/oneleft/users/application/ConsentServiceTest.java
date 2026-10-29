package es.upm.miw.oneleft.users.application;

import es.upm.miw.oneleft.users.domain.model.AcceptedConsent;
import es.upm.miw.oneleft.users.domain.model.Identity;
import es.upm.miw.oneleft.users.domain.model.PrivacyPolicy;
import es.upm.miw.oneleft.users.domain.model.ValidationException;
import es.upm.miw.oneleft.users.domain.port.out.Consents;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-045: the record of who accepted which version of the privacy text. */
class ConsentServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-30T09:00:00Z"), ZoneOffset.UTC);
    private final UUID userId = UUID.randomUUID();
    private final Identity identity = new Identity(userId.toString(), "Ana", "ana@oneleft.dev", Set.of());
    private final Map<UUID, AcceptedConsent> saved = new HashMap<>();
    private final Consents consents = new Consents() {
        @Override
        public Optional<AcceptedConsent> find(UUID id) {
            return Optional.ofNullable(saved.get(id));
        }

        @Override
        public void save(UUID id, String version, Instant acceptedAt) {
            saved.put(id, new AcceptedConsent(version, acceptedAt));
        }
    };
    private final ConsentService service = new ConsentService(consents, CLOCK);

    @Test
    void nobodyHasAcceptedAtFirst() {
        var status = service.status(identity);

        assertThat(status.accepted()).isFalse();
        assertThat(status.acceptedVersion()).isNull();
        assertThat(status.currentVersion()).isEqualTo(PrivacyPolicy.CURRENT_VERSION);
    }

    @Test
    void acceptingTheTextInForceIsRecordedWithItsDate() {
        var status = service.accept(identity, PrivacyPolicy.CURRENT_VERSION);

        assertThat(status.accepted()).isTrue();
        assertThat(status.acceptedAt()).isEqualTo(CLOCK.instant());
        assertThat(saved.get(userId).version()).isEqualTo(PrivacyPolicy.CURRENT_VERSION);
    }

    @Test
    void anOlderTextAskedAgainAndAnUnknownOneIsRefused() {
        saved.put(userId, new AcceptedConsent("2025-01-01", CLOCK.instant()));
        var status = service.status(identity);

        assertThat(status.accepted()).isFalse();
        assertThat(status.acceptedVersion()).isEqualTo("2025-01-01");
        assertThatThrownBy(() -> service.accept(identity, "2025-01-01")).isInstanceOf(ValidationException.class)
                .hasFieldOrPropertyWithValue("code", "consent.version");
        assertThatThrownBy(() -> service.accept(identity, null)).isInstanceOf(ValidationException.class);
    }
}
