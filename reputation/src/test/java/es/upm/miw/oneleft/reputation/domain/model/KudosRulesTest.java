package es.upm.miw.oneleft.reputation.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-060: who can give a kudos to whom, and until when. */
class KudosRulesTest {

    private static final Instant CLOSED = Instant.parse("2026-10-18T12:00:00Z");
    private final UUID plan = UUID.randomUUID();
    private final UUID ana = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();

    private Optional<Attendance> line(UUID userId, boolean organizer, boolean arrived) {
        return Optional.of(new Attendance(userId, plan, "PADEL", organizer, arrived, true, CLOSED));
    }

    @Test
    void whoeverArrivedGivesAKudosToSomeoneElseWhoArrivedForAWeek() {
        var kudos = KudosRules.kudos(lucia, line(lucia, false, true), line(ana, true, true),
                KudosKind.GREAT_ORGANIZER, CLOSED.plus(Duration.ofDays(6)));

        assertThat(kudos).isEqualTo(new Kudos(lucia, ana, plan, KudosKind.GREAT_ORGANIZER,
                CLOSED.plus(Duration.ofDays(6))));
        assertThat(KudosRules.until(line(lucia, false, true).orElseThrow())).isEqualTo(CLOSED.plus(KudosRules.WINDOW));
        assertThatThrownBy(() -> KudosRules.kudos(lucia, line(lucia, false, true), line(ana, true, true),
                KudosKind.PUNCTUAL, CLOSED.plus(KudosRules.WINDOW)))
                .isInstanceOfSatisfying(KudosException.class,
                        error -> assertThat(error.code()).isEqualTo("kudos.closed"));
    }

    @Test
    void onlyWhoeverArrivedGivesAndReceivesKudos() {
        assertThatThrownBy(() -> KudosRules.kudos(lucia, line(lucia, false, false), line(ana, true, true),
                KudosKind.GOOD_VIBES, CLOSED))
                .isInstanceOfSatisfying(KudosException.class,
                        error -> assertThat(error.code()).isEqualTo("kudos.notAttended"));
        assertThatThrownBy(() -> KudosRules.kudos(lucia, Optional.empty(), line(ana, true, true),
                KudosKind.GOOD_VIBES, CLOSED)).isInstanceOf(KudosException.class);
        assertThatThrownBy(() -> KudosRules.kudos(lucia, line(lucia, false, true), line(ana, true, false),
                KudosKind.GOOD_VIBES, CLOSED))
                .isInstanceOfSatisfying(ValidationException.class,
                        error -> assertThat(error.code()).isEqualTo("kudos.receiverNotAttended"));
        assertThatThrownBy(() -> KudosRules.kudos(lucia, line(lucia, false, true), Optional.empty(),
                KudosKind.GOOD_VIBES, CLOSED)).isInstanceOf(ValidationException.class);
    }

    @Test
    void nobodyGivesKudosToThemselvesAndOnlyTheOrganizerOrganized() {
        assertThatThrownBy(() -> KudosRules.kudos(lucia, line(lucia, false, true), line(lucia, false, true),
                KudosKind.GREAT_LEVEL, CLOSED))
                .isInstanceOfSatisfying(ValidationException.class,
                        error -> assertThat(error.code()).isEqualTo("kudos.self"));
        assertThatThrownBy(() -> KudosRules.kudos(ana, line(ana, true, true), line(lucia, false, true),
                KudosKind.GREAT_ORGANIZER, CLOSED))
                .isInstanceOfSatisfying(ValidationException.class,
                        error -> assertThat(error.code()).isEqualTo("kudos.notOrganizer"));
    }

    @Test
    void aKudosIsTheSameForTheSameGiverReceiverAndPlan() {
        var first = new Kudos(lucia, ana, plan, KudosKind.PUNCTUAL, CLOSED);
        var again = new Kudos(lucia, ana, plan, KudosKind.GOOD_VIBES, CLOSED.plusSeconds(60));

        assertThat(first.id()).isEqualTo(again.id());
        assertThat(new Kudos(ana, lucia, plan, KudosKind.PUNCTUAL, CLOSED).id()).isNotEqualTo(first.id());
        assertThatThrownBy(() -> new Kudos(null, ana, plan, KudosKind.PUNCTUAL, CLOSED))
                .isInstanceOf(ValidationException.class);
    }
}
