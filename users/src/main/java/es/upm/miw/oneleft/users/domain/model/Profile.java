package es.upm.miw.oneleft.users.domain.model;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Perfil público de un usuario: nombre visible, zona aproximada y aficiones con su nivel.
 */
public record Profile(UUID userId, String displayName, ApproximateZone zone, List<Hobby> hobbies) {

    public static final int MAX_DISPLAY_NAME_LENGTH = 50;
    public static final int MAX_HOBBIES = 10;

    public Profile {
        if (userId == null) {
            throw new IllegalArgumentException("El perfil debe pertenecer a un usuario");
        }
        if (displayName == null || displayName.isBlank() || displayName.strip().length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "El nombre visible es obligatorio y admite hasta " + MAX_DISPLAY_NAME_LENGTH + " caracteres");
        }
        displayName = displayName.strip();
        hobbies = hobbies == null ? List.of() : List.copyOf(hobbies);
        if (hobbies.size() > MAX_HOBBIES) {
            throw new IllegalArgumentException("Se admiten como máximo " + MAX_HOBBIES + " aficiones");
        }
        if (hobbies.stream().map(Hobby::activity).distinct().count() != hobbies.size()) {
            throw new IllegalArgumentException("Cada actividad solo puede aparecer una vez");
        }
    }

    /** Perfil inicial de un usuario recién llegado: su nombre de Keycloak, sin zona ni aficiones. */
    public static Profile initial(User user) {
        return new Profile(user.id(), user.name(), null, List.of());
    }

    public Profile update(String newDisplayName, ApproximateZone newZone, Collection<Hobby> newHobbies) {
        return new Profile(userId, newDisplayName, newZone, newHobbies == null ? null : List.copyOf(newHobbies));
    }
}
