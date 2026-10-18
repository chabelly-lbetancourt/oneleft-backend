package es.upm.miw.oneleft.reputation.application;

import es.upm.miw.oneleft.reputation.domain.model.Kudos;
import es.upm.miw.oneleft.reputation.domain.model.KudosException;
import es.upm.miw.oneleft.reputation.domain.model.KudosKind;
import es.upm.miw.oneleft.reputation.domain.model.KudosRules;
import es.upm.miw.oneleft.reputation.domain.model.LedgerEntry;
import es.upm.miw.oneleft.reputation.domain.model.PlanKudos;
import es.upm.miw.oneleft.reputation.domain.model.Reason;
import es.upm.miw.oneleft.reputation.domain.port.in.KudosUseCase;
import es.upm.miw.oneleft.reputation.domain.port.out.BadgeNotices;
import es.upm.miw.oneleft.reputation.domain.port.out.Badges;
import es.upm.miw.oneleft.reputation.domain.port.out.KudosNotices;
import es.upm.miw.oneleft.reputation.domain.port.out.KudosStore;
import es.upm.miw.oneleft.reputation.domain.port.out.Ledger;
import es.upm.miw.oneleft.reputation.domain.port.out.Scores;
import es.upm.miw.oneleft.reputation.domain.port.out.TrackRecords;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Kudos between the people of a plan (HU-060). Whoever arrived gives one to each of the others who arrived, up to a
 * week after the plan, which is checked with the track record that the reputation already keeps: no question to the
 * plans service. Each kudos is an entry of the ledger (+5) for whoever receives it, may bring the badge «Alma del
 * barrio», and is told to them in real time.
 */
@Service
@Transactional
public class KudosService implements KudosUseCase {

    private final KudosStore kudos;
    private final TrackRecords trackRecords;
    private final Ledger ledger;
    private final Scores scores;
    private final KudosNotices notices;
    private final BadgeAwards awards;
    private final Clock clock;

    public KudosService(KudosStore kudos, TrackRecords trackRecords, Ledger ledger, Scores scores,
                        KudosNotices notices, Badges badges, BadgeNotices badgeNotices, Clock clock) {
        this.kudos = kudos;
        this.trackRecords = trackRecords;
        this.ledger = ledger;
        this.scores = scores;
        this.notices = notices;
        this.awards = new BadgeAwards(trackRecords, badges, badgeNotices);
        this.clock = clock;
    }

    @Override
    public Kudos give(UUID giverId, String giverName, UUID planId, UUID receiverId, KudosKind kind) {
        var given = KudosRules.kudos(giverId, trackRecords.attendance(giverId, planId),
                trackRecords.attendance(receiverId, planId), kind, clock.instant());
        if (!kudos.save(given)) {
            throw new KudosException(KudosException.Reason.ALREADY_GIVEN, "One kudos per person and plan");
        }
        var entry = LedgerEntry.of(receiverId, Reason.KUDOS, given.id(), ledger.planTitle(planId).orElse(null),
                given.givenAt());
        if (ledger.append(entry)) {
            scores.add(receiverId, entry.points());
            awards.awardReached(receiverId, given.givenAt());
        }
        notices.received(given, giverName);
        return given;
    }

    @Override
    @Transactional(readOnly = true)
    public PlanKudos kudosOf(UUID me, UUID planId) {
        var people = trackRecords.attendancesOf(planId);
        var mine = people.stream().filter(person -> person.userId().equals(me) && person.arrived()).findFirst();
        if (mine.isEmpty()) {
            return new PlanKudos(false, null, List.of());
        }
        var given = kudos.givenBy(me, planId).stream()
                .collect(Collectors.toMap(Kudos::receiverId, Kudos::kind));
        var others = people.stream().filter(person -> person.arrived() && !person.userId().equals(me))
                .map(person -> new PlanKudos.Person(person.userId(), person.organizer(), given.get(person.userId())))
                .toList();
        return new PlanKudos(KudosRules.open(mine.get(), clock.instant()), KudosRules.until(mine.get()), others);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<KudosKind, Long> receivedBy(UUID userId) {
        var counts = new EnumMap<KudosKind, Long>(Arrays.stream(KudosKind.values())
                .collect(Collectors.toMap(Function.identity(), kind -> 0L)));
        counts.putAll(kudos.countsOf(userId));
        return counts;
    }
}
