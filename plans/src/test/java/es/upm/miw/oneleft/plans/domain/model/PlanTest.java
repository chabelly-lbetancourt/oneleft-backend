package es.upm.miw.oneleft.plans.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.PISTAS;
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
        assertThat(plan.status()).isEqualTo(PlanStatus.ABIERTO);
        assertThat(plan.occupied()).isZero();
        assertThat(plan.freeSpots()).isEqualTo(1);
        assertThat(plan.publishedAt()).isEqualTo(NOW);
        assertThat(plan.organizer()).isEqualTo(organizer);
    }

    @Test
    void planMustStartInTheNextHours() {
        assertThatThrownBy(() -> padelPlan(ana(), Duration.ofMinutes(4), CLOCK))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("al menos 5 minutos");
        assertThatThrownBy(() -> padelPlan(ana(), Duration.ofHours(12).plusMinutes(1), CLOCK))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("próximas 12 horas");
        assertThatThrownBy(() -> Plan.publish(ana(), Activity.CINE, "Cine", null, PISTAS, null, 1, null, CLOCK))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(padelPlan(ana(), Duration.ofMinutes(5), CLOCK).startsAt()).isEqualTo(NOW.plusSeconds(300));
        assertThat(padelPlan(ana(), Duration.ofHours(12), CLOCK).startsAt()).isEqualTo(NOW.plusSeconds(43_200));
    }

    @Test
    void titleDescriptionAndSpotsAreValidated() {
        var startsAt = NOW.plusSeconds(3600);
        assertThatThrownBy(() -> Plan.publish(ana(), Activity.CINE, "Yo", null, PISTAS, startsAt, 1, null, CLOCK))
                .hasMessageContaining("título");
        assertThatThrownBy(() -> Plan.publish(ana(), Activity.CINE, null, null, PISTAS, startsAt, 1, null, CLOCK))
                .hasMessageContaining("título");
        assertThatThrownBy(() -> Plan.publish(ana(), Activity.CINE, "x".repeat(81), null, PISTAS, startsAt, 1, null,
                CLOCK)).hasMessageContaining("título");
        assertThatThrownBy(() -> Plan.publish(ana(), Activity.CINE, "Cine", "x".repeat(281), PISTAS, startsAt, 1,
                null, CLOCK)).hasMessageContaining("descripción");
        assertThatThrownBy(() -> Plan.publish(ana(), Activity.CINE, "Cine", null, PISTAS, startsAt, 0, null, CLOCK))
                .hasMessageContaining("plazas");
        assertThatThrownBy(() -> Plan.publish(ana(), Activity.CINE, "Cine", null, PISTAS, startsAt, 21, null, CLOCK))
                .hasMessageContaining("plazas");
    }

    @Test
    void blankDescriptionIsStoredAsNull() {
        var plan = Plan.publish(ana(), Activity.CINE, "  Cine esta noche ", "   ", PISTAS, NOW.plusSeconds(3600), 2,
                null, CLOCK);
        assertThat(plan.title()).isEqualTo("Cine esta noche");
        assertThat(plan.description()).isNull();
        assertThat(plan.level()).isNull();
    }

    @Test
    void occupiedSpotsCannotExceedTheSpots() {
        var organizer = ana();
        assertThatThrownBy(() -> new Plan(UUID.randomUUID(), organizer, Activity.CINE, "Cine", null, PISTAS,
                NOW, 2, 3, null, PlanStatus.ABIERTO, NOW)).hasMessageContaining("ocupadas");
        assertThatThrownBy(() -> new Plan(UUID.randomUUID(), organizer, Activity.CINE, "Cine", null, PISTAS,
                NOW, 2, -1, null, PlanStatus.ABIERTO, NOW)).hasMessageContaining("ocupadas");
        assertThatThrownBy(() -> new Plan(null, organizer, Activity.CINE, "Cine", null, PISTAS,
                NOW, 2, 0, null, PlanStatus.ABIERTO, NOW)).hasMessageContaining("obligatorios");
        assertThat(new Plan(UUID.randomUUID(), organizer, Activity.CINE, "Cine", null, PISTAS, NOW, 4, 3, null,
                PlanStatus.ABIERTO, NOW).freeSpots()).isEqualTo(1);
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
        assertThat(event.level()).isEqualTo(Level.INTERMEDIO);
        assertThat(event.occurredAt()).isEqualTo(NOW);
    }

    @Test
    void meetingPointAndOrganizerAreValidated() {
        assertThatThrownBy(() -> new MeetingPoint(" ", 40, -3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MeetingPoint(null, 40, -3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MeetingPoint("x".repeat(101), 40, -3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MeetingPoint("Polo", 91, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MeetingPoint("Polo", -91, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MeetingPoint("Fecha", 0, 181)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MeetingPoint("Fecha", 0, -181)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new MeetingPoint("  Bar Paco ", 40, -3).name()).isEqualTo("Bar Paco");
        assertThatThrownBy(() -> new Organizer(null, "Ana")).isInstanceOf(IllegalArgumentException.class);
        assertThat(new Organizer(UUID.randomUUID(), " ").name()).isEqualTo("Organizador");
        assertThat(new Organizer(UUID.randomUUID(), null).name()).isEqualTo("Organizador");
    }
}
