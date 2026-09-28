package es.upm.miw.oneleft.users.domain.port.out;

import es.upm.miw.oneleft.users.domain.model.Profile;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository {

    Optional<Profile> findById(UUID userId);

    Profile save(Profile profile);
}
