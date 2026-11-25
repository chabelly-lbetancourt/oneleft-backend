package es.upm.miw.oneleft.reputation.application;

import es.upm.miw.oneleft.reputation.domain.model.MyStats;
import es.upm.miw.oneleft.reputation.domain.model.StatsQuery;
import es.upm.miw.oneleft.reputation.domain.model.StatsRules;
import es.upm.miw.oneleft.reputation.domain.port.in.MyStatsUseCase;
import es.upm.miw.oneleft.reputation.domain.port.out.TrackRecords;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * My statistics (HU-043), worked out when asked from the track record that the attendance events already fill
 * (HU-044): no table of its own, so there is nothing to keep in step nor to erase with the account.
 */
@Service
@Transactional(readOnly = true)
public class StatsService implements MyStatsUseCase {

    private final TrackRecords trackRecords;
    private final Clock clock;

    public StatsService(TrackRecords trackRecords, Clock clock) {
        this.trackRecords = trackRecords;
        this.clock = clock;
    }

    @Override
    public MyStats myStats(UUID me, StatsQuery query) {
        var now = clock.instant();
        var record = trackRecords.of(me);
        var months = trackRecords.monthsOf(me, StatsRules.since(now, query.months()));
        return new MyStats(record.completed(), record.organizedHeld(), trackRecords.joinedOf(me),
                trackRecords.topActivitiesOf(me, query.activities()),
                StatsRules.series(months, now, query.months()));
    }
}
