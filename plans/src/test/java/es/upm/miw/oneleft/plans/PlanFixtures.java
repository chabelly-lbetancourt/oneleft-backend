package es.upm.miw.oneleft.plans;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.Level;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.Organizer;
import es.upm.miw.oneleft.plans.domain.model.Plan;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

/** Datos de prueba comunes. */
public final class PlanFixtures {

    public static final Instant NOW = Instant.parse("2026-09-28T16:00:00Z");
    public static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    public static final MeetingPoint PISTAS = new MeetingPoint("Pistas del polideportivo", 40.3912, -3.6287);

    private PlanFixtures() {
    }

    public static Plan padelPlan(Organizer organizer, Duration startsIn, Clock clock) {
        return Plan.publish(organizer, Activity.PADEL, "Partido de pádel, falta uno", "Nivel medio", PISTAS,
                clock.instant().plus(startsIn), 1, Level.INTERMEDIO, clock);
    }

    public static Organizer ana() {
        return new Organizer(UUID.randomUUID(), "Ana");
    }
}
