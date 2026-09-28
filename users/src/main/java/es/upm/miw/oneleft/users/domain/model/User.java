package es.upm.miw.oneleft.users.domain.model;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public record User(UUID id, String name, String email, Set<Role> roles) {

    public User {
        if (id == null) {
            throw new ValidationException("user.idRequired", "The user must have an identifier");
        }
        if (email == null || !email.contains("@")) {
            throw new ValidationException("user.invalidEmail", "The user must have a valid email");
        }
        roles = roles == null || roles.isEmpty() ? EnumSet.of(Role.USER) : Set.copyOf(roles);
    }

    public boolean isAdmin() {
        return roles.contains(Role.ADMIN);
    }
}
