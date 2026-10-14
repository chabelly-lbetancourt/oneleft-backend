package es.upm.miw.oneleft.users.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * In development the bucket of the photos is created on startup, so the local storage needs no setup. Deployed,
 * the bucket already exists ({@code STORAGE_CREATE_BUCKET=false}). If the storage does not answer, the service still
 * starts: only the photos fail, and they say so.
 */
@Component
public class BucketInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BucketInitializer.class);
    private static final int NOT_FOUND = 404;

    private final S3Client s3;
    private final StorageProperties storage;

    public BucketInitializer(S3Client s3, StorageProperties storage) {
        this.s3 = s3;
        this.storage = storage;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!storage.createBucket()) {
            return;
        }
        try {
            if (!exists()) {
                s3.createBucket(request -> request.bucket(storage.bucket()));
                log.info("Bucket {} created", storage.bucket());
            }
        } catch (SdkException exception) {
            log.warn("The storage of the photos is not available: {}", exception.getMessage());
        }
    }

    private boolean exists() {
        try {
            s3.headBucket(request -> request.bucket(storage.bucket()));
            return true;
        } catch (S3Exception exception) {
            if (exception.statusCode() == NOT_FOUND) {
                return false;
            }
            throw exception;
        }
    }
}
