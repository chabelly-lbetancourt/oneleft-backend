package es.upm.miw.oneleft.plans.domain.model;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-052: what a photo of an album must be, and where it is kept. */
class MemoryTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F'};
    private static final byte[] WEBP = "RIFF\u0010\u0000\u0000\u0000WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);

    private final UUID user = UUID.randomUUID();
    private final UUID plan = UUID.randomUUID();

    private static void assertCode(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ValidationException.class,
                error -> assertThat(error.code()).isEqualTo(code));
    }

    @Test
    void onlyWebpAndJpegWhateverTheCaseOfTheType() {
        assertThat(MemoryFormat.of(" IMAGE/WebP ")).isEqualTo(MemoryFormat.WEBP);
        assertThat(MemoryFormat.of("image/jpeg").extension()).isEqualTo("jpg");
        assertCode(() -> MemoryFormat.of("image/png"), "memory.format");
        assertCode(() -> MemoryFormat.of("image/svg+xml"), "memory.format");
        assertCode(() -> MemoryFormat.of(null), "memory.format");
    }

    @Test
    void theFirstBytesSayWhatAFileReallyIs() {
        assertThat(MemoryFormat.detect(JPEG)).hasValue(MemoryFormat.JPEG);
        assertThat(MemoryFormat.detect(WEBP)).hasValue(MemoryFormat.WEBP);
        assertThat(MemoryFormat.detect("<html><script>".getBytes(StandardCharsets.UTF_8))).isEmpty();
        assertThat(MemoryFormat.detect("RIFF\u0010\u0000\u0000\u0000WAVEfmt ".getBytes(StandardCharsets.ISO_8859_1)))
                .isEmpty();
        assertThat(MemoryFormat.detect(new byte[] {(byte) 0xFF, (byte) 0xD8})).isEmpty();
        assertThat(MemoryFormat.detect(new byte[0])).isEmpty();
        assertThat(MemoryFormat.detect(null)).isEmpty();
        assertThat(WEBP).hasSize(MemoryFormat.HEAD_BYTES + 4);
    }

    @Test
    void aPhotoIsNotEmptyNorBiggerThanTheLimit() {
        assertThat(MemorySize.require(MemorySize.MAX_BYTES)).isEqualTo(2L * 1024 * 1024);
        assertCode(() -> MemorySize.require(0), "memory.empty");
        assertCode(() -> MemorySize.require(-1), "memory.empty");
        assertCode(() -> MemorySize.require(MemorySize.MAX_BYTES + 1), "memory.tooBig");
    }

    @Test
    void whatWasUploadedMustBeThePhotoItSays() {
        assertThatCode(() -> new StoredMemory(1000, "image/webp", MemoryFormat.WEBP).requireValid())
                .doesNotThrowAnyException();
        assertCode(() -> new StoredMemory(1000, "image/webp", null).requireValid(), "memory.notAPhoto");
        assertCode(() -> new StoredMemory(1000, "image/webp", MemoryFormat.JPEG).requireValid(), "memory.notAPhoto");
        assertCode(() -> new StoredMemory(1000, "text/html", null).requireValid(), "memory.format");
        assertCode(() -> new StoredMemory(MemorySize.MAX_BYTES + 1, "image/jpeg", MemoryFormat.JPEG).requireValid(),
                "memory.tooBig");
    }

    @Test
    void theKeyIsRandomUnderTheFolderOfThePersonAndThePlan() {
        var key = MemoryKey.newFor(user, plan, MemoryFormat.JPEG);

        assertThat(key).startsWith("memories/" + user + "/" + plan + "/").endsWith(".jpg")
                .hasSizeLessThanOrEqualTo(MemoryKey.MAX_LENGTH)
                .isNotEqualTo(MemoryKey.newFor(user, plan, MemoryFormat.JPEG));
        assertThat(MemoryKey.prefixOf(user, plan)).startsWith(MemoryKey.prefixOf(user));
        assertThat(MemoryKey.requireOwnedBy(key, user, plan)).isEqualTo(key);
    }

    @Test
    void aKeyOfSomebodyElseOrOfAnotherPlanIsNotMine() {
        var mine = MemoryKey.prefixOf(user, plan);
        for (var key : List.of(MemoryKey.newFor(UUID.randomUUID(), plan, MemoryFormat.WEBP),
                MemoryKey.newFor(user, UUID.randomUUID(), MemoryFormat.WEBP), mine + "deeper/photo.webp",
                "photos/" + user + "/photo.webp", mine + "x".repeat(MemoryKey.MAX_LENGTH))) {
            assertCode(() -> MemoryKey.requireOwnedBy(key, user, plan), "memory.key");
        }
        assertCode(() -> MemoryKey.requireOwnedBy(null, user, plan), "memory.key");
    }

    @Test
    void aMemoryKnowsWhoseItIs() {
        var memory = Memory.uploaded(plan, user, "memories/a.webp", 1000, Instant.parse("2026-11-24T10:00:00Z"));

        assertThat(memory.isOf(user)).isTrue();
        assertThat(memory.isOf(UUID.randomUUID())).isFalse();
        assertThat(memory.id()).isNotEqualTo(Memory.uploaded(plan, user, "memories/a.webp", 1000, Instant.EPOCH).id());
        assertCode(() -> new Memory(null, plan, user, "k", 1, Instant.EPOCH), "memory.missingData");
        assertCode(() -> Memory.uploaded(plan, user, null, 1, Instant.EPOCH), "memory.missingData");
    }

    @Test
    void pagesStartAtZeroAndHaveALimit() {
        assertThat(new PageQuery(0, PageQuery.MAX_SIZE).size()).isEqualTo(50);
        assertCode(() -> new PageQuery(-1, 20), "page.invalid");
        assertCode(() -> new PageQuery(0, 0), "page.invalid");
        assertCode(() -> new PageQuery(0, PageQuery.MAX_SIZE + 1), "page.invalid");
        assertThat(new PageResult<>(List.of(1, 2), 3, 2, 8).map(number -> "n" + number))
                .isEqualTo(new PageResult<>(List.of("n1", "n2"), 3, 2, 8));
    }
}
