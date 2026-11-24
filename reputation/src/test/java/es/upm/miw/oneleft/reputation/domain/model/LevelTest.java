package es.upm.miw.oneleft.reputation.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-062: the level each score reaches, and where the next one starts. */
class LevelTest {

    @Test
    void eachScoreReachesTheLevelOfItsRange() {
        assertThat(Level.of(-40)).isEqualTo(Level.NEW);
        assertThat(Level.of(0)).isEqualTo(Level.NEW);
        assertThat(Level.of(99)).isEqualTo(Level.NEW);
        assertThat(Level.of(100)).isEqualTo(Level.REGULAR);
        assertThat(Level.of(499)).isEqualTo(Level.REGULAR);
        assertThat(Level.of(500)).isEqualTo(Level.NEIGHBOURHOOD_REFERENCE);
        assertThat(Level.of(1499)).isEqualTo(Level.NEIGHBOURHOOD_REFERENCE);
        assertThat(Level.of(1500)).isEqualTo(Level.LEGEND);
        assertThat(Level.of(Integer.MAX_VALUE)).isEqualTo(Level.LEGEND);
    }

    @Test
    void theReputationSaysItsLevelAndWhereTheNextOneStarts() {
        var userId = UUID.randomUUID();

        assertThat(new Reputation(userId, 35).level()).isEqualTo(Level.NEW);
        assertThat(new Reputation(userId, 35).nextLevelAt()).contains(100);
        assertThat(new Reputation(userId, 750).nextLevelAt()).contains(1500);
        assertThat(new Reputation(userId, 2000).nextLevelAt()).isEmpty();
        assertThat(Level.LEGEND.next()).isEmpty();
    }
}
