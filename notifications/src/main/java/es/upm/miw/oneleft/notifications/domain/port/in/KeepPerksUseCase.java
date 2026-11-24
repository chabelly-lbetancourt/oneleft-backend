package es.upm.miw.oneleft.notifications.domain.port.in;

import es.upm.miw.oneleft.notifications.domain.model.MemberPerks;

/** Keeping the copy of what the level of each person is worth, from the reputation service (HU-063). */
public interface KeepPerksUseCase {

    void changed(MemberPerks perks);
}
