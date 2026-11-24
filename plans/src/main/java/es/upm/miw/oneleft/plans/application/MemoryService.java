package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.JoinRejectedException;
import es.upm.miw.oneleft.plans.domain.model.Memory;
import es.upm.miw.oneleft.plans.domain.model.MemoryForbiddenException;
import es.upm.miw.oneleft.plans.domain.model.MemoryFormat;
import es.upm.miw.oneleft.plans.domain.model.MemoryKey;
import es.upm.miw.oneleft.plans.domain.model.MemoryNotFoundException;
import es.upm.miw.oneleft.plans.domain.model.MemorySize;
import es.upm.miw.oneleft.plans.domain.model.MemoryUpload;
import es.upm.miw.oneleft.plans.domain.model.MemoryView;
import es.upm.miw.oneleft.plans.domain.model.PageQuery;
import es.upm.miw.oneleft.plans.domain.model.PageResult;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException;
import es.upm.miw.oneleft.plans.domain.model.PlanStatus;
import es.upm.miw.oneleft.plans.domain.model.ValidationException;
import es.upm.miw.oneleft.plans.domain.port.in.PlanMemoriesUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.MemoryRepository;
import es.upm.miw.oneleft.plans.domain.port.out.MemoryStorage;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * The album of a plan (HU-052). The group of the plan goes first in every operation: who is not its organizer or a
 * participant learns nothing about the album. Then the plan must be over to add photos, and the album and each
 * person have a limit.
 */
@Service
public class MemoryService implements PlanMemoriesUseCase {

    /** Long enough to upload a photo on a slow connection */
    static final Duration UPLOAD_VALIDITY = Duration.ofMinutes(5);
    /** The links to see the photos: a page keeps working for a while without asking again */
    static final Duration VIEW_VALIDITY = Duration.ofHours(1);
    /** An upload that was not confirmed in this time is not coming: it is deleted from the storage */
    static final Duration ABANDONED_AFTER = Duration.ofHours(1);

    private final PlanRepository plans;
    private final MemoryRepository memories;
    private final MemoryStorage storage;
    private final Clock clock;

    public MemoryService(PlanRepository plans, MemoryRepository memories, MemoryStorage storage, Clock clock) {
        this.plans = plans;
        this.memories = memories;
        this.storage = storage;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public MemoryUpload startUpload(UUID planId, UUID userId, String contentType, long size) {
        requireOver(planOfTheGroup(planId, userId));
        var format = MemoryFormat.of(contentType);
        MemorySize.require(size);
        requireRoom(planId, userId);
        // What this person uploaded to this album and never confirmed
        storage.deleteAbandoned(MemoryKey.prefixOf(userId, planId), memories.keysOf(planId, userId),
                clock.instant().minus(ABANDONED_AFTER));
        var key = MemoryKey.newFor(userId, planId, format);
        var url = storage.signUpload(key, format.contentType(), size, UPLOAD_VALIDITY);
        return new MemoryUpload(key, url, Map.of("Content-Type", format.contentType()),
                clock.instant().plus(UPLOAD_VALIDITY));
    }

    @Override
    @Transactional
    public MemoryView confirmUpload(UUID planId, UUID userId, String key) {
        requireOver(planOfTheGroup(planId, userId));
        MemoryKey.requireOwnedBy(key, userId, planId);
        var stored = storage.find(key)
                .orElseThrow(() -> new ValidationException("memory.notUploaded", "No photo was uploaded with that key"));
        try {
            stored.requireValid();
        } catch (ValidationException exception) {
            // Whatever it is, it does not stay in the storage
            storage.delete(key);
            throw exception;
        }
        memories.lockAlbum(planId);
        var confirmed = memories.findByKey(key);
        if (confirmed.isPresent()) {
            return view(confirmed.get());
        }
        requireRoom(planId, userId);
        return view(memories.save(Memory.uploaded(planId, userId, key, stored.size(), clock.instant())));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MemoryView> memories(UUID planId, UUID userId, PageQuery page) {
        planOfTheGroup(planId, userId);
        return memories.findByPlan(planId, page).map(this::view);
    }

    @Override
    @Transactional
    public void remove(UUID planId, UUID userId, UUID memoryId) {
        planOfTheGroup(planId, userId);
        var memory = memories.find(planId, memoryId).orElseThrow(() -> new MemoryNotFoundException(memoryId));
        if (!memory.isOf(userId)) {
            throw new MemoryForbiddenException(MemoryForbiddenException.NOT_YOURS,
                    "Only whoever uploaded a photo can remove it");
        }
        memories.delete(memoryId);
        storage.delete(memory.key());
    }

    private Plan planOfTheGroup(UUID planId, UUID userId) {
        var plan = plans.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));
        if (!plan.isMember(userId)) {
            throw new MemoryForbiddenException(MemoryForbiddenException.NOT_PARTICIPANT,
                    "The album of a plan is only for its organizer and its participants");
        }
        return plan;
    }

    /** By the time of the plan and not by its status, which the lifecycle task updates a little later (HU-007). */
    private void requireOver(Plan plan) {
        if (plan.status() == PlanStatus.CANCELLED || plan.endsAt().isAfter(clock.instant())) {
            throw new JoinRejectedException("memory.planNotFinished",
                    "Photos can be added once the plan is over, and not to a cancelled plan");
        }
    }

    private void requireRoom(UUID planId, UUID userId) {
        if (memories.countByPlanAndUser(planId, userId) >= Memory.MAX_PER_PERSON) {
            throw new JoinRejectedException("memory.personLimit",
                    "Each person can add up to " + Memory.MAX_PER_PERSON + " photos to an album");
        }
        if (memories.countByPlan(planId) >= Memory.MAX_PER_PLAN) {
            throw new JoinRejectedException("memory.planLimit",
                    "An album takes up to " + Memory.MAX_PER_PLAN + " photos");
        }
    }

    private MemoryView view(Memory memory) {
        return new MemoryView(memory, storage.signDownload(memory.key(), VIEW_VALIDITY));
    }
}
