package es.upm.miw.oneleft.reputation.domain.port.out;

import es.upm.miw.oneleft.reputation.domain.model.Kudos;
import es.upm.miw.oneleft.reputation.domain.model.KudosKind;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The kudos given (HU-060). */
public interface KudosStore {

    /** @return false if the giver had already given one to that person in that plan */
    boolean save(Kudos kudos);

    List<Kudos> givenBy(UUID giverId, UUID planId);

    Map<KudosKind, Long> countsOf(UUID receiverId);
}
