package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.Availability;
import es.upm.miw.oneleft.plans.domain.model.FreePerson;
import es.upm.miw.oneleft.plans.domain.model.Interest;
import es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException;
import es.upm.miw.oneleft.plans.domain.model.PlanStatus;
import es.upm.miw.oneleft.plans.domain.port.in.AvailabilityUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.AvailabilityRepository;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * «I'm free now» (HU-035). Free people are only shown to the organizer of an upcoming plan nearby, and only as
 * {@link FreePerson}: no names, no places.
 */
@Service
public class AvailabilityService implements AvailabilityUseCase {

    private final AvailabilityRepository availabilities;
    private final PlanRepository plans;
    private final Clock clock;

    public AvailabilityService(AvailabilityRepository availabilities, PlanRepository plans, Clock clock) {
        this.availabilities = availabilities;
        this.plans = plans;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Availability start(UUID userId, double latitude, double longitude, int hours, Set<Interest> interests) {
        return availabilities.save(Availability.start(userId, latitude, longitude, hours, interests, clock));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Availability> mine(UUID userId) {
        return availabilities.findByUser(userId).filter(availability -> availability.activeAt(clock.instant()));
    }

    @Override
    @Transactional
    public void stop(UUID userId) {
        availabilities.delete(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FreePerson> freePeopleNear(UUID planId, UUID requesterId) {
        var plan = plans.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));
        var upcoming = plan.status() == PlanStatus.OPEN || plan.status() == PlanStatus.FULL;
        if (!upcoming || !plan.organizer().id().equals(requesterId)) {
            return List.of();
        }
        var point = plan.meetingPoint();
        return availabilities.findActiveNear(point.latitude(), point.longitude(), RADIUS_METERS, clock.instant())
                .stream()
                .filter(availability -> !availability.userId().equals(requesterId)
                        && !plan.isParticipant(availability.userId()) && availability.wants(plan.activity()))
                .map(availability -> FreePerson.near(availability, plan))
                .sorted(Comparator.comparingLong(FreePerson::distanceMeters))
                .toList();
    }

    @Override
    @Transactional
    public int purgeExpired() {
        return availabilities.deleteExpired(clock.instant());
    }
}
