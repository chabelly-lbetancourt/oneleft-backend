package es.upm.miw.oneleft.plans.domain.model;

/**
 * A link just created, with its secret (HU-034). This is the only time the secret is known: it is not stored.
 */
public record IssuedTrustedLink(TrustedLink link, String token) {
}
