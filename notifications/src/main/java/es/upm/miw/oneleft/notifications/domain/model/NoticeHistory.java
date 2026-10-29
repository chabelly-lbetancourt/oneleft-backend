package es.upm.miw.oneleft.notifications.domain.model;

/**
 * What a person did with the notices they received recently (HU-012): how many, how many led to a spot and how many
 * arrived today. It feeds the acceptance model.
 */
public record NoticeHistory(long sent, long accepted, long sentToday) {

    public static final NoticeHistory NONE = new NoticeHistory(0, 0, 0);
    private static final int PRIOR_ACCEPTED = 1;
    private static final int PRIOR_SENT = 4;

    /** Acceptance rate smoothed towards 25 %: someone new is neither a sure yes nor a sure no. */
    public double rate() {
        return (accepted + PRIOR_ACCEPTED) / (double) (sent + PRIOR_SENT);
    }
}
