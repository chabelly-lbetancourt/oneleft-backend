package es.upm.miw.oneleft.reputation.domain.port.in;

import es.upm.miw.oneleft.reputation.domain.model.Kudos;
import es.upm.miw.oneleft.reputation.domain.model.KudosKind;
import es.upm.miw.oneleft.reputation.domain.model.PlanKudos;

import java.util.Map;
import java.util.UUID;

/** Kudos between the people of a plan (HU-060). */
public interface KudosUseCase {

    /** Gives a kudos: +5 in the reputation of whoever receives it, and a notice with the name of whoever gives it */
    Kudos give(UUID giverId, String giverName, UUID planId, UUID receiverId, KudosKind kind);

    PlanKudos kudosOf(UUID me, UUID planId);

    /** The kudos someone has received, by kind; every kind appears, with 0 if none */
    Map<KudosKind, Long> receivedBy(UUID userId);
}
