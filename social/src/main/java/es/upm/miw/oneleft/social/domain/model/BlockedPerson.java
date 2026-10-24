package es.upm.miw.oneleft.social.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Someone I blocked (HU-033) and since when. */
public record BlockedPerson(UUID userId, String name, Instant since) {
}
