package es.upm.miw.oneleft.reputation.infrastructure.persistence;

import es.upm.miw.oneleft.reputation.domain.model.Endorsement;
import es.upm.miw.oneleft.reputation.domain.model.EndorsementCount;
import es.upm.miw.oneleft.reputation.domain.port.out.EndorsementStore;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Output adapter: the endorsements in PostgreSQL (HU-061). */
@Repository
public class JpaEndorsementRepository implements EndorsementStore {

    private final SpringDataEndorsementRepository endorsements;

    public JpaEndorsementRepository(SpringDataEndorsementRepository endorsements) {
        this.endorsements = endorsements;
    }

    @Override
    public boolean save(Endorsement given) {
        return endorsements.give(given.endorserId(), given.receiverId(), given.activity(), given.level().name(),
                given.givenAt()) == 1;
    }

    @Override
    public List<EndorsementCount> endorsementCountsOf(UUID receiverId) {
        return endorsements.countsOf(receiverId).stream()
                .map(count -> new EndorsementCount(count.getActivity(), count.getVouched(), count.getTotal())).toList();
    }

    @Override
    public List<Endorsement> endorsementsBy(UUID endorserId, UUID receiverId) {
        return endorsements.findByIdEndorserIdAndIdReceiverId(endorserId, receiverId).stream()
                .map(EndorsementEntity::toDomain).toList();
    }

    @Override
    public int countEndorsements(UUID endorserId, UUID receiverId, Instant from, Instant to) {
        return Math.toIntExact(endorsements.countGiven(endorserId, receiverId, from, to));
    }
}
