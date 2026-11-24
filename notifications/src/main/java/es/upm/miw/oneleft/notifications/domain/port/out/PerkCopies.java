package es.upm.miw.oneleft.notifications.domain.port.out;

import es.upm.miw.oneleft.notifications.domain.model.MemberPerks;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** What the level of each person is worth, copied from the reputation service (HU-063). */
public interface PerkCopies {

    /** Nobody has perks: where there is no copy */
    PerkCopies NONE = new PerkCopies() {
        @Override
        public Map<UUID, MemberPerks> findByUsers(Collection<UUID> userIds) {
            return Map.of();
        }

        @Override
        public void save(MemberPerks perks) {
            // nothing to keep
        }
    };

    /** Of those whose perks are known; the rest are missing from the map. */
    Map<UUID, MemberPerks> findByUsers(Collection<UUID> userIds);

    /** Keeps the perks unless the ones kept are newer */
    void save(MemberPerks perks);
}
