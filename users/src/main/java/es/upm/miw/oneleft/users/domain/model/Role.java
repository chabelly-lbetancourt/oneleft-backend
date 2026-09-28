package es.upm.miw.oneleft.users.domain.model;

import java.util.Arrays;
import java.util.Optional;

public enum Role {
    USER,
    ADMIN;

    /**
     * Maps a role name from the identity provider (for example, "admin") to a OneLeft role.
     * Roles that OneLeft does not know are ignored.
     */
    public static Optional<Role> fromName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(role -> role.name().equalsIgnoreCase(name.trim()))
                .findFirst();
    }
}
