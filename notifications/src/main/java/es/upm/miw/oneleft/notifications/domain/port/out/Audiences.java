package es.upm.miw.oneleft.notifications.domain.port.out;

import es.upm.miw.oneleft.notifications.domain.model.MemberAudience;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** The gender and age of people, copied from the users service (HU-070). */
public interface Audiences {

    /** Of those whose data is known; the rest are missing from the map. */
    Map<UUID, MemberAudience> findByUsers(Collection<UUID> userIds);

    void save(MemberAudience audience);
}
