package es.upm.miw.oneleft.users.domain.port.in;

import es.upm.miw.oneleft.users.domain.model.ConsentStatus;
import es.upm.miw.oneleft.users.domain.model.Identity;

/** The record of who accepted which version of the privacy text (HU-045). */
public interface ManageConsentUseCase {

    ConsentStatus status(Identity identity);

    /**
     * @throws es.upm.miw.oneleft.users.domain.model.ValidationException if the version is not the one in force
     */
    ConsentStatus accept(Identity identity, String version);
}
