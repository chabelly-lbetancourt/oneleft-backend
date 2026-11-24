package es.upm.miw.oneleft.notifications.infrastructure.messaging;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The reason of the {@code plan.cancelled} event says what the notification tells. */
class PlanCancelledMessageTest {

    private static PlanCancelledMessage message(String reason) {
        return new PlanCancelledMessage(UUID.randomUUID(), "Pádel", "Pistas", Instant.parse("2026-11-16T17:00:00Z"),
                List.of(UUID.randomUUID()), reason);
    }

    @Test
    void onlyTheReasonOfTheOrganizerMakesItACancellationByTheOrganizer() {
        var byOrganizer = message("ORGANIZER");

        assertThat(byOrganizer.toDomain().byOrganizer()).isTrue();
        assertThat(byOrganizer.toDomain().recipientIds()).isEqualTo(byOrganizer.recipientIds());
        assertThat(message("MINIMUM_NOT_REACHED").toDomain().byOrganizer()).isFalse();
        // An event of before the reason existed
        assertThat(message(null).toDomain().byOrganizer()).isFalse();
    }
}
