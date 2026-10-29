package es.upm.miw.oneleft.users.domain.port.out;

import es.upm.miw.oneleft.users.domain.model.AcceptedConsent;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** The consent given by each person (HU-045). */
public interface Consents {

    Optional<AcceptedConsent> find(UUID userId);

    void save(UUID userId, String version, Instant acceptedAt);
}
