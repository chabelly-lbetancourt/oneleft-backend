package es.upm.miw.oneleft.reputation.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * The rules that keep the reputation from being inflated (HU-066). Only what adds points is limited, never what takes
 * them away, and the entry is always kept with the points it really gave, so that the ledger still explains the score:
 * <ul>
 *     <li>a daily cap per reason: from the cap on, that day, the entries of that reason give nothing;</li>
 *     <li>kudos from the same person within {@link #SAME_PERSON_WINDOW} are worth half the one before (5, 2, 1, 0);</li>
 *     <li>two people who gave each other {@link #MUTUAL_PAIR_KUDOS} kudos or more within that window are a pair that
 *     rates each other: their kudos give nothing.</li>
 * </ul>
 * Plans only count with a check-in: attending is the check-in itself, organizing needs a participant who checked in,
 * and kudos are only given between people who checked in (HU-060).
 */
public final class AntiCheatRules {

    /** Days start and end at midnight in Spain, where the plans are */
    public static final ZoneId ZONE = ZoneId.of("Europe/Madrid");

    /** Four plans attended in one day are already plenty: more look like check-ins to empty plans */
    public static final int MAX_ATTENDED_PER_DAY = 4;
    /** Organizing three plans held in one day is a lot of plans for the next few hours */
    public static final int MAX_ORGANIZED_PER_DAY = 3;
    /** Ten kudos received in one day (50 points) is a very good day */
    public static final int MAX_KUDOS_PER_DAY = 10;

    /** How far back the kudos between two people are looked at */
    public static final Duration SAME_PERSON_WINDOW = Duration.ofDays(30);
    /** Kudos in each direction, within the window, that make two people a pair that rates each other */
    public static final int MUTUAL_PAIR_KUDOS = 3;

    private static final Map<Reason, Integer> DAILY_CAPS = new EnumMap<>(Map.of(
            Reason.ATTENDED, MAX_ATTENDED_PER_DAY,
            Reason.ORGANIZED_HELD, MAX_ORGANIZED_PER_DAY,
            Reason.KUDOS, MAX_KUDOS_PER_DAY));

    private AntiCheatRules() {
    }

    /** The most entries of a reason that give points in one day; none for the reasons without a cap */
    public static Optional<Integer> dailyCap(Reason reason) {
        return Optional.ofNullable(DAILY_CAPS.get(reason));
    }

    /** Whether an entry may be cut by the daily cap: it adds points and its reason has a cap */
    public static boolean capped(LedgerEntry entry) {
        return entry.points() > 0 && DAILY_CAPS.containsKey(entry.reason());
    }

    /** The start of the day of an instant, in {@link #ZONE} */
    public static Instant startOfDay(Instant at) {
        return at.atZone(ZONE).toLocalDate().atStartOfDay(ZONE).toInstant();
    }

    /** The start of the next day, in {@link #ZONE}: a day is the interval from its start to this, not included */
    public static Instant endOfDay(Instant at) {
        return at.atZone(ZONE).toLocalDate().plusDays(1).atStartOfDay(ZONE).toInstant();
    }

    /**
     * @param earlierThatDay the entries of the same person and reason already in the ledger that day
     * @return the entry, worth nothing if the person already reached the cap of its reason that day
     */
    public static LedgerEntry dailyCapped(LedgerEntry entry, int earlierThatDay) {
        var cap = DAILY_CAPS.get(entry.reason());
        if (entry.points() <= 0 || cap == null || earlierThatDay < cap) {
            return entry;
        }
        return entry.limited(0, PointsLimit.DAILY_CAP);
    }

    /**
     * The entry of a kudos, after the limits between two people.
     *
     * @param givenToThem the kudos from the giver to the receiver within the window, this one included
     * @param givenBack   the kudos from the receiver to the giver within the window
     */
    public static LedgerEntry kudos(LedgerEntry entry, int givenToThem, int givenBack) {
        if (givenToThem >= MUTUAL_PAIR_KUDOS && givenBack >= MUTUAL_PAIR_KUDOS) {
            return entry.limited(0, PointsLimit.MUTUAL_PAIR);
        }
        var halvings = Math.clamp(givenToThem - 1L, 0, Integer.SIZE - 1);
        return entry.limited(entry.points() >> halvings, PointsLimit.SAME_PERSON);
    }
}
