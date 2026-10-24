package es.upm.miw.oneleft.social.domain.model;

/** Why someone reports a person or a plan (HU-033). */
public enum ReportReason {
    /** Advertising or the same message again and again. */
    SPAM,
    /** Insults, threats or unwanted insistence. */
    HARASSMENT,
    /** Offensive or sexual content. */
    INAPPROPRIATE,
    /** A false profile or a plan that does not exist. */
    FAKE,
    /** Something that puts someone at risk. */
    DANGEROUS,
    /** Anything else: the comment says what. */
    OTHER
}
