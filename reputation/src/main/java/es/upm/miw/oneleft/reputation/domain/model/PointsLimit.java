package es.upm.miw.oneleft.reputation.domain.model;

/**
 * Why an entry gave fewer points than its reason is worth (HU-066), so that the ledger still explains the score: the
 * entry is kept, with the points it really gave.
 */
public enum PointsLimit {

    /** The person already had the most entries of that reason for one day. */
    DAILY_CAP,
    /**
     * The same giver already gave kudos, or endorsements, to the same person lately: each one is worth half the one
     * before.
     */
    SAME_PERSON,
    /** Two people who keep giving kudos, or endorsements, to each other: they give nothing. */
    MUTUAL_PAIR
}
