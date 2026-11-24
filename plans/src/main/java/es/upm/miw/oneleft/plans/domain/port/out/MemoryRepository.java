package es.upm.miw.oneleft.plans.domain.port.out;

import es.upm.miw.oneleft.plans.domain.model.Memory;
import es.upm.miw.oneleft.plans.domain.model.PageQuery;
import es.upm.miw.oneleft.plans.domain.model.PageResult;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** The photos of the albums of the plans (HU-052): whose each one is and where it is in the storage. */
public interface MemoryRepository {

    /**
     * Makes whoever adds a photo to this album wait for their turn until the end of the transaction, so that two
     * photos at once cannot both take the last place.
     */
    void lockAlbum(UUID planId);

    long countByPlan(UUID planId);

    long countByPlanAndUser(UUID planId, UUID userId);

    Memory save(Memory memory);

    Optional<Memory> findByKey(String key);

    Optional<Memory> find(UUID planId, UUID memoryId);

    /** The album of a plan, newest first. */
    PageResult<Memory> findByPlan(UUID planId, PageQuery page);

    /** The keys of the photos a person has in the album of a plan. */
    Set<String> keysOf(UUID planId, UUID userId);

    void delete(UUID memoryId);
}
