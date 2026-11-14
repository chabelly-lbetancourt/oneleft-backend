package es.upm.miw.oneleft.notifications.application;

import es.upm.miw.oneleft.notifications.domain.model.PublishedPlan;
import es.upm.miw.oneleft.notifications.domain.port.out.Audiences;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Who of some people the plan is for (HU-070). A plan for everyone leaves nobody out and does not even look at the
 * data; otherwise one query gives the gender and age of all of them.
 */
@Component
class AudienceFilter {

    private final Audiences audiences;

    AudienceFilter(Audiences audiences) {
        this.audiences = audiences;
    }

    Set<UUID> allowed(PublishedPlan plan, Collection<UUID> userIds) {
        if (plan.audience().isEveryone()) {
            return new HashSet<>(userIds);
        }
        var known = audiences.findByUsers(userIds);
        var allowed = new HashSet<UUID>();
        for (var userId : userIds) {
            if (plan.audience().fits(userId, known)) {
                allowed.add(userId);
            }
        }
        return allowed;
    }
}
