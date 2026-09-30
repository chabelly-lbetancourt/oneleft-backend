package es.upm.miw.oneleft.notifications.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuietHoursTest {

    @Test
    void quietHoursInsideTheDay() {
        var siesta = new QuietHours(LocalTime.of(14, 0), LocalTime.of(16, 0));
        assertThat(siesta.includes(LocalTime.of(14, 0))).isTrue();
        assertThat(siesta.includes(LocalTime.of(15, 59))).isTrue();
        assertThat(siesta.includes(LocalTime.of(16, 0))).isFalse();
        assertThat(siesta.includes(LocalTime.of(13, 59))).isFalse();
    }

    @Test
    void quietHoursAcrossMidnight() {
        var night = new QuietHours(LocalTime.of(23, 0), LocalTime.of(8, 0));
        assertThat(night.includes(LocalTime.of(23, 30))).isTrue();
        assertThat(night.includes(LocalTime.of(3, 0))).isTrue();
        assertThat(night.includes(LocalTime.of(8, 0))).isFalse();
        assertThat(night.includes(LocalTime.of(12, 0))).isFalse();
    }

    @Test
    void quietHoursNeedAStartAndADifferentEnd() {
        assertThatThrownBy(() -> new QuietHours(LocalTime.NOON, LocalTime.NOON))
                .isInstanceOf(ValidationException.class).hasFieldOrPropertyWithValue("code", "notifications.quietHours");
        assertThatThrownBy(() -> new QuietHours(null, LocalTime.NOON)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new QuietHours(LocalTime.NOON, null)).isInstanceOf(ValidationException.class);
    }
}
