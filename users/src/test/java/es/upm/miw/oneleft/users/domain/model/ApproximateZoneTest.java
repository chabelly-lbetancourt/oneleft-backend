package es.upm.miw.oneleft.users.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApproximateZoneTest {

    @Test
    void coordinatesAreRoundedToTwoDecimals() {
        var zone = new ApproximateZone("  Vallecas ", 40.391234, -3.628765);
        assertThat(zone.name()).isEqualTo("Vallecas");
        assertThat(zone.latitude()).isEqualTo(40.39);
        assertThat(zone.longitude()).isEqualTo(-3.63);
    }

    @Test
    void nameIsMandatoryAndLimited() {
        assertThatThrownBy(() -> new ApproximateZone(" ", 40, -3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApproximateZone(null, 40, -3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApproximateZone("x".repeat(61), 40, -3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void coordinatesMustBeInRange() {
        assertThatThrownBy(() -> new ApproximateZone("Pole", 91, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApproximateZone("Pole", -91, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApproximateZone("Fecha", 0, 181)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApproximateZone("Fecha", 0, -181)).isInstanceOf(IllegalArgumentException.class);
    }
}
