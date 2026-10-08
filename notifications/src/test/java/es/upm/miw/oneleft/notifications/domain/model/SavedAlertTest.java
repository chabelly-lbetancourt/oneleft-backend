package es.upm.miw.oneleft.notifications.domain.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-036: what a saved alert accepts and which plans it looks for. */
class SavedAlertTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final UUID LUCIA = UUID.randomUUID();
    private static final UUID ANA = UUID.randomUUID();
    /** Monday 16/11/2026 at 18:30 in Madrid */
    private static final Instant MONDAY_EVENING = Instant.parse("2026-11-16T17:30:00Z");

    private static SavedAlert alert(Set<Activity> activities, Level level, Set<DayOfWeek> days, LocalTime from,
                                    LocalTime to) {
        return new SavedAlert(UUID.randomUUID(), LUCIA, " Pádel al salir ", activities, level, 40.391234, -3.628765,
                2_000, days, from, to);
    }

    private static PublishedPlan plan(Activity activity, Level level, double latitude, Instant startsAt) {
        return new PublishedPlan(UUID.randomUUID(), ANA, activity, "Plan", "Pistas", latitude, -3.63, startsAt, 1,
                level);
    }

    private static void assertRejected(Runnable creation, String code) {
        assertThatThrownBy(creation::run)
                .isInstanceOfSatisfying(ValidationException.class, error -> assertThat(error.code()).isEqualTo(code));
    }

    @Test
    void keepsAnApproximateZoneAndATidyName() {
        var alert = alert(Set.of(), null, Set.of(), null, null);
        assertThat(alert.name()).isEqualTo("Pádel al salir");
        assertThat(alert.latitude()).isEqualTo(40.39);
        assertThat(alert.longitude()).isEqualTo(-3.63);
        assertThat(alert.activities()).isEmpty();
        assertThat(alert.days()).isEmpty();
    }

    @Test
    void rejectsWrongData() {
        assertRejected(() -> new SavedAlert(UUID.randomUUID(), LUCIA, " ", Set.of(), null, 40, -3, 2_000, Set.of(),
                null, null), "alerts.name");
        assertRejected(() -> new SavedAlert(UUID.randomUUID(), LUCIA, "x".repeat(41), Set.of(), null, 40, -3, 2_000,
                Set.of(), null, null), "alerts.name");
        assertRejected(() -> new SavedAlert(UUID.randomUUID(), LUCIA, "Pádel", Set.of(), null, 91, -3, 2_000,
                Set.of(), null, null), "alerts.zone");
        assertRejected(() -> new SavedAlert(UUID.randomUUID(), LUCIA, "Pádel", Set.of(), null, 40, -3, 100,
                Set.of(), null, null), "alerts.radius");
        assertRejected(() -> alert(Set.of(), null, Set.of(), LocalTime.of(17, 0), null), "alerts.hours");
        assertRejected(() -> alert(Set.of(), null, Set.of(), LocalTime.of(21, 0), LocalTime.of(17, 0)),
                "alerts.hours");
        assertRejected(() -> new SavedAlert(null, LUCIA, "Pádel", Set.of(), null, 40, -3, 2_000, Set.of(), null,
                null), "alerts.missingData");
    }

    @Test
    void looksForTheActivitiesAndTheLevel() {
        var padelIntermediate = alert(Set.of(Activity.PADEL), Level.INTERMEDIATE, Set.of(), null, null);

        assertThat(padelIntermediate.matches(plan(Activity.PADEL, Level.INTERMEDIATE, 40.39, MONDAY_EVENING), MADRID))
                .isTrue();
        assertThat(padelIntermediate.matches(plan(Activity.PADEL, null, 40.39, MONDAY_EVENING), MADRID)).isTrue();
        assertThat(padelIntermediate.matches(plan(Activity.PADEL, Level.ADVANCED, 40.39, MONDAY_EVENING), MADRID))
                .isFalse();
        assertThat(padelIntermediate.matches(plan(Activity.TENNIS, Level.INTERMEDIATE, 40.39, MONDAY_EVENING),
                MADRID)).isFalse();
        assertThat(alert(Set.of(), null, Set.of(), null, null)
                .matches(plan(Activity.CINEMA, Level.ADVANCED, 40.39, MONDAY_EVENING), MADRID)).isTrue();
    }

    @Test
    void looksWithinItsRadiusAndNeverAtTheOwnPlans() {
        var alert = alert(Set.of(), null, Set.of(), null, null);

        // About 1.1 km north: in; about 3.3 km: out of the 2 km
        assertThat(alert.matches(plan(Activity.PADEL, null, 40.40, MONDAY_EVENING), MADRID)).isTrue();
        assertThat(alert.matches(plan(Activity.PADEL, null, 40.42, MONDAY_EVENING), MADRID)).isFalse();
        var own = new PublishedPlan(UUID.randomUUID(), LUCIA, Activity.PADEL, "Mío", "Pistas", 40.39, -3.63,
                MONDAY_EVENING, 1, null);
        assertThat(alert.matches(own, MADRID)).isFalse();
    }

    @Test
    void looksForTheDaysAndHoursOfTheStartInLocalTime() {
        var weekdayAfternoons = alert(Set.of(), null, Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
                LocalTime.of(17, 0), LocalTime.of(18, 30));

        assertThat(weekdayAfternoons.matches(plan(Activity.PADEL, null, 40.39,
                MONDAY_EVENING.minusSeconds(60)), MADRID)).isTrue();
        // 18:30 is the end, not included
        assertThat(weekdayAfternoons.matches(plan(Activity.PADEL, null, 40.39, MONDAY_EVENING), MADRID)).isFalse();
        // Sunday afternoon
        assertThat(weekdayAfternoons.matches(plan(Activity.PADEL, null, 40.39,
                MONDAY_EVENING.minusSeconds(86_400 + 60)), MADRID)).isFalse();
    }

    @Test
    void anEditKeepsTheIdAndTheOwner() {
        var alert = alert(Set.of(), null, Set.of(), null, null);
        var changes = new SavedAlert(UUID.randomUUID(), ANA, "Tenis", Set.of(Activity.TENNIS), Level.ADVANCED, 40.4,
                -3.7, 5_000, Set.of(DayOfWeek.SATURDAY), LocalTime.of(9, 0), LocalTime.of(13, 0));

        var edited = alert.edit(changes);

        assertThat(edited.id()).isEqualTo(alert.id());
        assertThat(edited.userId()).isEqualTo(LUCIA);
        assertThat(edited.name()).isEqualTo("Tenis");
        assertThat(edited.radiusMeters()).isEqualTo(5_000);
    }
}
