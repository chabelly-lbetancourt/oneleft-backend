package es.upm.miw.oneleft.social.domain.port.in;

import es.upm.miw.oneleft.social.domain.model.Activity;

import java.util.UUID;

/** The activity of the plans, kept for the feeds (HU-051). A repeated event changes nothing. */
public interface RecordActivityUseCase {

    void record(Activity activity);

    /** Someone left a plan: it is no longer in the feed that they joined it. */
    void left(UUID planId, UUID userId);

    /** A plan was cancelled: nothing of it stays in the feeds. */
    void cancelled(UUID planId);
}
