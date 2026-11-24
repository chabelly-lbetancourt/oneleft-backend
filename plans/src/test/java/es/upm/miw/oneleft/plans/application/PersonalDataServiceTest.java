package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.MemoryFormat;
import es.upm.miw.oneleft.plans.domain.model.MemoryKey;
import es.upm.miw.oneleft.plans.domain.model.StoredMemory;
import es.upm.miw.oneleft.plans.domain.port.out.PersonalData;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-045 with the photos of HU-052: the erasure of an account also deletes them from the storage. */
class PersonalDataServiceTest {

    private static final StoredMemory PHOTO = new StoredMemory(1000, "image/webp", MemoryFormat.WEBP);

    private final UUID user = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();
    private final UUID plan = UUID.randomUUID();
    private final List<UUID> erased = new ArrayList<>();
    private final MemoryServiceTest.InMemoryStorage storage = new MemoryServiceTest.InMemoryStorage();
    private final PersonalDataService service = new PersonalDataService(new PersonalData() {
        @Override
        public Map<String, List<Map<String, Object>>> of(UUID userId) {
            return Map.of("memories", List.of(Map.of("user", userId)));
        }

        @Override
        public void erase(UUID userId) {
            erased.add(userId);
        }
    }, storage);

    @Test
    void erasingAnAccountDeletesTheirPhotosOfEveryAlbumAndNobodyElses() {
        var mine = MemoryKey.newFor(user, plan, MemoryFormat.WEBP);
        var inAnotherPlan = MemoryKey.newFor(user, UUID.randomUUID(), MemoryFormat.WEBP);
        var notMine = MemoryKey.newFor(other, plan, MemoryFormat.WEBP);
        List.of(mine, inAnotherPlan, notMine).forEach(key -> storage.upload(key, PHOTO, Instant.EPOCH));

        service.erase(user);
        // The event can arrive twice
        service.erase(user);

        assertThat(erased).containsExactly(user, user);
        assertThat(storage.objects).containsOnlyKeys(notMine);
    }

    @Test
    void theRestIsErasedEvenIfTheStorageDoesNotAnswer() {
        storage.down = true;

        service.erase(user);

        assertThat(erased).containsExactly(user);
    }

    @Test
    void theExportIsWhatTheStorageOfThisServiceKeeps() {
        assertThat(service.export(user)).containsOnlyKeys("memories");
    }
}
