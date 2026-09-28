package es.upm.miw.oneleft.users.domain.port.in;

import es.upm.miw.oneleft.users.domain.model.ApproximateZone;
import es.upm.miw.oneleft.users.domain.model.Hobby;
import es.upm.miw.oneleft.users.domain.model.Identity;
import es.upm.miw.oneleft.users.domain.model.Profile;

import java.util.List;
import java.util.UUID;

/**
 * Profile use cases: read and edit your own, and view another participant's.
 */
public interface ManageProfileUseCase {

    /** Returns the authenticated user's profile; creates it from their identity if it does not exist yet. */
    Profile myProfile(Identity identity);

    Profile updateMyProfile(Identity identity, String displayName, ApproximateZone zone, List<Hobby> hobbies);

    /** @throws es.upm.miw.oneleft.users.domain.model.ProfileNotFoundException if the user has no profile */
    Profile profileOf(UUID userId);
}
