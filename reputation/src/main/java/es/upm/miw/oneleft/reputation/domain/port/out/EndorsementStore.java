package es.upm.miw.oneleft.reputation.domain.port.out;

import es.upm.miw.oneleft.reputation.domain.model.Endorsement;
import es.upm.miw.oneleft.reputation.domain.model.EndorsementCount;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** The endorsements given (HU-061). */
public interface EndorsementStore {

    /** @return false if the endorser had already endorsed that person in that activity */
    boolean save(Endorsement endorsement);

    /** How many people vouch for each level of each activity of someone, by activity and level */
    List<EndorsementCount> endorsementCountsOf(UUID receiverId);

    List<Endorsement> endorsementsBy(UUID endorserId, UUID receiverId);

    /** The endorsements from someone to someone else given from an instant to another, both included */
    int countEndorsements(UUID endorserId, UUID receiverId, Instant from, Instant to);
}
