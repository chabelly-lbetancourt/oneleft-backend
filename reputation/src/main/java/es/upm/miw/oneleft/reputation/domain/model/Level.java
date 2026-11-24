package es.upm.miw.oneleft.reputation.domain.model;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

/**
 * The level of someone, from their score (HU-062): 🌱 Nuevo, ⚡ Habitual, 🔥 Referente del barrio and 👑 Leyenda. The
 * clients show the name and the emoji in the language of the person; the API only gives the level.
 */
public enum Level {

    /** 0 to 99 points, and anyone with a negative score */
    NEW(0),
    /** 100 to 499 points */
    REGULAR(100),
    /** 500 to 1,499 points */
    NEIGHBOURHOOD_REFERENCE(500),
    /** 1,500 points or more */
    LEGEND(1500);

    private final int minScore;

    Level(int minScore) {
        this.minScore = minScore;
    }

    public int minScore() {
        return minScore;
    }

    /** The highest level whose minimum the score reaches */
    public static Level of(int score) {
        return Arrays.stream(values()).filter(level -> score >= level.minScore)
                .max(Comparator.comparingInt(Level::minScore)).orElse(NEW);
    }

    /** The level that comes after this one; none after the last */
    public Optional<Level> next() {
        return ordinal() + 1 < values().length ? Optional.of(values()[ordinal() + 1]) : Optional.empty();
    }
}
