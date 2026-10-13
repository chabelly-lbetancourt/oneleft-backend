package es.upm.miw.oneleft.social.domain.model;

/** Where I stand with someone: I do not follow them, I asked to, I follow them, or it is me. */
public enum Relation {
    NONE,
    REQUESTED,
    FOLLOWING,
    SELF
}
