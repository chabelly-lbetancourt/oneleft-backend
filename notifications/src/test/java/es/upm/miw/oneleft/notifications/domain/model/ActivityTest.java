package es.upm.miw.oneleft.notifications.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-068: notices and alerts admit the new activities, but nobody can ask to hear about «other». */
class ActivityTest {

    @Test
    void theNewActivitiesAreInterestsAndOtherIsNot() {
        assertThat(Activity.YOGA.isOfInterest()).isTrue();
        assertThat(Activity.COFFEE.isOfInterest()).isTrue();
        assertThat(Activity.OTHER.isOfInterest()).isFalse();
        assertThat(Arrays.stream(Activity.values()).map(Activity::name)).allMatch(code -> code.length() <= 20);
    }

    @Test
    void preferencesAdmitNewActivitiesAndRejectOther() {
        var user = UUID.randomUUID();

        var accepted = new NotificationPreferences(user, true, 40.39, -3.63, 3_000,
                Set.of(Activity.YOGA, Activity.TAPAS), null, 5);

        assertThat(accepted.activities()).containsExactlyInAnyOrder(Activity.YOGA, Activity.TAPAS);
        assertThatThrownBy(() -> new NotificationPreferences(user, true, 40.39, -3.63, 3_000,
                Set.of(Activity.OTHER), null, 5)).isInstanceOf(ValidationException.class)
                .satisfies(error -> assertThat(((ValidationException) error).code())
                        .isEqualTo("notifications.activity"));
    }

    @Test
    void alertsAdmitNewActivitiesAndRejectOther() {
        var user = UUID.randomUUID();

        var accepted = new SavedAlert(UUID.randomUUID(), user, "Yoga", Set.of(Activity.YOGA), null, 40.39, -3.63,
                2_000, Set.of(), LocalTime.of(8, 0), LocalTime.of(10, 0));

        assertThat(accepted.activities()).containsExactly(Activity.YOGA);
        assertThatThrownBy(() -> new SavedAlert(UUID.randomUUID(), user, "Otra", Set.of(Activity.OTHER), null, 40.39,
                -3.63, 2_000, Set.of(), null, null)).isInstanceOf(ValidationException.class)
                .satisfies(error -> assertThat(((ValidationException) error).code()).isEqualTo("alerts.activity"));
    }
}
