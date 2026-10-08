package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.Availability;
import es.upm.miw.oneleft.plans.domain.model.FreePerson;
import es.upm.miw.oneleft.plans.domain.model.Interest;
import es.upm.miw.oneleft.plans.domain.model.Level;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException;
import es.upm.miw.oneleft.plans.domain.model.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-035: free mode and what the organizer of a nearby plan sees. */
class AvailabilityServiceTest {

    private static final UUID LUCIA = UUID.randomUUID();
    private static final UUID DIEGO = UUID.randomUUID();
    private static final UUID MARTA = UUID.randomUUID();
    private static final UUID PABLO = UUID.randomUUID();

    private final JoinPlanServiceTest.Plans plans = new JoinPlanServiceTest.Plans();
    private final Availabilities availabilities = new Availabilities();
    private final AvailabilityService service = new AvailabilityService(availabilities, plans, CLOCK);
    private final Plan plan = plans.save(Plan.publish(ana(), Activity.PADEL, "Padel match", null,
            new MeetingPoint("Courts", 40.3964, -3.6297), NOW.plus(Duration.ofHours(1)), 2, null, CLOCK));

    @Test
    void freeModeLastsOneToThreeHoursInAnApproximateZone() {
        var free = service.start(LUCIA, 40.391234, -3.628765, 2,
                Set.of(new Interest(Activity.PADEL, Level.INTERMEDIATE)));

        assertThat(free.until()).isEqualTo(NOW.plus(Duration.ofHours(2)));
        assertThat(free.latitude()).isEqualTo(40.39);
        assertThat(free.longitude()).isEqualTo(-3.63);
        assertThat(service.mine(LUCIA)).contains(free);
        assertThatThrownBy(() -> service.start(LUCIA, 40.39, -3.63, 4, Set.of()))
                .isInstanceOfSatisfying(ValidationException.class,
                        error -> assertThat(error.code()).isEqualTo("availability.hours"));
        assertThatThrownBy(() -> service.start(LUCIA, 40.39, -3.63, 0, Set.of()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void freeModeEndsWhenTurnedOffOrExpired() {
        service.start(LUCIA, 40.39, -3.63, 1, Set.of());
        service.stop(LUCIA);
        assertThat(service.mine(LUCIA)).isEmpty();

        service.start(DIEGO, 40.39, -3.63, 1, Set.of());
        var later = new AvailabilityService(availabilities, plans,
                Clock.fixed(NOW.plus(Duration.ofMinutes(61)), ZoneOffset.UTC));
        assertThat(later.mine(DIEGO)).isEmpty();
        assertThat(later.purgeExpired()).isEqualTo(1);
        assertThat(availabilities.data).isEmpty();
    }

    @Test
    void theOrganizerSeesWhoIsFreeNearbyForTheActivityWithoutNamesNorPlaces() {
        // Lucía about 1.8 km away, intermediate at padel; Diego 400 m away, free for anything; Marta only for the
        // cinema; Pablo too far
        service.start(LUCIA, 40.38, -3.63, 2, Set.of(new Interest(Activity.PADEL, Level.INTERMEDIATE),
                new Interest(Activity.TENNIS, Level.BEGINNER)));
        service.start(DIEGO, 40.40, -3.63, 2, Set.of());
        service.start(MARTA, 40.39, -3.63, 2, Set.of(new Interest(Activity.CINEMA, null)));
        service.start(PABLO, 40.50, -3.63, 2, Set.of());

        var free = service.freePeopleNear(plan.id(), plan.organizer().id());

        assertThat(free).containsExactly(
                new FreePerson(500, null, Set.of()),
                new FreePerson(2000, Level.INTERMEDIATE, Set.of(Activity.PADEL, Activity.TENNIS)));
    }

    @Test
    void nobodyElseSeesTheFreePeopleOfAPlan() {
        service.start(LUCIA, 40.39, -3.63, 2, Set.of());

        assertThat(service.freePeopleNear(plan.id(), DIEGO)).isEmpty();
        var started = plans.save(plan.advance(plan.startsAt()));
        assertThat(service.freePeopleNear(started.id(), started.organizer().id())).isEmpty();
        var unknown = UUID.randomUUID();
        assertThatThrownBy(() -> service.freePeopleNear(unknown, DIEGO)).isInstanceOf(PlanNotFoundException.class);
    }

    @Test
    void theParticipantsOfThePlanAndTheOrganizerAreNotCounted() {
        service.start(LUCIA, 40.39, -3.63, 2, Set.of());
        service.start(plan.organizer().id(), 40.39, -3.63, 2, Set.of());
        var joined = plans.save(plans.data.get(plan.id()).join(LUCIA, "Lucía", CLOCK));

        assertThat(service.freePeopleNear(joined.id(), joined.organizer().id())).isEmpty();
    }

    @Test
    void anAvailabilityKnowsTheLevelOfEachActivity() {
        var free = new Availability(LUCIA, 40.39, -3.63, NOW, Set.of(new Interest(Activity.PADEL, Level.ADVANCED)));
        assertThat(free.levelIn(Activity.PADEL)).isEqualTo(Level.ADVANCED);
        assertThat(free.levelIn(Activity.TENNIS)).isNull();
        assertThat(free.wants(Activity.TENNIS)).isFalse();
        assertThat(free.activeAt(NOW)).isFalse();
        assertThatThrownBy(() -> new Interest(null, null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new Availability(LUCIA, 91, 0, NOW, Set.of()))
                .isInstanceOfSatisfying(ValidationException.class,
                        error -> assertThat(error.code()).isEqualTo("availability.zone"));
    }
}
