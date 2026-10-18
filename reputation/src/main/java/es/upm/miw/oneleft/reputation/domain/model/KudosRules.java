package es.upm.miw.oneleft.reputation.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Who can give a kudos to whom (HU-060): two people who arrived at the same plan (check-in), for {@link #WINDOW} after
 * its attendance closed. Nobody gives one to themselves, and only the organizer can be told that they organized well.
 */
public final class KudosRules {

    public static final Duration WINDOW = Duration.ofDays(7);

    private KudosRules() {
    }

    /** Whether kudos can still be given in a plan someone arrived at */
    public static boolean open(Attendance mine, Instant now) {
        return mine.arrived() && now.isBefore(until(mine));
    }

    public static Instant until(Attendance attendance) {
        return attendance.closedAt().plus(WINDOW);
    }

    /** The kudos, if it may be given; the reason why not otherwise */
    public static Kudos kudos(UUID giverId, Optional<Attendance> mine, Optional<Attendance> theirs, KudosKind kind,
                              Instant now) {
        var giver = mine.filter(Attendance::arrived).orElseThrow(() -> new KudosException(
                KudosException.Reason.NOT_ATTENDED, "Only whoever arrived at the plan gives kudos in it"));
        var receiver = theirs.filter(Attendance::arrived).orElseThrow(() -> new ValidationException(
                "kudos.receiverNotAttended", "Kudos go to someone who arrived at the same plan"));
        if (receiver.userId().equals(giverId)) {
            throw new ValidationException("kudos.self", "Nobody gives kudos to themselves");
        }
        if (kind == KudosKind.GREAT_ORGANIZER && !receiver.organizer()) {
            throw new ValidationException("kudos.notOrganizer", "Only the organizer organized the plan");
        }
        if (!open(giver, now)) {
            throw new KudosException(KudosException.Reason.CLOSED, "Kudos are given up to a week after the plan");
        }
        return new Kudos(giverId, receiver.userId(), receiver.planId(), kind, now);
    }
}
