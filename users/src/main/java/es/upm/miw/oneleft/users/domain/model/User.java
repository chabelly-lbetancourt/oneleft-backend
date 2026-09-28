package es.upm.miw.oneleft.users.domain.model;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public record User(UUID id, String name, String email, Set<Role> roles) {

    public User {
        if (id == null) {
            throw new IllegalArgumentException("El usuario debe tener identificador");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("El usuario debe tener un email válido");
        }
        roles = roles == null || roles.isEmpty() ? EnumSet.of(Role.USER) : Set.copyOf(roles);
    }

    public boolean isAdmin() {
        return roles.contains(Role.ADMIN);
    }
}
