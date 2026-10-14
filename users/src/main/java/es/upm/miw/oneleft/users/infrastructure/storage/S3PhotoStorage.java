package es.upm.miw.oneleft.users.infrastructure.storage;

import es.upm.miw.oneleft.users.domain.model.PhotoStorageUnavailableException;
import es.upm.miw.oneleft.users.domain.model.StoredPhoto;
import es.upm.miw.oneleft.users.domain.port.out.PhotoStorage;
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
import java.util.Objects;
import java.util.Optional;

/**
 * Output adapter: the photos in an S3-compatible storage, with the AWS SDK.
 */
@Component
public class S3PhotoStorage implements PhotoStorage {

    private static final int NOT_FOUND = 404;

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;

    public S3PhotoStorage(S3Client s3, S3Presigner presigner, StorageProperties storage) {
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
    public Optional<StoredPhoto> find(String key) {
        try {
            var head = s3.headObject(request -> request.bucket(bucket).key(key));
            return Optional.of(new StoredPhoto(head.contentLength(), head.contentType()));
        } catch (S3Exception exception) {
            if (exception.statusCode() == NOT_FOUND) {
                return Optional.empty();
            }
            throw new PhotoStorageUnavailableException(exception);
        } catch (SdkException exception) {
            throw new PhotoStorageUnavailableException(exception);
        }
    }

    @Override
    public void deleteUnder(String prefix, String keep) {
        try {
            // One by one: a person has their photo and, at most, an upload never confirmed
            s3.listObjectsV2Paginator(request -> request.bucket(bucket).prefix(prefix)).contents().stream()
                    .map(S3Object::key)
                    .filter(key -> !Objects.equals(key, keep))
                    .forEach(key -> s3.deleteObject(request -> request.bucket(bucket).key(key)));
        } catch (SdkException exception) {
            throw new PhotoStorageUnavailableException(exception);
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
