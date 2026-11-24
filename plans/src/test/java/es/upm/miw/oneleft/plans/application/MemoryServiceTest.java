package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.JoinRejectedException;
import es.upm.miw.oneleft.plans.domain.model.Memory;
import es.upm.miw.oneleft.plans.domain.model.MemoryForbiddenException;
import es.upm.miw.oneleft.plans.domain.model.MemoryFormat;
import es.upm.miw.oneleft.plans.domain.model.MemoryKey;
import es.upm.miw.oneleft.plans.domain.model.MemoryNotFoundException;
import es.upm.miw.oneleft.plans.domain.model.MemoryStorageUnavailableException;
import es.upm.miw.oneleft.plans.domain.model.MemoryView;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.PageQuery;
import es.upm.miw.oneleft.plans.domain.model.PageResult;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException;
import es.upm.miw.oneleft.plans.domain.model.PlanStatus;
import es.upm.miw.oneleft.plans.domain.model.StoredMemory;
import es.upm.miw.oneleft.plans.domain.model.ValidationException;
import es.upm.miw.oneleft.plans.domain.port.out.MemoryRepository;
import es.upm.miw.oneleft.plans.domain.port.out.MemoryStorage;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-052: only the group of a plan that is over, real photos within the limits, and each one removes their own. */
class MemoryServiceTest {

    private static final UUID LUCIA = UUID.randomUUID();
    private static final UUID DIEGO = UUID.randomUUID();
    private static final UUID MARTA = UUID.randomUUID();

    /** In-memory storage: what was uploaded and when, and the links it signed */
    static class InMemoryStorage implements MemoryStorage {
        final Map<String, StoredMemory> objects = new HashMap<>();
        final Map<String, Instant> uploadedAt = new HashMap<>();
        final List<String> signedUploads = new ArrayList<>();
        boolean down;

        void upload(String key, StoredMemory object, Instant at) {
            objects.put(key, object);
            uploadedAt.put(key, at);
        }

        @Override
        public URI signUpload(String key, String contentType, long size, Duration validity) {
            signedUploads.add(key + " " + contentType + " " + size + " " + validity);
            return URI.create("https://storage.test/" + key + "?upload");
        }

        @Override
        public URI signDownload(String key, Duration validity) {
            return URI.create("https://storage.test/" + key + "?for=" + validity);
        }

        @Override
        public Optional<StoredMemory> find(String key) {
            return Optional.ofNullable(objects.get(key));
        }

        @Override
        public void delete(String key) {
            objects.remove(key);
        }

        @Override
        public void deleteUnder(String prefix) {
            if (down) {
                throw new MemoryStorageUnavailableException(new IllegalStateException("down"));
            }
            objects.keySet().removeIf(key -> key.startsWith(prefix));
        }

        @Override
        public void deleteAbandoned(String prefix, Set<String> keep, Instant before) {
            objects.keySet().removeIf(key -> key.startsWith(prefix) && !keep.contains(key)
                    && uploadedAt.get(key).isBefore(before));
        }
    }

    static class InMemoryMemories implements MemoryRepository {
        final Map<UUID, Memory> data = new HashMap<>();
        final List<UUID> locked = new ArrayList<>();

        @Override
        public void lockAlbum(UUID planId) {
            locked.add(planId);
        }

        @Override
        public long countByPlan(UUID planId) {
            return data.values().stream().filter(memory -> memory.planId().equals(planId)).count();
        }

        @Override
        public long countByPlanAndUser(UUID planId, UUID userId) {
            return data.values().stream().filter(memory -> memory.planId().equals(planId) && memory.isOf(userId))
                    .count();
        }

        @Override
        public Memory save(Memory memory) {
            data.put(memory.id(), memory);
            return memory;
        }

        @Override
        public Optional<Memory> findByKey(String key) {
            return data.values().stream().filter(memory -> memory.key().equals(key)).findFirst();
        }

        @Override
        public Optional<Memory> find(UUID planId, UUID memoryId) {
            return Optional.ofNullable(data.get(memoryId)).filter(memory -> memory.planId().equals(planId));
        }

        @Override
        public PageResult<Memory> findByPlan(UUID planId, PageQuery page) {
            var all = data.values().stream().filter(memory -> memory.planId().equals(planId))
                    .sorted(Comparator.comparing(Memory::uploadedAt).reversed()).toList();
            return new PageResult<>(all.stream().skip((long) page.page() * page.size()).limit(page.size()).toList(),
                    page.page(), page.size(), all.size());
        }

        @Override
        public Set<String> keysOf(UUID planId, UUID userId) {
            return data.values().stream().filter(memory -> memory.planId().equals(planId) && memory.isOf(userId))
                    .map(Memory::key).collect(Collectors.toSet());
        }

        @Override
        public void delete(UUID memoryId) {
            data.remove(memoryId);
        }
    }

    private final JoinPlanServiceTest.Plans plans = new JoinPlanServiceTest.Plans();
    private final InMemoryMemories memories = new InMemoryMemories();
    private final InMemoryStorage storage = new InMemoryStorage();
    private final Plan plan = plans.save(Plan.publish(ana(), Activity.PADEL, "Padel 2 vs 2", null,
            new MeetingPoint("Courts", 40.39, -3.62), NOW.plus(Duration.ofHours(1)), 3, null, CLOCK)
            .join(LUCIA, "Lucía", CLOCK).join(DIEGO, "Diego", CLOCK));
    private final UUID ana = plan.organizer().id();
    private final Instant over = plan.endsAt();
    private final MemoryService service = serviceAt(over);

    private MemoryService serviceAt(Instant now) {
        return new MemoryService(plans, memories, storage, Clock.fixed(now, ZoneOffset.UTC));
    }

    private static void assertRejected(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(JoinRejectedException.class,
                error -> assertThat(error.code()).isEqualTo(code));
    }

    private static void assertInvalid(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ValidationException.class,
                error -> assertThat(error.code()).isEqualTo(code));
    }

    private static void assertForbidden(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(MemoryForbiddenException.class,
                error -> assertThat(error.code()).isEqualTo(code));
    }

    /** As the web does: asks for the link, uploads a real photo and adds it to the album */
    private MemoryView add(UUID who, Instant at) {
        var upload = serviceAt(at).startUpload(plan.id(), who, "image/webp", 1000);
        storage.upload(upload.key(), new StoredMemory(1000, "image/webp", MemoryFormat.WEBP), at);
        return serviceAt(at).confirmUpload(plan.id(), who, upload.key());
    }

    @Test
    void someoneOfTheGroupGetsALinkForExactlyTheirPhotoOnceThePlanIsOver() {
        var upload = service.startUpload(plan.id(), LUCIA, "image/jpeg", 300_000);

        assertThat(upload.key()).startsWith(MemoryKey.prefixOf(LUCIA, plan.id())).endsWith(".jpg");
        assertThat(upload.headers()).containsExactly(Map.entry("Content-Type", "image/jpeg"));
        assertThat(upload.expiresAt()).isEqualTo(over.plus(Duration.ofMinutes(5)));
        assertThat(storage.signedUploads).containsExactly(upload.key() + " image/jpeg 300000 PT5M");
    }

    @Test
    void theUploadedPhotoIsAddedToTheAlbumAndTheWholeGroupSeesItNewestFirst() {
        var first = add(LUCIA, over);
        var second = add(ana, over.plusSeconds(60));

        assertThat(first.memory()).satisfies(memory -> {
            assertThat(memory.planId()).isEqualTo(plan.id());
            assertThat(memory.userId()).isEqualTo(LUCIA);
            assertThat(memory.size()).isEqualTo(1000);
            assertThat(memory.uploadedAt()).isEqualTo(over);
        });
        assertThat(first.url()).hasToString("https://storage.test/" + first.memory().key() + "?for=PT1H");
        assertThat(memories.locked).containsExactly(plan.id(), plan.id());

        var album = service.memories(plan.id(), DIEGO, new PageQuery(0, 20));
        assertThat(album.items()).containsExactly(second, first);
        assertThat(album.total()).isEqualTo(2);
        assertThat(service.memories(plan.id(), DIEGO, new PageQuery(1, 1)).items()).containsExactly(first);
    }

    @Test
    void whoIsNotInTheGroupNeitherUploadsNorSeesNorRemoves() {
        var memory = add(LUCIA, over).memory();
        var key = MemoryKey.newFor(MARTA, plan.id(), MemoryFormat.WEBP);
        storage.upload(key, new StoredMemory(1000, "image/webp", MemoryFormat.WEBP), over);

        assertForbidden(() -> service.startUpload(plan.id(), MARTA, "image/webp", 1000), "plan.notParticipant");
        assertForbidden(() -> service.confirmUpload(plan.id(), MARTA, key), "plan.notParticipant");
        assertForbidden(() -> service.memories(plan.id(), MARTA, new PageQuery(0, 20)), "plan.notParticipant");
        assertForbidden(() -> service.remove(plan.id(), MARTA, memory.id()), "plan.notParticipant");
        assertThat(storage.signedUploads).hasSize(1);
        assertThat(memories.data).containsOnlyKeys(memory.id());
    }

    @Test
    void aPlanThatDoesNotExistHasNoAlbum() {
        var unknown = UUID.randomUUID();

        assertThatThrownBy(() -> service.startUpload(unknown, LUCIA, "image/webp", 1000))
                .isInstanceOf(PlanNotFoundException.class);
        assertThatThrownBy(() -> service.memories(unknown, LUCIA, new PageQuery(0, 20)))
                .isInstanceOf(PlanNotFoundException.class);
    }

    @Test
    void photosAreAddedOnlyOnceThePlanIsOverAndNeverToACancelledOne() {
        var during = serviceAt(over.minusSeconds(1));
        assertRejected(() -> during.startUpload(plan.id(), LUCIA, "image/webp", 1000), "memory.planNotFinished");
        assertRejected(() -> during.confirmUpload(plan.id(), LUCIA, "any"), "memory.planNotFinished");
        assertThat(during.memories(plan.id(), LUCIA, new PageQuery(0, 20)).items()).isEmpty();

        var cancelled = plans.save(new Plan(UUID.randomUUID(), plan.organizer(), Activity.PADEL, "Cancelled", null,
                plan.meetingPoint(), plan.startsAt(), 3, 0, null, PlanStatus.CANCELLED, NOW));
        assertRejected(() -> service.startUpload(cancelled.id(), ana, "image/webp", 1000), "memory.planNotFinished");
    }

    @Test
    void thePhotoMustBeWebpOrJpegAndWithinTheSize() {
        assertInvalid(() -> service.startUpload(plan.id(), LUCIA, "image/gif", 1000), "memory.format");
        assertInvalid(() -> service.startUpload(plan.id(), LUCIA, "image/webp", 0), "memory.empty");
        assertInvalid(() -> service.startUpload(plan.id(), LUCIA, "image/webp", 3_000_000), "memory.tooBig");
        assertThat(storage.signedUploads).isEmpty();
    }

    @Test
    void aFileThatIsNotReallyAPhotoIsRejectedAndDeletedFromTheStorage() {
        var upload = service.startUpload(plan.id(), LUCIA, "image/webp", 1000);
        // It says image/webp, but its first bytes are something else
        storage.upload(upload.key(), new StoredMemory(1000, "image/webp", null), over);

        assertInvalid(() -> service.confirmUpload(plan.id(), LUCIA, upload.key()), "memory.notAPhoto");

        assertThat(storage.objects).isEmpty();
        assertThat(memories.data).isEmpty();
    }

    @Test
    void onlyAKeyGivenToMeForThisPlanWithSomethingUploadedIsConfirmed() {
        var upload = service.startUpload(plan.id(), LUCIA, "image/webp", 1000);
        var ofDiego = MemoryKey.newFor(DIEGO, plan.id(), MemoryFormat.WEBP);
        storage.upload(ofDiego, new StoredMemory(1000, "image/webp", MemoryFormat.WEBP), over);

        assertInvalid(() -> service.confirmUpload(plan.id(), LUCIA, ofDiego), "memory.key");
        assertInvalid(() -> service.confirmUpload(plan.id(), LUCIA, null), "memory.key");
        assertInvalid(() -> service.confirmUpload(plan.id(), LUCIA, upload.key()), "memory.notUploaded");
        assertThat(memories.data).isEmpty();
    }

    @Test
    void confirmingTheSamePhotoAgainReturnsItAndAddsNothing() {
        var first = add(LUCIA, over);

        var again = serviceAt(over.plusSeconds(600)).confirmUpload(plan.id(), LUCIA, first.memory().key());

        assertThat(again.memory()).isEqualTo(first.memory());
        assertThat(memories.data).hasSize(1);
    }

    @Test
    void eachPersonHasALimitOfPhotosInAnAlbum() {
        for (int i = 0; i < Memory.MAX_PER_PERSON; i++) {
            add(LUCIA, over.plusSeconds(i));
        }
        var key = MemoryKey.newFor(LUCIA, plan.id(), MemoryFormat.WEBP);
        storage.upload(key, new StoredMemory(1000, "image/webp", MemoryFormat.WEBP), over);

        assertRejected(() -> service.startUpload(plan.id(), LUCIA, "image/webp", 1000), "memory.personLimit");
        assertRejected(() -> service.confirmUpload(plan.id(), LUCIA, key), "memory.personLimit");
        assertThat(add(DIEGO, over).memory().userId()).isEqualTo(DIEGO);
    }

    @Test
    void anAlbumHasALimitOfPhotos() {
        for (int i = 0; i < Memory.MAX_PER_PLAN; i++) {
            memories.save(Memory.uploaded(plan.id(), UUID.randomUUID(), "memories/other/" + i, 1000, over));
        }

        assertRejected(() -> service.startUpload(plan.id(), LUCIA, "image/webp", 1000), "memory.planLimit");
    }

    @Test
    void eachOneRemovesOnlyTheirOwnPhotosAlsoFromTheStorage() {
        var ofLucia = add(LUCIA, over).memory();
        var ofDiego = add(DIEGO, over).memory();

        assertForbidden(() -> service.remove(plan.id(), ana, ofLucia.id()), "memory.notYours");
        assertForbidden(() -> service.remove(plan.id(), DIEGO, ofLucia.id()), "memory.notYours");
        service.remove(plan.id(), LUCIA, ofLucia.id());

        assertThat(memories.data).containsOnlyKeys(ofDiego.id());
        assertThat(storage.objects).containsOnlyKeys(ofDiego.key());
        assertThatThrownBy(() -> service.remove(plan.id(), LUCIA, ofLucia.id()))
                .isInstanceOf(MemoryNotFoundException.class);
    }

    @Test
    void aPhotoOfAnotherAlbumIsNotFoundInThisOne() {
        var other = plans.save(Plan.publish(ana(), Activity.PADEL, "Another plan", null,
                new MeetingPoint("Courts", 40.39, -3.62), NOW.plus(Duration.ofHours(1)), 3, null, CLOCK)
                .join(LUCIA, "Lucía", CLOCK));
        var memory = add(LUCIA, over).memory();

        assertThatThrownBy(() -> service.remove(other.id(), LUCIA, memory.id()))
                .isInstanceOf(MemoryNotFoundException.class);
        assertThat(memories.data).containsKey(memory.id());
    }

    @Test
    void uploadsNeverConfirmedAreDeletedTheNextTimeThatPersonUploadsThere() {
        var kept = add(LUCIA, over).memory();
        var abandoned = service.startUpload(plan.id(), LUCIA, "image/webp", 1000).key();
        var recent = service.startUpload(plan.id(), LUCIA, "image/webp", 1000).key();
        var ofDiego = service.startUpload(plan.id(), DIEGO, "image/webp", 1000).key();
        var photo = new StoredMemory(1000, "image/webp", MemoryFormat.WEBP);
        storage.upload(abandoned, photo, over);
        storage.upload(ofDiego, photo, over);
        storage.upload(recent, photo, over.plus(Duration.ofMinutes(90)));

        serviceAt(over.plus(Duration.ofHours(2))).startUpload(plan.id(), LUCIA, "image/webp", 1000);

        assertThat(storage.objects).containsOnlyKeys(kept.key(), recent, ofDiego);
    }
}
