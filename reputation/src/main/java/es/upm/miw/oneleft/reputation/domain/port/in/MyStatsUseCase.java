package es.upm.miw.oneleft.reputation.domain.port.in;

import es.upm.miw.oneleft.reputation.domain.model.MyStats;
import es.upm.miw.oneleft.reputation.domain.model.StatsQuery;

import java.util.UUID;

/** My statistics (HU-043): how much and what I play. */
public interface MyStatsUseCase {

    MyStats myStats(UUID me, StatsQuery query);
}
