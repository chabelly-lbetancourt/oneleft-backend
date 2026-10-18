package es.upm.miw.oneleft.reputation.application;

import es.upm.miw.oneleft.reputation.domain.model.BadgeRules;
import es.upm.miw.oneleft.reputation.domain.port.out.BadgeNotices;
import es.upm.miw.oneleft.reputation.domain.port.out.Badges;
import es.upm.miw.oneleft.reputation.domain.port.out.TrackRecords;

import java.time.Instant;
import java.util.UUID;

/**
 * The badges someone has just reached are theirs, with a notice (HU-044); the ones they had change nothing. After an
 * attendance and after a kudos (HU-060), which counts for «Alma del barrio».
 */
final class BadgeAwards {

    private final TrackRecords trackRecords;
    private final Badges badges;
    private final BadgeNotices notices;

    BadgeAwards(TrackRecords trackRecords, Badges badges, BadgeNotices notices) {
        this.trackRecords = trackRecords;
        this.badges = badges;
        this.notices = notices;
    }

    void awardReached(UUID userId, Instant at) {
        BadgeRules.reached(trackRecords.of(userId)).stream()
                .filter(badge -> badges.award(userId, badge, at))
                .forEach(badge -> notices.earned(userId, badge, at));
    }
}
