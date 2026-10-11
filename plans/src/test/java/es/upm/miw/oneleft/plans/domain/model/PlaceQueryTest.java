package es.upm.miw.oneleft.plans.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-071: what can be searched as a meeting point. */
class PlaceQueryTest {

    private static void assertRejected(String text, double latitude, double longitude, String code) {
        assertThatThrownBy(() -> new PlaceQuery(text, latitude, longitude))
                .isInstanceOfSatisfying(ValidationException.class, error -> assertThat(error.code()).isEqualTo(code));
    }

    @Test
    void theTextHasFromThreeToOneHundredCharactersWithoutTheSpaces() {
        assertThat(new PlaceQuery("  pad  ", 40.39, -3.63).text()).isEqualTo("pad");
        assertThat(new PlaceQuery("p".repeat(100), 40.39, -3.63).text()).hasSize(100);
        assertRejected("  pa ", 40.39, -3.63, "place.query");
        assertRejected(null, 40.39, -3.63, "place.query");
        assertRejected("p".repeat(101), 40.39, -3.63, "place.query");
    }

    @Test
    void theDeviceIsSomewhereOnEarth() {
        assertRejected("padel", 90.01, -3.63, "coordinates.outOfRange");
        assertRejected("padel", 40.39, -180.01, "coordinates.outOfRange");
        assertThat(new PlaceQuery("padel", -90, 180).latitude()).isEqualTo(-90);
    }

    @Test
    void itMeasuresHowFarAPlaceIs() {
        var query = new PlaceQuery("polideportivo", 40.39, -3.63);

        // About 111 m per thousandth of a degree of latitude
        assertThat(query.distanceTo(new Place("Polideportivo", "", 40.391, -3.63))).isEqualTo(111);
        assertThat(query.distanceTo(new Place("Polideportivo", "", 40.39, -3.63))).isZero();
    }
}
