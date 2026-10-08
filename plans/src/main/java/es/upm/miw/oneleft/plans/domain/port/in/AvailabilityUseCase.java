package es.upm.miw.oneleft.plans.domain.port.in;

import es.upm.miw.oneleft.plans.domain.model.Availability;
import es.upm.miw.oneleft.plans.domain.model.FreePerson;
import es.upm.miw.oneleft.plans.domain.model.Interest;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** «I'm free now» (HU-035). */
public interface AvailabilityUseCase {

    /** How far from the meeting point an organizer sees free people. */
    int RADIUS_METERS = 5_000;

    Availability start(UUID userId, double latitude, double longitude, int hours, Set<Interest> interests);

    /** My free mode, if it is still on. */
    Optional<Availability> mine(UUID userId);

    void stop(UUID userId);

    /**
     * Free people near an upcoming plan who would do its activity, only for its organizer: for anyone else, or for a
     * plan that has started, there is nobody to show.
     */
    List<FreePerson> freePeopleNear(UUID planId, UUID requesterId);

    /** Removes the expired availabilities. @return how many */
    int purgeExpired();
}
