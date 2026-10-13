package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.CheckIn;
import es.upm.miw.oneleft.plans.domain.model.DeviceLocation;
import es.upm.miw.oneleft.plans.domain.model.JoinRejectedException;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanArrival;
import es.upm.miw.oneleft.plans.domain.model.PlanCancelled;
import es.upm.miw.oneleft.plans.domain.model.PlanCheckedIn;
import es.upm.miw.oneleft.plans.domain.model.PlanJoined;
import es.upm.miw.oneleft.plans.domain.model.PlanLeftEvent;
import es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException;
import es.upm.miw.oneleft.plans.domain.model.PlanPublished;
import es.upm.miw.oneleft.plans.domain.model.PlanReminder;
import es.upm.miw.oneleft.plans.domain.port.out.CheckInRepository;
import es.upm.miw.oneleft.plans.domain.port.out.PlanEventPublisher;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-025: the rules of the plan, the geofence, one check-in per person and the notice to the group. */
class CheckInServiceTest {

    private static final UUID LUCIA = UUID.randomUUID();
    private static final UUID DIEGO = UUID.randomUUID();
    private static final DeviceLocation HERE = new DeviceLocation(40.39, -3.62);
    private static final DeviceLocation FAR = new DeviceLocation(40.45, -3.70);

    private final JoinPlanServiceTest.Plans plans = new JoinPlanServiceTest.Plans();
    private final CheckIns checkIns = new CheckIns();
    private final List<PlanCheckedIn> events = new ArrayList<>();
    private final List<DeviceLocation> asked = new ArrayList<>();
    private final Plan plan = plans.save(Plan.publish(ana(), Activity.PADEL, "Padel 2 vs 2", null,
            new MeetingPoint("Courts", 40.39, -3.62), NOW.plus(Duration.ofHours(1)), 3, null, CLOCK)
            .join(LUCIA, "Lucía", CLOCK).join(DIEGO, "Diego", CLOCK));
    private final CheckInService service = serviceAt(plan.startsAt().minusSeconds(60));

    @Test
    void aParticipantAtTheMeetingPointChecksInAndTheRestOfTheGroupIsTold() {
        var checkIn = service.checkIn(plan.id(), LUCIA, HERE);

        assertThat(checkIn).isEqualTo(new CheckIn(plan.id(), LUCIA, "Lucía", plan.startsAt().minusSeconds(60)));
        assertThat(checkIns.findByPlan(plan.id())).containsExactly(checkIn);
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.name()).isEqualTo("Lucía");
            assertThat(event.recipientIds()).containsExactly(plan.organizer().id(), DIEGO);
        });
    }

    @Test
    void theGeofenceIsAskedAboutTheMeetingPointWithinOneHundredAndFiftyMeters() {
        assertThatThrownBy(() -> service.checkIn(plan.id(), LUCIA, FAR))
                .isInstanceOfSatisfying(JoinRejectedException.class,
                        error -> assertThat(error.code()).isEqualTo("checkIn.tooFar"));

        assertThat(asked).containsExactly(FAR);
        assertThat(checkIns.findByPlan(plan.id())).isEmpty();
        assertThat(events).isEmpty();
    }

    @Test
    void checkingInAgainKeepsTheFirstOneAndDoesNotTellTheGroupTwice() {
        var first = service.checkIn(plan.id(), LUCIA, HERE);

        var again = serviceAt(plan.startsAt().plusSeconds(600)).checkIn(plan.id(), LUCIA, HERE);

        assertThat(again).isEqualTo(first);
        assertThat(events).hasSize(1);
    }

    @Test
    void theRulesOfThePlanGoBeforeTheGeofence() {
        assertThatThrownBy(() -> service.checkIn(plan.id(), UUID.randomUUID(), HERE))
                .isInstanceOfSatisfying(JoinRejectedException.class,
                        error -> assertThat(error.code()).isEqualTo("plan.notParticipant"));
        assertThatThrownBy(() -> serviceAt(NOW).checkIn(plan.id(), LUCIA, HERE))
                .isInstanceOfSatisfying(JoinRejectedException.class,
                        error -> assertThat(error.code()).isEqualTo("checkIn.notYet"));
        assertThatThrownBy(() -> service.checkIn(UUID.randomUUID(), LUCIA, HERE))
                .isInstanceOf(PlanNotFoundException.class);
        assertThat(asked).isEmpty();
    }

    @Test
    void onlyTheGroupSeesWhoHasArrived() {
        service.checkIn(plan.id(), LUCIA, HERE);
        serviceAt(plan.startsAt()).checkIn(plan.id(), plan.organizer().id(), HERE);

        assertThat(service.checkIns(plan.id(), DIEGO)).extracting(CheckIn::name).containsExactly("Lucía", "Ana");
        assertThatThrownBy(() -> service.checkIns(plan.id(), UUID.randomUUID()))
                .isInstanceOfSatisfying(JoinRejectedException.class,
                        error -> assertThat(error.code()).isEqualTo("plan.notParticipant"));
    }

    private CheckInService serviceAt(Instant now) {
        return new CheckInService(plans, checkIns, (planId, location, meters) -> {
            asked.add(location);
            return planId.equals(plan.id()) && meters == CheckIn.GEOFENCE_METERS && location.equals(HERE);
        }, new Publisher(), Clock.fixed(now, ZoneOffset.UTC));
    }

    private static final class CheckIns implements CheckInRepository {
        private final Map<String, CheckIn> data = new HashMap<>();

        @Override
        public Optional<CheckIn> find(UUID planId, UUID userId) {
            return Optional.ofNullable(data.get(planId + ":" + userId));
        }

        @Override
        public CheckIn save(CheckIn checkIn) {
            data.put(checkIn.planId() + ":" + checkIn.userId(), checkIn);
            return checkIn;
        }

        @Override
        public List<CheckIn> findByPlan(UUID planId) {
            return data.values().stream().filter(checkIn -> checkIn.planId().equals(planId))
                    .sorted(Comparator.comparing(CheckIn::at)).toList();
        }
    }

    private final class Publisher implements PlanEventPublisher {
        @Override
        public void publish(PlanPublished event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void publish(PlanJoined event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void publish(PlanLeftEvent event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void publish(PlanReminder event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void publish(PlanCancelled event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void publish(PlanArrival event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void publish(PlanCheckedIn event) {
            events.add(event);
        }
    }
}
