package es.upm.miw.oneleft.plans.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GeoDistanceTest {

    @Test
    void oneDegreeOfLatitudeIsAbout111Kilometres() {
        assertThat(GeoDistance.meters(40, -3.7, 41, -3.7)).isCloseTo(111_195, within(1.0));
    }

    @Test
    void measuresShortDistancesInTheCity() {
        // Puerta del Sol to Atocha station (Madrid): about 1.7 km as the crow flies
        assertThat(GeoDistance.meters(40.4169, -3.7035, 40.4066, -3.6892)).isCloseTo(1_680, within(20.0));
        assertThat(GeoDistance.meters(40.4169, -3.7035, 40.4169, -3.7035)).isZero();
    }

    @Test
    void worksAcrossTheAntimeridian() {
        assertThat(GeoDistance.meters(0, 179.9, 0, -179.9)).isCloseTo(22_239, within(1.0));
    }
}
