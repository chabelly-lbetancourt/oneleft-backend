package es.upm.miw.oneleft.users.domain.model;

import java.util.UUID;

public class ProfileNotFoundException extends RuntimeException {

    public static final String CODE = "profile.notFound";

    public ProfileNotFoundException(UUID userId) {
        super("There is no profile for user " + userId);
    }
}
