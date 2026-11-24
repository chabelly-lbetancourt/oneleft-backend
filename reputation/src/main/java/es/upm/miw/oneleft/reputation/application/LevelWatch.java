package es.upm.miw.oneleft.reputation.application;

import es.upm.miw.oneleft.reputation.domain.model.Level;
import es.upm.miw.oneleft.reputation.domain.model.Perks;
import es.upm.miw.oneleft.reputation.domain.port.out.PerkNotices;
import es.upm.miw.oneleft.reputation.domain.port.out.Scores;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Adds points to a score and, when they take the person to another level, up or down, tells the other services what
 * the new one is worth (HU-063). Points that leave the level as it was tell nothing.
 */
final class LevelWatch {

    private final Scores scores;
    private final PerkNotices notices;
    private final Clock clock;

    LevelWatch(Scores scores, PerkNotices notices, Clock clock) {
        this.scores = scores;
        this.notices = notices;
        this.clock = clock;
    }

    void add(UUID userId, int points) {
        var before = scores.scoresOf(List.of(userId)).getOrDefault(userId, 0);
        scores.add(userId, points);
        var level = Level.of(before + points);
        if (level != Level.of(before)) {
            notices.changed(userId, Perks.of(level), clock.instant());
        }
    }

    /** Tells the perks of everyone with a score: after a rebuild, and to fill the copies of the other services */
    void announceAll() {
        var now = clock.instant();
        scores.all().forEach((userId, score) -> notices.changed(userId, Perks.of(Level.of(score)), now));
    }
}
