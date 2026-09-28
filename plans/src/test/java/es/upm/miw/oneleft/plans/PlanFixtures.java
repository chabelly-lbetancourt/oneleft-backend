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

/** Shared test data. */
public final class PlanFixtures {

    public static final Instant NOW = Instant.parse("2026-09-28T16:00:00Z");
    public static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    public static final MeetingPoint COURTS = new MeetingPoint("Sports centre courts", 40.3912, -3.6287);

    private PlanFixtures() {
    }

    public static Plan padelPlan(Organizer organizer, Duration startsIn, Clock clock) {
        return Plan.publish(organizer, Activity.PADEL, "Padel match, one player missing", "Intermediate level", COURTS,
                clock.instant().plus(startsIn), 1, Level.INTERMEDIATE, clock);
    }

    public static Organizer ana() {
        return new Organizer(UUID.randomUUID(), "Ana");
    }
}
