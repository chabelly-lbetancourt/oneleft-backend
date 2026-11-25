package es.upm.miw.oneleft.reputation.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-061: who can endorse the level of whom. */
class EndorsementRulesTest {

    private static final Instant NOW = Instant.parse("2026-11-25T10:00:00.700Z");
    private final UUID ana = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();

    private Endorsement wanted(UUID endorser, UUID receiver, String activity) {
        return new Endorsement(endorser, receiver, activity, SkillLevel.INTERMEDIATE, NOW);
    }

    @Test
    void whoeverPlayedThatActivityWithThemEndorsesTheirLevel() {
        var endorsement = EndorsementRules.checked(wanted(lucia, ana, "PADEL"), List.of("RUNNING", "PADEL"));

        assertThat(endorsement).isEqualTo(new Endorsement(lucia, ana, "PADEL", SkillLevel.INTERMEDIATE,
                Instant.parse("2026-11-25T10:00:00Z")));
    }

    @Test
    void nobodyEndorsesThemselvesNotEvenWithPlansOfThatActivity() {
        assertThatThrownBy(() -> EndorsementRules.checked(wanted(ana, ana, "PADEL"), List.of("PADEL")))
                .isInstanceOfSatisfying(ValidationException.class,
                        error -> assertThat(error.code()).isEqualTo("endorsement.self"));
    }

    @Test
    void withoutAPlanOfThatActivityTogetherThereIsNoEndorsement() {
        assertThatThrownBy(() -> EndorsementRules.checked(wanted(lucia, ana, "PADEL"), List.of("RUNNING")))
                .isInstanceOfSatisfying(EndorsementException.class, error -> {
                    assertThat(error.code()).isEqualTo("endorsement.notPlayedTogether");
                    assertThat(error.reason()).isEqualTo(EndorsementException.Reason.NOT_PLAYED_TOGETHER);
                });
        assertThatThrownBy(() -> EndorsementRules.checked(wanted(lucia, ana, "PADEL"), List.of()))
                .isInstanceOf(EndorsementException.class);
    }

    @Test
    void anEndorsementNeedsItsPeopleItsActivityAndItsLevel() {
        assertThatThrownBy(() -> new Endorsement(lucia, ana, " ", SkillLevel.BEGINNER, NOW))
                .hasFieldOrPropertyWithValue("code", "endorsement.missingData");
        assertThatThrownBy(() -> new Endorsement(lucia, ana, "PADEL", null, NOW))
                .hasFieldOrPropertyWithValue("code", "endorsement.missingData");
        assertThatThrownBy(() -> new Endorsement(lucia, null, "PADEL", SkillLevel.BEGINNER, NOW))
                .hasFieldOrPropertyWithValue("code", "endorsement.missingData");
        assertThatThrownBy(() -> new Endorsement(lucia, ana, "A".repeat(31), SkillLevel.BEGINNER, NOW))
                .hasFieldOrPropertyWithValue("code", "endorsement.activity");
    }

    @Test
    void theSameEndorserReceiverAndActivityAreTheSameEndorsementWhateverItsLevel() {
        var one = wanted(lucia, ana, "PADEL");

        assertThat(new Endorsement(lucia, ana, "PADEL", SkillLevel.ADVANCED, NOW.plusSeconds(60)).id())
                .isEqualTo(one.id());
        assertThat(wanted(ana, lucia, "PADEL").id()).isNotEqualTo(one.id());
        assertThat(wanted(lucia, ana, "RUNNING").id()).isNotEqualTo(one.id());
    }
}
