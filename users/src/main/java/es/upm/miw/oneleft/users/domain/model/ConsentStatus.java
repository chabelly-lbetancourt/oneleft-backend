package es.upm.miw.oneleft.users.domain.model;

import java.time.Instant;

/**
 * Whether someone accepted the privacy text in force (HU-045).
 *
 * @param acceptedVersion the version they accepted, or {@code null} if they did not accept any
 */
public record ConsentStatus(boolean accepted, String acceptedVersion, Instant acceptedAt, String currentVersion) {

    public static ConsentStatus of(AcceptedConsent consent) {
        var current = PrivacyPolicy.CURRENT_VERSION;
        if (consent == null) {
            return new ConsentStatus(false, null, null, current);
        }
        return new ConsentStatus(current.equals(consent.version()), consent.version(), consent.acceptedAt(), current);
    }
}
