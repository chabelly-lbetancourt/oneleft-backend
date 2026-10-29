package es.upm.miw.oneleft.users.domain.model;

import java.time.Instant;

/** The version of the privacy text someone accepted, and when (HU-045). */
public record AcceptedConsent(String version, Instant acceptedAt) {
}
