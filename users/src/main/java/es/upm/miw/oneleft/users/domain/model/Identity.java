package es.upm.miw.oneleft.users.domain.model;

import java.util.Set;

/**
 * Authenticated identity exactly as the identity provider (Keycloak) delivers it, uninterpreted.
 */
public record Identity(String subject, String name, String email, Set<String> roles) {

    public Identity {
        if (subject == null || subject.isBlank()) {
            throw new ValidationException("identity.subjectRequired", "The identity must have a subject");
        }
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }
}
