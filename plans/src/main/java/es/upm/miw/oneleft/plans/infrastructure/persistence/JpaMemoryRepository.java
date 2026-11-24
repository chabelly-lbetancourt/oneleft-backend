package es.upm.miw.oneleft.plans.infrastructure.persistence;

import es.upm.miw.oneleft.plans.domain.model.Memory;
import es.upm.miw.oneleft.plans.domain.model.PageQuery;
import es.upm.miw.oneleft.plans.domain.model.PageResult;
import es.upm.miw.oneleft.plans.domain.port.out.MemoryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Output adapter: the photos of the albums of the plans (HU-052), in PostgreSQL. */
@Repository
class JpaMemoryRepository implements MemoryRepository {

    private final SpringDataMemoryRepository memories;

    JpaMemoryRepository(SpringDataMemoryRepository memories) {
        this.memories = memories;
    }

    @Override
    @Transactional
    public void lockAlbum(UUID planId) {
        memories.lockPlan(planId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByPlan(UUID planId) {
        return memories.countByPlanId(planId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByPlanAndUser(UUID planId, UUID userId) {
        return memories.countByPlanIdAndUserId(planId, userId);
    }

    @Override
    @Transactional
    public Memory save(Memory memory) {
        return memories.save(new MemoryEntity(memory)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Memory> findByKey(String key) {
        return memories.findByObjectKey(key).map(MemoryEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Memory> find(UUID planId, UUID memoryId) {
        return memories.findByIdAndPlanId(memoryId, planId).map(MemoryEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Memory> findByPlan(UUID planId, PageQuery page) {
        var found = memories.findByPlanIdOrderByUploadedAtDescIdDesc(planId, PageRequest.of(page.page(), page.size()));
        return new PageResult<>(found.map(MemoryEntity::toDomain).getContent(), page.page(), page.size(),
                found.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> keysOf(UUID planId, UUID userId) {
        return memories.keysOf(planId, userId);
    }

    @Override
    @Transactional
    public void delete(UUID memoryId) {
        memories.deleteById(memoryId);
    }
}
