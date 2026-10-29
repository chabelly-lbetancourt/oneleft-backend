package es.upm.miw.oneleft.users.domain.port.in;

import es.upm.miw.oneleft.users.domain.model.Identity;

import java.util.Map;

/** My data and my account (HU-045): download everything the services keep of me, and delete the account. */
public interface ManageAccountUseCase {

    /**
     * Everything the services keep of the person, by service.
     *
     * @throws es.upm.miw.oneleft.users.domain.model.AccountException if a service cannot give its part: an
     *                                                                incomplete copy is not given
     */
    Map<String, Object> exportMyData(Identity identity);

    /** Erases the account here and in Keycloak, and announces it so that the other services erase theirs. */
    void deleteMyAccount(Identity identity);
}
