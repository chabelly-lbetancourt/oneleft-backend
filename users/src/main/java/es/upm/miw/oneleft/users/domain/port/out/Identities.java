package es.upm.miw.oneleft.users.domain.port.out;

import java.util.UUID;

/** The accounts of the identity provider (Keycloak), as far as deleting them (HU-045). */
public interface Identities {

    /**
     * Deletes the account; there is nothing to do if it is already gone.
     *
     * @throws es.upm.miw.oneleft.users.domain.model.AccountException if the identity provider does not answer
     */
    void delete(UUID userId);
}
