package es.upm.miw.oneleft.reputation.domain.model;

import java.util.Collection;

/**
 * Who can endorse whom (HU-061): an endorsement says something about someone else, so only whoever really played with
 * them does it. Both arrived (check-in) at the same plan of that activity, at any time; nobody endorses themselves.
 */
public final class EndorsementRules {

    private EndorsementRules() {
    }

    /**
     * @param playedTogether the activities of the plans both arrived at
     * @return the endorsement, if it may be given; the reason why not otherwise
     */
    public static Endorsement checked(Endorsement wanted, Collection<String> playedTogether) {
        if (wanted.endorserId().equals(wanted.receiverId())) {
            throw new ValidationException("endorsement.self", "Nobody endorses themselves");
        }
        if (!playedTogether.contains(wanted.activity())) {
            throw new EndorsementException(EndorsementException.Reason.NOT_PLAYED_TOGETHER,
                    "Only whoever arrived at a plan of that activity with them endorses their level");
        }
        return wanted;
    }
}
