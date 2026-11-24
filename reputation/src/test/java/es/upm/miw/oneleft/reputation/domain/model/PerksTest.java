package es.upm.miw.oneleft.reputation.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-063: what each level is worth, and who is a trusted organizer. */
class PerksTest {

    @Test
    void aNewPersonHasWhatEveryoneHadBeforeThePerks() {
        assertThat(Perks.of(Level.NEW)).isEqualTo(new Perks(Level.NEW, 0, 1.0, 5, 5));
    }

    @Test
    void eachLevelIsWorthMoreThanTheOneBefore() {
        var perks = Arrays.stream(Level.values()).map(Perks::of).toList();

        for (var higher = 1; higher < perks.size(); higher++) {
            var before = perks.get(higher - 1);
            var after = perks.get(higher);
            assertThat(after.waitlistPriority()).isGreaterThan(before.waitlistPriority());
            assertThat(after.noticeWeight()).isGreaterThan(before.noticeWeight());
            assertThat(after.maxActivePlans()).isGreaterThan(before.maxActivePlans());
            assertThat(after.maxSavedAlerts()).isGreaterThan(before.maxSavedAlerts());
        }
        assertThat(Perks.of(Level.LEGEND)).isEqualTo(new Perks(Level.LEGEND, 3, 1.5, 10, 10));
    }

    @Test
    void aTrustedOrganizerHasTheLevelAndThePlansHeld() {
        assertThat(Perks.trustedOrganizer(Level.REGULAR, 5)).isTrue();
        assertThat(Perks.trustedOrganizer(Level.LEGEND, 12)).isTrue();
        assertThat(Perks.trustedOrganizer(Level.REGULAR, 4)).isFalse();
        assertThat(Perks.trustedOrganizer(Level.NEW, 20)).isFalse();
    }

    @Test
    void theReputationSaysItsPerksAndItsSeal() {
        var userId = UUID.randomUUID();

        assertThat(new Reputation(userId, 600, 5).perks()).isEqualTo(Perks.of(Level.NEIGHBOURHOOD_REFERENCE));
        assertThat(new Reputation(userId, 600, 5).trustedOrganizer()).isTrue();
        assertThat(new Reputation(userId, 600).trustedOrganizer()).isFalse();
        assertThat(new Reputation(userId, 20, 9).trustedOrganizer()).isFalse();
    }
}
