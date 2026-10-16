package es.upm.miw.oneleft.social.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-051: what is kept of each activity. */
class ActivityTest {

    private static final Instant NOW = Instant.parse("2026-10-16T10:00:00Z");

    @Test
    void theTitleIsTrimmedAndKeptWithinItsLength() {
        var activity = new Activity(UUID.randomUUID(), ActivityType.PUBLISHED, UUID.randomUUID(),
                "  " + "a".repeat(100), "PADEL", NOW, NOW);

        assertThat(activity.planTitle()).hasSize(Activity.MAX_TITLE_LENGTH);
        assertThat(new Activity(UUID.randomUUID(), ActivityType.JOINED, UUID.randomUUID(), null, "PADEL", NOW, NOW)
                .planTitle()).isEmpty();
    }

    @Test
    void anActivityWithoutWhoWhatOrWhenIsRejected() {
        assertThatThrownBy(() -> new Activity(null, ActivityType.JOINED, UUID.randomUUID(), "Padel", "PADEL", NOW,
                NOW)).isInstanceOf(ValidationException.class)
                .hasFieldOrPropertyWithValue("code", "activity.missingData");
        assertThatThrownBy(() -> new Activity(UUID.randomUUID(), ActivityType.JOINED, UUID.randomUUID(), "Padel",
                "PADEL", NOW, null)).isInstanceOf(ValidationException.class);
    }
}
