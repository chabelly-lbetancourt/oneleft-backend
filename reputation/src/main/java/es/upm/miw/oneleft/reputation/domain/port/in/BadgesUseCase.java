package es.upm.miw.oneleft.reputation.domain.port.in;

import es.upm.miw.oneleft.reputation.domain.model.BadgeProgress;

import java.util.List;
import java.util.UUID;

/** The badges for milestones (HU-044). */
public interface BadgesUseCase {

    /** Every badge, earned or not, with how far I am. */
    List<BadgeProgress> myBadges(UUID me);

    /** The badges someone has earned. */
    List<BadgeProgress> earnedBadgesOf(UUID userId);
}
