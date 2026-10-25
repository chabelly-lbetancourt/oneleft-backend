package es.upm.miw.oneleft.social.domain.model;

/** Where I stand with someone: I do not follow them, I asked to, I follow them, I blocked them, or it is me. */
public enum Relation {
    NONE,
    REQUESTED,
    FOLLOWING,
    /** I blocked them (HU-033): we do not follow each other and I do not see their plans. */
    BLOCKED,
    SELF
}
