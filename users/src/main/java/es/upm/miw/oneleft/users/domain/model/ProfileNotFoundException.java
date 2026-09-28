package es.upm.miw.oneleft.users.domain.model;

import java.util.UUID;

public class ProfileNotFoundException extends RuntimeException {

    public ProfileNotFoundException(UUID userId) {
        super("No existe el perfil del usuario " + userId);
    }
}
