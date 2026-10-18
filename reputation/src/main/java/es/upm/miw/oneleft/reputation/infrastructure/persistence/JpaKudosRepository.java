package es.upm.miw.oneleft.reputation.infrastructure.persistence;

import es.upm.miw.oneleft.reputation.domain.model.Kudos;
import es.upm.miw.oneleft.reputation.domain.model.KudosKind;
import es.upm.miw.oneleft.reputation.domain.port.out.KudosStore;
import org.springframework.stereotype.Repository;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Output adapter: the kudos in PostgreSQL (HU-060). */
@Repository
public class JpaKudosRepository implements KudosStore {

    private final SpringDataKudosRepository kudos;

    public JpaKudosRepository(SpringDataKudosRepository kudos) {
        this.kudos = kudos;
    }

    @Override
    public boolean save(Kudos given) {
        return kudos.give(given.giverId(), given.receiverId(), given.planId(), given.kind().name(),
                given.givenAt()) == 1;
    }

    @Override
    public List<Kudos> givenBy(UUID giverId, UUID planId) {
        return kudos.findByIdGiverIdAndIdPlanId(giverId, planId).stream().map(KudosEntity::toDomain).toList();
    }

    @Override
    public Map<KudosKind, Long> countsOf(UUID receiverId) {
        var counts = new EnumMap<KudosKind, Long>(KudosKind.class);
        kudos.countsOf(receiverId).forEach(count -> counts.put(count.getKind(), count.getTotal()));
        return counts;
    }
}
