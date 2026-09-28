package es.upm.miw.oneleft.users.domain.port.in;

import es.upm.miw.oneleft.users.domain.model.Identity;
import es.upm.miw.oneleft.users.domain.model.User;

/**
 * Use case: get the OneLeft user matching the authenticated identity.
 */
public interface GetCurrentUserUseCase {

    User currentUser(Identity identity);
}
