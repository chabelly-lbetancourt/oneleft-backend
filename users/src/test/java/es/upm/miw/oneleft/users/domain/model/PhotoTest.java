package es.upm.miw.oneleft.users.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhotoTest {

    private final UUID ana = UUID.randomUUID();

    private static void assertCode(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ValidationException.class,
                exception -> assertThat(exception.code()).isEqualTo(code));
    }

    @Test
    void onlyWebpAndJpegAreAccepted() {
        assertThat(PhotoFormat.of("image/webp")).isEqualTo(PhotoFormat.WEBP);
        assertThat(PhotoFormat.of(" IMAGE/JPEG ")).isEqualTo(PhotoFormat.JPEG);
        assertCode(() -> PhotoFormat.of("image/png"), "photo.format");
        assertCode(() -> PhotoFormat.of("text/html"), "photo.format");
        assertCode(() -> PhotoFormat.of(null), "photo.format");
    }

    @Test
    void aPhotoIsNeitherEmptyNorBiggerThanOneMebibyte() {
        assertThat(PhotoSize.require(PhotoSize.MAX_BYTES)).isEqualTo(1_048_576);
        assertCode(() -> PhotoSize.require(0), "photo.empty");
        assertCode(() -> PhotoSize.require(PhotoSize.MAX_BYTES + 1), "photo.tooBig");
    }

    @Test
    void everyUploadGetsANewKeyInTheFolderOfItsOwner() {
        var first = PhotoKey.newFor(ana, PhotoFormat.WEBP);
        var second = PhotoKey.newFor(ana, PhotoFormat.JPEG);

        assertThat(first).startsWith("photos/" + ana + "/").endsWith(".webp").isNotEqualTo(second);
        assertThat(second).endsWith(".jpg");
        assertThat(first.length()).isLessThanOrEqualTo(PhotoKey.MAX_LENGTH);
        assertThat(PhotoKey.requireOwnedBy(first, ana)).isEqualTo(first);
    }

    @Test
    void nobodyConfirmsAKeyThatIsNotTheirs() {
        var other = PhotoKey.newFor(UUID.randomUUID(), PhotoFormat.WEBP);
        assertCode(() -> PhotoKey.requireOwnedBy(other, ana), "photo.key");
        assertCode(() -> PhotoKey.requireOwnedBy(PhotoKey.prefixOf(ana) + "../x.webp/y", ana), "photo.key");
        assertCode(() -> PhotoKey.requireOwnedBy(PhotoKey.prefixOf(ana) + "x".repeat(200), ana), "photo.key");
        assertCode(() -> PhotoKey.requireOwnedBy(null, ana), "photo.key");
    }

    @Test
    void whatWasUploadedMustBeAPhotoWithinTheLimits() {
        assertThat(new StoredPhoto(48_000, "image/webp").requireValid().size()).isEqualTo(48_000);
        assertCode(() -> new StoredPhoto(48_000, "application/octet-stream").requireValid(), "photo.format");
        assertCode(() -> new StoredPhoto(PhotoSize.MAX_BYTES + 1, "image/jpeg").requireValid(), "photo.tooBig");
    }
}
