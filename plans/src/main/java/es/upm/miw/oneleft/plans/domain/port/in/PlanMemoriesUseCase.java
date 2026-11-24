package es.upm.miw.oneleft.plans.domain.port.in;

import es.upm.miw.oneleft.plans.domain.model.MemoryUpload;
import es.upm.miw.oneleft.plans.domain.model.MemoryView;
import es.upm.miw.oneleft.plans.domain.model.PageQuery;
import es.upm.miw.oneleft.plans.domain.model.PageResult;

import java.util.UUID;

/**
 * The album of a plan (HU-052): once the plan is over, its organizer and its participants share photos of it. Nobody
 * else uploads or sees them. A photo goes straight from the device to the storage with a signed link, as the profile
 * photos do (HU-020), and is seen through signed links that expire.
 * <p>
 * Every operation throws {@link es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException} if there is no such plan
 * and {@link es.upm.miw.oneleft.plans.domain.model.MemoryForbiddenException} if the person is not in its group.
 */
public interface PlanMemoriesUseCase {

    /**
     * A signed link to upload a photo to the album.
     *
     * @throws es.upm.miw.oneleft.plans.domain.model.ValidationException   if the photo is not WebP or JPEG or is too
     *                                                                     big
     * @throws es.upm.miw.oneleft.plans.domain.model.JoinRejectedException if the plan is not over, or the album or
     *                                                                     the person has no room for more photos
     */
    MemoryUpload startUpload(UUID planId, UUID userId, String contentType, long size);

    /**
     * Adds the uploaded photo to the album. Confirming the same photo again returns it as it is.
     *
     * @throws es.upm.miw.oneleft.plans.domain.model.ValidationException   if the key was not given to this person for
     *                                                                     this plan, nothing was uploaded there or
     *                                                                     it is not really a photo
     * @throws es.upm.miw.oneleft.plans.domain.model.JoinRejectedException as in {@link #startUpload}
     */
    MemoryView confirmUpload(UUID planId, UUID userId, String key);

    /** A page of the album, newest first. */
    PageResult<MemoryView> memories(UUID planId, UUID userId, PageQuery page);

    /**
     * Removes a photo of mine.
     *
     * @throws es.upm.miw.oneleft.plans.domain.model.MemoryNotFoundException  if the album has no such photo
     * @throws es.upm.miw.oneleft.plans.domain.model.MemoryForbiddenException if somebody else uploaded it
     */
    void remove(UUID planId, UUID userId, UUID memoryId);
}
