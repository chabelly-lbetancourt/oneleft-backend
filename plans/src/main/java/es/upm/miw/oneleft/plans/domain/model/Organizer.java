package es.upm.miw.oneleft.plans.domain.model;

import java.util.UUID;

public record Organizer(UUID id, String name) {

    public Organizer {
        if (id == null) {
            throw new ValidationException("plan.organizerRequired", "The plan needs an organizer");
        }
        name = name == null || name.isBlank() ? "Organizer" : name.strip();
    }
}
