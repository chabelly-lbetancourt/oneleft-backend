package es.upm.miw.oneleft.users.infrastructure.storage;

import es.upm.miw.oneleft.users.domain.model.PhotoStorageUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The storage without a server: signing works offline, and a storage that does not answer is said so. */
class S3PhotoStorageTest {

    private final StorageProperties properties = new StorageProperties(URI.create("http://localhost:1"),
            URI.create("https://photos.oneleft.test"), "oneleft-photos", "us-east-1", "access", "secret", true, true);
    private final StorageConfig config = new StorageConfig();
    private final S3Client s3 = config.s3Client(properties);
    private final S3Presigner presigner = config.s3Presigner(properties);
    private final S3PhotoStorage storage = new S3PhotoStorage(s3, presigner, properties);

    @AfterEach
    void close() {
        s3.close();
        presigner.close();
    }

    @Test
    void linksAreSignedForThePublicAddressWithTheTypeAndSizeOfThePhoto() {
        var upload = storage.signUpload("photos/a/b.webp", "image/webp", 48_000, Duration.ofMinutes(5));
        var view = storage.signDownload("photos/a/b.webp", Duration.ofHours(1));

        assertThat(upload.toString()).startsWith("https://photos.oneleft.test/oneleft-photos/photos/a/b.webp?")
                .contains("X-Amz-Expires=300").contains("content-length").contains("content-type");
        assertThat(view.toString()).startsWith("https://photos.oneleft.test/oneleft-photos/photos/a/b.webp?")
                .contains("X-Amz-Expires=3600");
    }

    @Test
    void aStorageThatDoesNotAnswerIsUnavailable() {
        assertThatThrownBy(() -> storage.find("photos/a/b.webp"))
                .isInstanceOf(PhotoStorageUnavailableException.class);
        assertThatThrownBy(() -> storage.deleteUnder("photos/a/", null))
                .isInstanceOf(PhotoStorageUnavailableException.class);
    }

    @Test
    void theServiceStartsEvenIfTheStorageDoesNotAnswer() {
        var initializer = new BucketInitializer(s3, properties);
        assertThatCode(() -> initializer.run(new DefaultApplicationArguments())).doesNotThrowAnyException();
    }
}
