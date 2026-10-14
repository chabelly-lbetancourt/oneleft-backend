package es.upm.miw.oneleft.users.domain.port.out;

import es.upm.miw.oneleft.users.domain.model.StoredPhoto;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

/**
 * The object storage of the photos (S3 or compatible). Signing a link is done locally, without calling the storage;
 * the operations that do call it throw {@link es.upm.miw.oneleft.users.domain.model.PhotoStorageUnavailableException}
 * when it does not answer.
 */
public interface PhotoStorage {

    /** A link that only accepts a {@code PUT} of exactly this type and size */
    URI signUpload(String key, String contentType, long size, Duration validity);

    URI signDownload(String key, Duration validity);

    Optional<StoredPhoto> find(String key);

    /** Deletes every object under the prefix except {@code keep} (none if it is {@code null}) */
    void deleteUnder(String prefix, String keep);
}
