package es.upm.miw.oneleft.plans.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.COURTS;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static es.upm.miw.oneleft.plans.PlanFixtures.padelPlan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanTest {

    @Test
    void publishedPlanIsOpenWithAllSpotsFree() {
        var organizer = ana();
        var plan = padelPlan(organizer, Duration.ofHours(1), CLOCK);

        assertThat(plan.id()).isNotNull();
        assertThat(plan.status()).isEqualTo(PlanStatus.OPEN);
        assertThat(plan.occupied()).isZero();
        assertThat(plan.freeSpots()).isEqualTo(1);
        assertThat(plan.publishedAt()).isEqualTo(NOW);
        assertThat(plan.organizer()).isEqualTo(organizer);
    }

    @Test
    void planMustStartInTheNextHours() {
        assertThatThrownBy(() -> padelPlan(ana(), Duration.ofMinutes(4), CLOCK))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("at least 5 minutes");
        assertThatThrownBy(() -> padelPlan(ana(), Duration.ofHours(12).plusMinutes(1), CLOCK))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("next 12 hours");
        assertThatThrownBy(() -> Plan.publish(ana(), Activity.CINEMA, "Cinema", null, COURTS, null, 1, null, CLOCK))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(padelPlan(ana(), Duration.ofMinutes(5), CLOCK).startsAt()).isEqualTo(NOW.plusSeconds(300));
        assertThat(padelPlan(ana(), Duration.ofHours(12), CLOCK).startsAt()).isEqualTo(NOW.plusSeconds(43_200));
    }

    @Test
    void titleDescriptionAndSpotsAreValidated() {
        var startsAt = NOW.plusSeconds(3600);
        assertThatThrownBy(() -> Plan.publish(ana(), Activity.CINEMA, "Yo", null, COURTS, startsAt, 1, null, CLOCK))
                .hasMessageContaining("title");
        assertThatThrownBy(() -> Plan.publish(ana(), Activity.CINEMA, null, null, COURTS, startsAt, 1, null, CLOCK))
                .hasMessageContaining("title");
        assertThatThrownBy(() -> Plan.publish(ana(), Activity.CINEMA, "x".repeat(81), null, COURTS, startsAt, 1, null,
                CLOCK)).hasMessageContaining("title");
        assertThatThrownBy(() -> Plan.publish(ana(), Activity.CINEMA, "Cinema", "x".repeat(281), COURTS, startsAt, 1,
                null, CLOCK)).hasMessageContaining("description");
        assertThatThrownBy(() -> Plan.publish(ana(), Activity.CINEMA, "Cinema", null, COURTS, startsAt, 0, null, CLOCK))
                .hasMessageContaining("spots");
        assertThatThrownBy(() -> Plan.publish(ana(), Activity.CINEMA, "Cinema", null, COURTS, startsAt, 21, null, CLOCK))
                .hasMessageContaining("spots");
    }

    @Test
    void blankDescriptionIsStoredAsNull() {
        var plan = Plan.publish(ana(), Activity.CINEMA, "  Cine esta noche ", "   ", COURTS, NOW.plusSeconds(3600), 2,
                null, CLOCK);
        assertThat(plan.title()).isEqualTo("Cine esta noche");
        assertThat(plan.description()).isNull();
        assertThat(plan.level()).isNull();
    }

    @Test
    void occupiedSpotsCannotExceedTheSpots() {
        var organizer = ana();
        assertThatThrownBy(() -> new Plan(UUID.randomUUID(), organizer, Activity.CINEMA, "Cinema", null, COURTS,
                NOW, 2, 3, null, PlanStatus.OPEN, NOW)).hasMessageContaining("Occupied spots");
        assertThatThrownBy(() -> new Plan(UUID.randomUUID(), organizer, Activity.CINEMA, "Cinema", null, COURTS,
                NOW, 2, -1, null, PlanStatus.OPEN, NOW)).hasMessageContaining("Occupied spots");
        assertThatThrownBy(() -> new Plan(null, organizer, Activity.CINEMA, "Cinema", null, COURTS,
                NOW, 2, 0, null, PlanStatus.OPEN, NOW)).hasMessageContaining("Required plan data");
        assertThat(new Plan(UUID.randomUUID(), organizer, Activity.CINEMA, "Cinema", null, COURTS, NOW, 4, 3, null,
                PlanStatus.OPEN, NOW).freeSpots()).isEqualTo(1);
    }

    @Test
    void publishedEventDescribesThePlan() {
        var plan = padelPlan(ana(), Duration.ofHours(2), CLOCK);
        var event = plan.publishedEvent();

        assertThat(event.planId()).isEqualTo(plan.id());
        assertThat(event.organizerId()).isEqualTo(plan.organizer().id());
        assertThat(event.activity()).isEqualTo(Activity.PADEL);
        assertThat(event.latitude()).isEqualTo(40.3912);
        assertThat(event.longitude()).isEqualTo(-3.6287);
        assertThat(event.freeSpots()).isEqualTo(1);
        assertThat(event.level()).isEqualTo(Level.INTERMEDIATE);
        assertThat(event.occurredAt()).isEqualTo(NOW);
    }

    @Test
    void meetingPointAndOrganizerAreValidated() {
        assertThatThrownBy(() -> new MeetingPoint(" ", 40, -3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MeetingPoint(null, 40, -3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MeetingPoint("x".repeat(101), 40, -3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MeetingPoint("Pole", 91, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MeetingPoint("Pole", -91, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MeetingPoint("Dateline", 0, 181)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MeetingPoint("Dateline", 0, -181)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new MeetingPoint("  Bar Paco ", 40, -3).name()).isEqualTo("Bar Paco");
        assertThatThrownBy(() -> new Organizer(null, "Ana")).isInstanceOf(IllegalArgumentException.class);
        assertThat(new Organizer(UUID.randomUUID(), " ").name()).isEqualTo("Organizer");
        assertThat(new Organizer(UUID.randomUUID(), null).name()).isEqualTo("Organizer");
    }
}
