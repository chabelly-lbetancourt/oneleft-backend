package es.upm.miw.oneleft.users.domain.model;

import java.util.Arrays;
import java.util.Optional;

public enum Role {
    USER,
    ADMIN;

    /**
     * Traduce el nombre de un rol del proveedor de identidad (por ejemplo, «admin») a un rol de OneLeft.
     * Los roles que OneLeft no conoce se ignoran.
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
