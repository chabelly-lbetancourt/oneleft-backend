package es.upm.miw.oneleft.plans.infrastructure.persistence;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.ConcurrentPlanUpdateException;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.Minimum;
import es.upm.miw.oneleft.plans.domain.model.NearbySearch;
import es.upm.miw.oneleft.plans.domain.model.Organizer;
import es.upm.miw.oneleft.plans.domain.model.Participant;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output adapter with Spring Data JPA, Hibernate Spatial and PostGIS.
 */
@Repository
public class JpaPlanRepository implements PlanRepository {

    /** WGS84, the GPS reference system. PostGIS uses the (longitude, latitude) order. */
    static final int WGS84 = 4326;
    private static final GeometryFactory GEOMETRY = new GeometryFactory(new PrecisionModel(), WGS84);

    private static final UUID NOBODY = new UUID(0, 0);

    private final SpringDataPlanRepository jpa;

    public JpaPlanRepository(SpringDataPlanRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Plan save(Plan plan) {
        var point = plan.meetingPoint();
        var location = GEOMETRY.createPoint(new Coordinate(point.longitude(), point.latitude()));
        var participants = plan.participants().stream().map(JpaPlanRepository::toEmbeddable).toList();
        var waitlist = plan.waitlist().stream().map(JpaPlanRepository::toEmbeddable).toList();
        var minimum = plan.minimum();
        var entity = new PlanEntity(plan.id(), plan.organizer().id(), plan.organizer().name(), plan.activity(),
                plan.title(), plan.description(), point.name(), location, plan.startsAt(), plan.spots(),
                plan.occupied(), plan.level(), plan.status(), plan.publishedAt(), participants, waitlist,
                plan.remindedAt(), minimum == null ? null : minimum.participants(),
                minimum == null ? null : minimum.deadline(), minimum == null ? null : minimum.confirmedAt(),
                plan.version());
        try {
            // Flushing here makes a stale version fail inside the adapter, where it becomes a domain exception
            return toDomain(jpa.saveAndFlush(entity));
        } catch (OptimisticLockingFailureException stale) {
            throw new ConcurrentPlanUpdateException(plan.id());
        }
    }

    @Override
    public Optional<Plan> findById(UUID planId) {
        return jpa.findById(planId).map(JpaPlanRepository::toDomain);
    }

    @Override
    public List<Plan> findByOrganizerStartingAfter(UUID organizerId, Instant from) {
        return jpa.findByOrganizerIdAndStartsAtAfterOrderByStartsAt(organizerId, from).stream()
                .map(JpaPlanRepository::toDomain).toList();
    }

    @Override
    public List<Plan> findOpenNearby(NearbySearch search, Instant now, int limit) {
        var activities = (search.activities().isEmpty() ? Arrays.stream(Activity.values())
                : search.activities().stream()).map(Enum::name).toList();
        // Without a requester no organizer is excluded (no user has the nil UUID)
        var requester = search.requesterId() == null ? NOBODY : search.requesterId();
        return jpa.findOpenNearby(search.latitude(), search.longitude(), search.radiusMeters(), activities, requester,
                        now, search.until(now), limit).stream()
                .map(JpaPlanRepository::toDomain).toList();
    }

    @Override
    public List<UUID> findDueForLifecycle(Instant now) {
        return jpa.findDueForLifecycle(now, now.plus(Plan.REMINDER_LEAD), now.minus(Plan.DURATION));
    }

    private static Plan toDomain(PlanEntity e) {
        var meetingPoint = new MeetingPoint(e.getMeetingPoint(), e.getLocation().getY(), e.getLocation().getX());
        var participants = e.getParticipants().stream().map(JpaPlanRepository::toParticipant).toList();
        var waitlist = e.getWaitlist().stream().map(JpaPlanRepository::toParticipant).toList();
        return new Plan(e.getId(), new Organizer(e.getOrganizerId(), e.getOrganizerName()), e.getActivity(),
                e.getTitle(), e.getDescription(), meetingPoint, e.getStartsAt(), e.getSpots(), e.getOccupied(),
                e.getLevel(), e.getStatus(), e.getPublishedAt(), participants, waitlist, e.getRemindedAt(),
                e.getMinParticipants() == null ? null
                        : new Minimum(e.getMinParticipants(), e.getMinimumDeadline(), e.getConfirmedAt()),
                e.getVersion());
    }

    private static ParticipantEmbeddable toEmbeddable(Participant person) {
        return new ParticipantEmbeddable(person.userId(), person.name(), person.joinedAt());
    }

    private static Participant toParticipant(ParticipantEmbeddable person) {
        return new Participant(person.getUserId(), person.getName(), person.getJoinedAt());
    }
}
