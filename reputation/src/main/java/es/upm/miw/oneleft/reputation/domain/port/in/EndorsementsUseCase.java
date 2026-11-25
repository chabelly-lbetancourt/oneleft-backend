package es.upm.miw.oneleft.reputation.domain.port.in;

import es.upm.miw.oneleft.reputation.domain.model.Endorsement;
import es.upm.miw.oneleft.reputation.domain.model.PersonEndorsements;
import es.upm.miw.oneleft.reputation.domain.model.SkillLevel;

import java.util.UUID;

/** Endorsements of the level of someone in an activity (HU-061). */
public interface EndorsementsUseCase {

    /** Vouches for the level of someone in an activity: +8 in their reputation, less if the two keep doing it */
    Endorsement endorse(UUID endorserId, UUID receiverId, String activity, SkillLevel level);

    /** The endorsements of someone, with the ones I gave them and the activities I can still endorse them in */
    PersonEndorsements endorsementsOf(UUID me, UUID userId);
}
