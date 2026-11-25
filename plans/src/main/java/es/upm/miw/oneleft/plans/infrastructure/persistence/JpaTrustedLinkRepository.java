package es.upm.miw.oneleft.plans.infrastructure.persistence;

import es.upm.miw.oneleft.plans.domain.model.TrustedLink;
import es.upm.miw.oneleft.plans.domain.port.out.TrustedLinkRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Output adapter: the links for trusted contacts (HU-034), in PostgreSQL. */
@Repository
class JpaTrustedLinkRepository implements TrustedLinkRepository {

    private final SpringDataTrustedLinkRepository links;

    JpaTrustedLinkRepository(SpringDataTrustedLinkRepository links) {
        this.links = links;
    }

    @Override
    @Transactional
    public void lockPlan(UUID planId) {
        links.lockPlan(planId);
    }

    @Override
    @Transactional
    public TrustedLink save(TrustedLink link) {
        return links.save(new TrustedLinkEntity(link)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TrustedLink> findByTokenHash(String tokenHash) {
        return links.findByTokenHash(tokenHash).map(TrustedLinkEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrustedLink> findActive(UUID planId, UUID userId, Instant now) {
        return links.findByPlanIdAndUserIdAndExpiresAtAfterOrderByCreatedAtAscIdAsc(planId, userId, now).stream()
                .map(TrustedLinkEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public void delete(UUID linkId, UUID planId, UUID userId) {
        links.deleteOwn(linkId, planId, userId);
    }

    @Override
    @Transactional
    public int deleteExpired(Instant now) {
        return links.deleteExpired(now);
    }
}
