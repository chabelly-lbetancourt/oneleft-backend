package es.upm.miw.oneleft.users.application;

import es.upm.miw.oneleft.users.domain.model.Identity;
import es.upm.miw.oneleft.users.domain.model.PhotoFormat;
import es.upm.miw.oneleft.users.domain.model.PhotoKey;
import es.upm.miw.oneleft.users.domain.model.PhotoSize;
import es.upm.miw.oneleft.users.domain.model.PhotoUpload;
import es.upm.miw.oneleft.users.domain.model.ValidationException;
import es.upm.miw.oneleft.users.domain.port.in.GetCurrentUserUseCase;
import es.upm.miw.oneleft.users.domain.port.in.ManagePhotoUseCase;
import es.upm.miw.oneleft.users.domain.port.out.PhotoRepository;
import es.upm.miw.oneleft.users.domain.port.out.PhotoStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PhotoService implements ManagePhotoUseCase {

    /** Long enough to upload a small photo on a slow connection */
    static final Duration UPLOAD_VALIDITY = Duration.ofMinutes(5);
    /** The links to see the photos: a page keeps working for a while without asking again */
    static final Duration VIEW_VALIDITY = Duration.ofHours(1);

    private final GetCurrentUserUseCase currentUser;
    private final PhotoRepository photos;
    private final PhotoStorage storage;
    private final Clock clock;

    public PhotoService(GetCurrentUserUseCase currentUser, PhotoRepository photos, PhotoStorage storage, Clock clock) {
        this.currentUser = currentUser;
        this.photos = photos;
        this.storage = storage;
        this.clock = clock;
    }

    @Override
    public PhotoUpload startUpload(Identity identity, String contentType, long size) {
        var format = PhotoFormat.of(contentType);
        PhotoSize.require(size);
        var key = PhotoKey.newFor(userId(identity), format);
        var url = storage.signUpload(key, format.contentType(), size, UPLOAD_VALIDITY);
        return new PhotoUpload(key, url, Map.of("Content-Type", format.contentType()),
                clock.instant().plus(UPLOAD_VALIDITY));
    }

    @Override
    @Transactional
    public URI confirmUpload(Identity identity, String key) {
        var userId = userId(identity);
        PhotoKey.requireOwnedBy(key, userId);
        storage.find(key)
                .orElseThrow(() -> new ValidationException("photo.notUploaded", "No photo was uploaded with that key"))
                .requireValid();
        photos.save(userId, key, clock.instant());
        // The previous photo and any upload that was never confirmed
        storage.deleteUnder(PhotoKey.prefixOf(userId), key);
        return storage.signDownload(key, VIEW_VALIDITY);
    }

    @Override
    @Transactional
    public void removeMyPhoto(Identity identity) {
        var userId = userId(identity);
        photos.delete(userId);
        storage.deleteUnder(PhotoKey.prefixOf(userId), null);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, URI> photosOf(Collection<UUID> userIds) {
        var people = new LinkedHashSet<>(userIds);
        if (people.size() > MAX_PEOPLE) {
            throw new ValidationException("photo.tooManyPeople", "At most " + MAX_PEOPLE + " people at a time");
        }
        return photos.keysOf(people).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> storage.signDownload(entry.getValue(), VIEW_VALIDITY)));
    }

    private UUID userId(Identity identity) {
        return currentUser.currentUser(identity).id();
    }
}
