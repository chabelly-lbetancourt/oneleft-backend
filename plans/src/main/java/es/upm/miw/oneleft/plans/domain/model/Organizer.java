package es.upm.miw.oneleft.plans.domain.model;

import java.util.UUID;

public record Organizer(UUID id, String name) {

    public Organizer {
        if (id == null) {
            throw new IllegalArgumentException("El plan necesita un organizador");
        }
        name = name == null || name.isBlank() ? "Organizador" : name.strip();
    }
}
