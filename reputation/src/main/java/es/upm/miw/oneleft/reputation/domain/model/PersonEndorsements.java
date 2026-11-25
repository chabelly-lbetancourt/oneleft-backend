package es.upm.miw.oneleft.reputation.domain.model;

import java.util.List;

/**
 * The endorsements of someone as I see them (HU-061).
 *
 * @param counts     how many people vouch for each level of each of their activities
 * @param mine       the ones I gave them
 * @param endorsable the activities I can still endorse them in: we arrived at a plan of it and I have not yet
 */
public record PersonEndorsements(List<EndorsementCount> counts, List<Endorsement> mine, List<String> endorsable) {

    public PersonEndorsements {
        counts = List.copyOf(counts);
        mine = List.copyOf(mine);
        endorsable = List.copyOf(endorsable);
    }
}
