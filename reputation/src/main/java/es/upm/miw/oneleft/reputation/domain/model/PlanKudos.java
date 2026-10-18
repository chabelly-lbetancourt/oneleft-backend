package es.upm.miw.oneleft.reputation.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The kudos of a plan as one of its people sees them (HU-060): whether they can still give them, until when, and the
 * others who arrived, with the kudos already given to each.
 *
 * @param until {@code null} if they did not arrive at the plan
 */
public record PlanKudos(boolean open, Instant until, List<Person> people) {

    public PlanKudos {
        people = List.copyOf(people);
    }

    /** @param given the kudos already given to them, or {@code null} */
    public record Person(UUID userId, boolean organizer, KudosKind given) {
    }
}
