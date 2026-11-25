package es.upm.miw.oneleft.reputation.application;

import es.upm.miw.oneleft.reputation.domain.model.AntiCheatRules;
import es.upm.miw.oneleft.reputation.domain.model.Endorsement;
import es.upm.miw.oneleft.reputation.domain.model.EndorsementException;
import es.upm.miw.oneleft.reputation.domain.model.EndorsementRules;
import es.upm.miw.oneleft.reputation.domain.model.LedgerEntry;
import es.upm.miw.oneleft.reputation.domain.model.PersonEndorsements;
import es.upm.miw.oneleft.reputation.domain.model.Reason;
import es.upm.miw.oneleft.reputation.domain.model.SkillLevel;
import es.upm.miw.oneleft.reputation.domain.port.in.EndorsementsUseCase;
import es.upm.miw.oneleft.reputation.domain.port.out.EndorsementStore;
import es.upm.miw.oneleft.reputation.domain.port.out.Ledger;
import es.upm.miw.oneleft.reputation.domain.port.out.PerkNotices;
import es.upm.miw.oneleft.reputation.domain.port.out.Scores;
import es.upm.miw.oneleft.reputation.domain.port.out.TrackRecords;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Endorsements of the level of someone in an activity (HU-061). Whoever arrived at a plan of that activity with them
 * vouches for their level, once per person and activity, which is checked with the track record that the reputation
 * already keeps: no question to the plans service. Each endorsement is an entry of the ledger (+8) for whoever
 * receives it, and the anti-cheating rules (HU-066) may make it worth less, or nothing: the endorsement itself is kept.
 */
@Service
@Transactional
public class EndorsementService implements EndorsementsUseCase {

    private final EndorsementStore endorsements;
    private final TrackRecords trackRecords;
    private final Ledger ledger;
    private final DailyCaps dailyCaps;
    private final LevelWatch levels;
    private final Clock clock;

    public EndorsementService(EndorsementStore endorsements, TrackRecords trackRecords, Ledger ledger, Scores scores,
                              PerkNotices perkNotices, Clock clock) {
        this.endorsements = endorsements;
        this.trackRecords = trackRecords;
        this.ledger = ledger;
        this.dailyCaps = new DailyCaps(ledger);
        this.levels = new LevelWatch(scores, perkNotices, clock);
        this.clock = clock;
    }

    @Override
    public Endorsement endorse(UUID endorserId, UUID receiverId, String activity, SkillLevel level) {
        var given = EndorsementRules.checked(new Endorsement(endorserId, receiverId, activity, level, clock.instant()),
                trackRecords.activitiesTogether(endorserId, receiverId));
        if (!endorsements.save(given)) {
            throw new EndorsementException(EndorsementException.Reason.ALREADY_GIVEN,
                    "One endorsement per person and activity");
        }
        var entry = limited(given, LedgerEntry.of(receiverId, Reason.ENDORSEMENT, given.id(), null, given.givenAt()));
        if (ledger.append(entry)) {
            levels.add(receiverId, entry.points());
        }
        return given;
    }

    /**
     * The points of an endorsement after the anti-cheating rules (HU-066): less for the same person lately, nothing
     * between a pair that keeps endorsing each other, and the daily cap of whoever receives it.
     */
    private LedgerEntry limited(Endorsement given, LedgerEntry entry) {
        var since = given.givenAt().minus(AntiCheatRules.SAME_PERSON_WINDOW);
        var toThem = endorsements.countEndorsements(given.endorserId(), given.receiverId(), since, given.givenAt());
        var back = endorsements.countEndorsements(given.receiverId(), given.endorserId(), since, given.givenAt());
        return dailyCaps.apply(AntiCheatRules.endorsement(entry, toThem, back));
    }

    @Override
    @Transactional(readOnly = true)
    public PersonEndorsements endorsementsOf(UUID me, UUID userId) {
        var counts = endorsements.endorsementCountsOf(userId);
        if (me.equals(userId)) {
            return new PersonEndorsements(counts, List.of(), List.of());
        }
        var mine = endorsements.endorsementsBy(me, userId);
        var endorsed = mine.stream().map(Endorsement::activity).toList();
        var endorsable = trackRecords.activitiesTogether(me, userId).stream()
                .filter(activity -> !endorsed.contains(activity)).sorted().toList();
        return new PersonEndorsements(counts, mine, endorsable);
    }
}
