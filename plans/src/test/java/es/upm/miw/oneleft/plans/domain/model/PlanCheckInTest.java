package es.upm.miw.oneleft.plans.domain.model;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static es.upm.miw.oneleft.plans.PlanFixtures.padelPlan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-025: check-in at the meeting point, from 15 minutes before the start to 30 minutes after it. */
class PlanCheckInTest {

    private static final UUID LUCIA = UUID.randomUUID();

    private static Clock at(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    private final Plan plan = padelPlan(ana(), Duration.ofHours(2), CLOCK).join(LUCIA, "Lucía", CLOCK);
    private final Instant start = plan.startsAt();

    @Test
    void theCheckInOpensFifteenMinutesBeforeTheStartAndClosesThirtyMinutesAfter() {
        var opening = start.minus(CheckIn.OPENS_BEFORE);
        var closing = start.plus(CheckIn.CLOSES_AFTER);

        assertThat(plan.checkIn(LUCIA, at(opening))).isEqualTo(new CheckIn(plan.id(), LUCIA, "Lucía", opening));
        assertThat(plan.checkIn(LUCIA, at(closing)).at()).isEqualTo(closing);
        assertRejected(() -> plan.checkIn(LUCIA, at(opening.minusSeconds(1))), "checkIn.notYet");
        assertRejected(() -> plan.checkIn(LUCIA, at(closing.plusSeconds(1))), "checkIn.closed");
    }

    @Test
    void theOrganizerChecksInTooWithTheirName() {
        var organizer = plan.organizer();

        var checkIn = plan.checkIn(organizer.id(), at(start));

        assertThat(checkIn.name()).isEqualTo("Ana");
    }

    @Test
    void onlyTheGroupOfThePlanCanCheckIn() {
        assertRejected(() -> plan.checkIn(UUID.randomUUID(), at(start)), "plan.notParticipant");
    }

    @Test
    void nobodyChecksInToACancelledPlan() {
        var cancelled = new Plan(plan.id(), plan.organizer(), plan.activity(), plan.title(), plan.description(),
                plan.meetingPoint(), start, plan.spots(), 0, plan.level(), PlanStatus.CANCELLED, plan.publishedAt());

        assertRejected(() -> cancelled.checkIn(cancelled.organizer().id(), at(start)), "plan.notOpen");
    }

    @Test
    void theEventGoesToTheRestOfTheGroup() {
        var checkIn = plan.checkIn(LUCIA, at(start));

        var event = plan.checkedInEvent(checkIn);

        assertThat(event).isEqualTo(new PlanCheckedIn(plan.id(), plan.title(), LUCIA, "Lucía",
                List.of(plan.organizer().id()), start));
    }

    @Test
    void theTimeOfArrivalIsKeptToTheSecond() {
        var checkIn = plan.checkIn(LUCIA, at(start.plusNanos(123_456_789)));

        assertThat(checkIn.at()).isEqualTo(start);
    }

    @Test
    void aCheckInNeedsAllItsData() {
        assertThatThrownBy(() -> new CheckIn(plan.id(), LUCIA, null, start))
                .isInstanceOfSatisfying(ValidationException.class,
                        exception -> assertThat(exception.code()).isEqualTo("checkIn.missingData"));
    }

    @Test
    void theLocationOfTheDeviceMustBeOnEarth() {
        assertThat(new DeviceLocation(40.3912, -3.6287).latitude()).isEqualTo(40.3912);
        assertThatThrownBy(() -> new DeviceLocation(91, 0)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new DeviceLocation(0, -181)).isInstanceOf(ValidationException.class);
    }

    private static void assertRejected(ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(JoinRejectedException.class,
                exception -> assertThat(exception.code()).isEqualTo(code));
    }
}
