package es.upm.miw.oneleft.notifications.domain.model;

import java.util.UUID;

/**
 * A person who may be notified of a plan (HU-012): how far the plan is, how far they accept, whether they chose the
 * activity, and what they did with their latest notices.
 */
public record Candidate(UUID userId, double distanceMeters, int radiusMeters, boolean explicitInterest,
                        NoticeHistory history) {

    public double distanceRatio() {
        return radiusMeters == 0 ? 1 : distanceMeters / radiusMeters;
    }
}
