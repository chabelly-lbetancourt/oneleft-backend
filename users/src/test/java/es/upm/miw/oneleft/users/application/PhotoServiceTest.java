package es.upm.miw.oneleft.users.application;

import es.upm.miw.oneleft.users.domain.model.Identity;
import es.upm.miw.oneleft.users.domain.model.PhotoFormat;
import es.upm.miw.oneleft.users.domain.model.PhotoKey;
import es.upm.miw.oneleft.users.domain.model.PhotoSize;
import es.upm.miw.oneleft.users.domain.model.StoredPhoto;
import es.upm.miw.oneleft.users.domain.model.ValidationException;
import es.upm.miw.oneleft.users.domain.port.in.ManagePhotoUseCase;
import es.upm.miw.oneleft.users.domain.port.out.PhotoRepository;
import es.upm.miw.oneleft.users.domain.port.out.PhotoStorage;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhotoServiceTest {

    /** In-memory storage: what was uploaded, and the links it signed */
    static class InMemoryStorage implements PhotoStorage {
        final Map<String, StoredPhoto> objects = new HashMap<>();
        final List<String> signedUploads = new ArrayList<>();

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
        public Optional<StoredPhoto> find(String key) {
            return Optional.ofNullable(objects.get(key));
        }

        @Override
        public void deleteUnder(String prefix, String keep) {
            objects.keySet().removeIf(key -> key.startsWith(prefix) && !key.equals(keep));
        }
    }

    static class InMemoryPhotos implements PhotoRepository {
        final Map<UUID, String> keys = new HashMap<>();

        @Override
        public Optional<String> keyOf(UUID userId) {
            return Optional.ofNullable(keys.get(userId));
        }

        @Override
        public Map<UUID, String> keysOf(Collection<UUID> userIds) {
            return userIds.stream().filter(keys::containsKey).collect(Collectors.toMap(id -> id, keys::get));
        }

        @Override
        public void save(UUID userId, String key, Instant updatedAt) {
            keys.put(userId, key);
        }

        @Override
        public void delete(UUID userId) {
            keys.remove(userId);
        }
    }

    private static final Instant NOW = Instant.parse("2026-10-14T10:00:00Z");

    private final InMemoryStorage storage = new InMemoryStorage();
    private final InMemoryPhotos photos = new InMemoryPhotos();
    private final PhotoService service = new PhotoService(new CurrentUserService(), photos, storage,
            Clock.fixed(NOW, ZoneOffset.UTC));
    private final UUID anaId = UUID.randomUUID();
    private final Identity ana = new Identity(anaId.toString(), "Ana Test", "ana@oneleft.dev", Set.of("user"));

    private static void assertCode(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ValidationException.class,
                exception -> assertThat(exception.code()).isEqualTo(code));
    }

    private String uploaded(String contentType, long size) {
        var key = service.startUpload(ana, contentType, size).key();
        storage.objects.put(key, new StoredPhoto(size, contentType));
        return key;
    }

    @Test
    void theUploadLinkIsSignedForExactlyThatPhotoForFiveMinutes() {
        var upload = service.startUpload(ana, "image/webp", 48_000);

        assertThat(upload.key()).startsWith(PhotoKey.prefixOf(anaId)).endsWith(".webp");
        assertThat(upload.headers()).containsExactly(Map.entry("Content-Type", "image/webp"));
        assertThat(upload.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
        assertThat(storage.signedUploads).containsExactly(upload.key() + " image/webp 48000 PT5M");
    }

    @Test
    void noLinkForSomethingThatIsNotAPhotoWithinTheLimits() {
        assertCode(() -> service.startUpload(ana, "image/gif", 48_000), "photo.format");
        assertCode(() -> service.startUpload(ana, "image/jpeg", 0), "photo.empty");
        assertCode(() -> service.startUpload(ana, "image/jpeg", PhotoSize.MAX_BYTES + 1), "photo.tooBig");
        assertThat(storage.signedUploads).isEmpty();
    }

    @Test
    void confirmingKeepsTheNewPhotoAndDeletesThePreviousOneAndAnyUnconfirmedUpload() {
        var previous = uploaded("image/webp", 40_000);
        service.confirmUpload(ana, previous);
        var forgotten = uploaded("image/webp", 41_000);
        var current = uploaded("image/jpeg", 90_000);

        var link = service.confirmUpload(ana, current);

        assertThat(photos.keys).containsEntry(anaId, current);
        assertThat(storage.objects).containsOnlyKeys(current).doesNotContainKeys(previous, forgotten);
        assertThat(link).hasToString("https://storage.test/" + current + "?for=PT1H");
    }

    @Test
    void nothingIsConfirmedThatIsNotMineOrWasNotUploadedOrIsNotValid() {
        var someoneElses = PhotoKey.newFor(UUID.randomUUID(), PhotoFormat.WEBP);
        assertCode(() -> service.confirmUpload(ana, someoneElses), "photo.key");
        assertCode(() -> service.confirmUpload(ana, service.startUpload(ana, "image/webp", 1000).key()),
                "photo.notUploaded");
        var tooBig = service.startUpload(ana, "image/jpeg", 1000).key();
        storage.objects.put(tooBig, new StoredPhoto(PhotoSize.MAX_BYTES + 1, "image/jpeg"));
        assertCode(() -> service.confirmUpload(ana, tooBig), "photo.tooBig");

        assertThat(photos.keys).isEmpty();
    }

    @Test
    void removingMyPhotoDeletesItFromTheStorageToo() {
        service.confirmUpload(ana, uploaded("image/webp", 40_000));

        service.removeMyPhoto(ana);
        service.removeMyPhoto(ana);

        assertThat(photos.keys).isEmpty();
        assertThat(storage.objects).isEmpty();
    }

    @Test
    void thePhotosOfAListOnlyIncludeWhoeverHasOne() {
        var key = uploaded("image/webp", 40_000);
        service.confirmUpload(ana, key);
        var lucia = UUID.randomUUID();

        assertThat(service.photosOf(List.of(anaId, lucia, anaId)))
                .containsExactly(Map.entry(anaId, URI.create("https://storage.test/" + key + "?for=PT1H")));
    }

    @Test
    void atMostFiftyPeopleAtATime() {
        var fifty = IntStream.range(0, ManagePhotoUseCase.MAX_PEOPLE).mapToObj(i -> UUID.randomUUID()).toList();
        assertThat(service.photosOf(fifty)).isEmpty();

        var more = new ArrayList<>(fifty);
        more.add(UUID.randomUUID());
        assertCode(() -> service.photosOf(more), "photo.tooManyPeople");
    }
}
