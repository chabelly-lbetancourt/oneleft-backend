package es.upm.miw.oneleft.users.application;

import es.upm.miw.oneleft.users.domain.model.ConsentStatus;
import es.upm.miw.oneleft.users.domain.model.Identity;
import es.upm.miw.oneleft.users.domain.model.PrivacyPolicy;
import es.upm.miw.oneleft.users.domain.model.ValidationException;
import es.upm.miw.oneleft.users.domain.port.in.ManageConsentUseCase;
import es.upm.miw.oneleft.users.domain.port.out.Consents;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/** The consent to the privacy text (HU-045): the version accepted and when, one per person. */
@Service
@Transactional
public class ConsentService implements ManageConsentUseCase {

    private final Consents consents;
    private final Clock clock;

    public ConsentService(Consents consents, Clock clock) {
        this.consents = consents;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ConsentStatus status(Identity identity) {
        return ConsentStatus.of(consents.find(UUID.fromString(identity.subject())).orElse(null));
    }

    @Override
    public ConsentStatus accept(Identity identity, String version) {
        if (!PrivacyPolicy.CURRENT_VERSION.equals(version)) {
            throw new ValidationException("consent.version", "Only the privacy text in force can be accepted");
        }
        consents.save(UUID.fromString(identity.subject()), version, clock.instant());
        return status(identity);
    }
}
