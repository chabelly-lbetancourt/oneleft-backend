package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.CheckIn;
import es.upm.miw.oneleft.plans.domain.model.DeviceLocation;
import es.upm.miw.oneleft.plans.domain.model.JoinRejectedException;
import es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException;
import es.upm.miw.oneleft.plans.domain.port.in.CheckInUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.CheckInRepository;
import es.upm.miw.oneleft.plans.domain.port.out.MeetingPointGeofence;
import es.upm.miw.oneleft.plans.domain.port.out.PlanEventPublisher;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Check-in at the meeting point (HU-025). The rules of the plan go first (in the group, within the time, not
 * cancelled) and then the geofence, measured by PostGIS against the meeting point. The location of the device is only
 * used for that question: it is neither saved nor logged.
 */
@Service
public class CheckInService implements CheckInUseCase {

    private final PlanRepository plans;
    private final CheckInRepository checkIns;
    private final MeetingPointGeofence geofence;
    private final PlanEventPublisher events;
    private final Clock clock;

    public CheckInService(PlanRepository plans, CheckInRepository checkIns, MeetingPointGeofence geofence,
                          PlanEventPublisher events, Clock clock) {
        this.plans = plans;
        this.checkIns = checkIns;
        this.geofence = geofence;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CheckIn checkIn(UUID planId, UUID userId, DeviceLocation location) {
        var plan = plans.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));
        var checkIn = plan.checkIn(userId, clock);
        if (!geofence.contains(planId, location, CheckIn.GEOFENCE_METERS)) {
            throw new JoinRejectedException("checkIn.tooFar", "You must be within " + CheckIn.GEOFENCE_METERS
                    + " m of the meeting point");
        }
        var previous = checkIns.find(planId, userId);
        if (previous.isPresent()) {
            return previous.get();
        }
        var saved = checkIns.save(checkIn);
        events.publish(plan.checkedInEvent(saved));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckIn> checkIns(UUID planId, UUID userId) {
        var plan = plans.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));
        plan.checkIsMember(userId);
        return checkIns.findByPlan(planId);
    }
}
