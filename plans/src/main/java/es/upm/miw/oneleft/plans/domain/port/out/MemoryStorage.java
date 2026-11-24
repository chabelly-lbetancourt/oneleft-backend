package es.upm.miw.oneleft.plans.domain.port.out;

import es.upm.miw.oneleft.plans.domain.model.StoredMemory;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * The object storage of the photos (S3 or compatible). Signing a link is done locally, without calling the storage;
 * the operations that do call it throw
 * {@link es.upm.miw.oneleft.plans.domain.model.MemoryStorageUnavailableException} when it does not answer.
 */
public interface MemoryStorage {

    /** A link that only accepts a {@code PUT} of exactly this type and size */
    URI signUpload(String key, String contentType, long size, Duration validity);

    URI signDownload(String key, Duration validity);

    /** What was uploaded there, with what its first bytes say it is */
    Optional<StoredMemory> find(String key);

    void delete(String key);

    /** Deletes every object under the prefix */
    void deleteUnder(String prefix);

    /** Deletes the objects under the prefix uploaded before {@code before}, except those in {@code keep} */
    void deleteAbandoned(String prefix, Set<String> keep, Instant before);
}
