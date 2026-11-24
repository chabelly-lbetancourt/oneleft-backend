package es.upm.miw.oneleft.plans.infrastructure.storage;

import es.upm.miw.oneleft.plans.domain.model.MemoryFormat;
import es.upm.miw.oneleft.plans.domain.model.MemoryStorageUnavailableException;
import es.upm.miw.oneleft.plans.domain.model.StoredMemory;
import es.upm.miw.oneleft.plans.domain.port.out.MemoryStorage;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Output adapter: the photos of the albums in an S3-compatible storage, with the AWS SDK.
 */
@Component
public class S3MemoryStorage implements MemoryStorage {

    private static final int NOT_FOUND = 404;
    private static final String HEAD_RANGE = "bytes=0-" + (MemoryFormat.HEAD_BYTES - 1);

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;

    public S3MemoryStorage(S3Client s3, S3Presigner presigner, StorageProperties storage) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = storage.bucket();
    }

    @Override
    public URI signUpload(String key, String contentType, long size, Duration validity) {
        var put = PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).contentLength(size)
                .build();
        return uri(presigner.presignPutObject(request -> request.signatureDuration(validity).putObjectRequest(put))
                .url());
    }

    @Override
    public URI signDownload(String key, Duration validity) {
        var get = GetObjectRequest.builder().bucket(bucket).key(key).build();
        return uri(presigner.presignGetObject(request -> request.signatureDuration(validity).getObjectRequest(get))
                .url());
    }

    @Override
    public Optional<StoredMemory> find(String key) {
        try {
            var head = s3.headObject(request -> request.bucket(bucket).key(key));
            return Optional.of(new StoredMemory(head.contentLength(), head.contentType(),
                    head.contentLength() > 0 ? contentOf(key) : null));
        } catch (S3Exception exception) {
            if (exception.statusCode() == NOT_FOUND) {
                return Optional.empty();
            }
            throw new MemoryStorageUnavailableException(exception);
        } catch (SdkException exception) {
            throw new MemoryStorageUnavailableException(exception);
        }
    }

    /** Only the first bytes are read: they are enough to know what the file is */
    private MemoryFormat contentOf(String key) {
        var start = s3.getObjectAsBytes(request -> request.bucket(bucket).key(key).range(HEAD_RANGE)).asByteArray();
        return MemoryFormat.detect(start).orElse(null);
    }

    @Override
    public void delete(String key) {
        try {
            s3.deleteObject(request -> request.bucket(bucket).key(key));
        } catch (SdkException exception) {
            throw new MemoryStorageUnavailableException(exception);
        }
    }

    @Override
    public void deleteUnder(String prefix) {
        deleteUnder(prefix, object -> true);
    }

    @Override
    public void deleteAbandoned(String prefix, Set<String> keep, Instant before) {
        deleteUnder(prefix, object -> !keep.contains(object.key()) && object.lastModified().isBefore(before));
    }

    private void deleteUnder(String prefix, Predicate<S3Object> which) {
        try {
            // One by one: a person has a few photos in each album
            s3.listObjectsV2Paginator(request -> request.bucket(bucket).prefix(prefix)).contents().stream()
                    .filter(which)
                    .map(S3Object::key)
                    .forEach(key -> s3.deleteObject(request -> request.bucket(bucket).key(key)));
        } catch (SdkException exception) {
            throw new MemoryStorageUnavailableException(exception);
        }
    }

    private static URI uri(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("The storage signed an invalid link", exception);
        }
    }
}
