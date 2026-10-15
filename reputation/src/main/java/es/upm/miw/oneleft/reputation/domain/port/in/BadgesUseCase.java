package es.upm.miw.oneleft.reputation.domain.port.in;

import es.upm.miw.oneleft.reputation.domain.model.BadgeProgress;
import es.upm.miw.oneleft.reputation.domain.model.TrackRecord;

import java.util.List;
import java.util.UUID;

/** The badges for milestones (HU-044). */
public interface BadgesUseCase {

    /** Every badge, earned or not, with how far I am. */
    List<BadgeProgress> myBadges(UUID me);

    /** The badges someone has earned. */
    List<BadgeProgress> earnedBadgesOf(UUID userId);

    /** What someone has done, for their social profile (HU-048): the same plans that count for the badges. */
    TrackRecord trackRecordOf(UUID userId);
}
