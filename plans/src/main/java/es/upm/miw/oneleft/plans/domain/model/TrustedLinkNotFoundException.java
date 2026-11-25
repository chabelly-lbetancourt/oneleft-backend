package es.upm.miw.oneleft.plans.domain.model;

/**
 * No plan can be seen with that link (HU-034). On purpose it does not say why: a link that never existed, one that
 * expired, one that was revoked and one of someone who left the plan get exactly the same answer, so that nobody can
 * tell them apart.
 */
public class TrustedLinkNotFoundException extends RuntimeException {

    public static final String CODE = "trustedLink.notFound";

    public TrustedLinkNotFoundException() {
        super("This link does not exist or is no longer available");
    }
}
