package es.upm.miw.oneleft.reputation.domain.port.out;

import es.upm.miw.oneleft.reputation.domain.model.ActivityPlans;
import es.upm.miw.oneleft.reputation.domain.model.Attendance;
import es.upm.miw.oneleft.reputation.domain.model.MonthPlans;
import es.upm.miw.oneleft.reputation.domain.model.TrackRecord;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** What each person has done in the plans held, for the badges. */
public interface TrackRecords {

    /** The same person and plan twice count once */
    void record(List<Attendance> attendances);

    TrackRecord of(UUID userId);

    /** The plans organized that were held of several people at once (HU-063); who has none is missing */
    Map<UUID, Integer> organizedHeldOf(Collection<UUID> userIds);

    /** Someone in a plan whose attendance closed (HU-060) */
    Optional<Attendance> attendance(UUID userId, UUID planId);

    /** The people of a plan whose attendance closed */
    List<Attendance> attendancesOf(UUID planId);

    /** The plans of someone else that a person arrived at (HU-043) */
    int joinedOf(UUID userId);

    /** The activities a person arrived at the most, the most first; among equals, the one done the latest */
    List<ActivityPlans> topActivitiesOf(UUID userId, int limit);

    /** The months with plans of a person from an instant on, in any order (HU-043) */
    List<MonthPlans> monthsOf(UUID userId, Instant since);
}
