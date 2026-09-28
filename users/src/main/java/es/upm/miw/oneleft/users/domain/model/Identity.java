package es.upm.miw.oneleft.users.domain.model;

import java.util.Set;

/**
 * Identidad autenticada tal y como la entrega el proveedor de identidad (Keycloak), sin interpretar.
 */
public record Identity(String subject, String name, String email, Set<String> roles) {

    public Identity {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("La identidad debe tener un sujeto");
        }
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }
}
