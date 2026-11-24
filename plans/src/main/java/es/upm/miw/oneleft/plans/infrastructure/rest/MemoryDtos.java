package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.domain.model.MemoryUpload;
import es.upm.miw.oneleft.plans.domain.model.MemoryView;
import es.upm.miw.oneleft.plans.domain.model.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTOs of the album of a plan (HU-052).
 */
public final class MemoryDtos {

    private MemoryDtos() {
    }

    /** Checked by the domain, which answers with the code of each problem ({@code memory.format}...) */
    @Schema(description = "The photo about to be uploaded, already resized (up to 2 MiB)")
    public record MemoryUploadRequest(
            @Schema(example = "image/webp", allowableValues = {"image/webp", "image/jpeg"}) String contentType,
            @Schema(example = "248213") long size) {
    }

    @Schema(description = "Signed link to upload the photo straight to the storage, with exactly these headers")
    public record MemoryUploadResponse(@Schema(example = "memories/5b0f7c2e-…/0d7a…/9a1c….webp") String key, URI url,
                                       @Schema(example = "PUT") String method, Map<String, String> headers,
                                       Instant expiresAt) {

        static MemoryUploadResponse of(MemoryUpload upload) {
            return new MemoryUploadResponse(upload.key(), upload.url(), "PUT", upload.headers(), upload.expiresAt());
        }
    }

    @Schema(description = "The photo just uploaded, to add it to the album")
    public record ConfirmMemoryRequest(String key) {
    }

    @Schema(description = "A photo of the album of a plan")
    public record MemoryResponse(UUID id, UUID planId,
                                 @Schema(description = "Who uploaded it: only they can remove it") UUID userId,
                                 @Schema(description = "Signed link to see the photo, valid for an hour") URI url,
                                 @Schema(description = "Bytes", example = "248213") long size, Instant uploadedAt) {

        static MemoryResponse of(MemoryView view) {
            var memory = view.memory();
            return new MemoryResponse(memory.id(), memory.planId(), memory.userId(), view.url(), memory.size(),
                    memory.uploadedAt());
        }
    }

    @Schema(description = "A page of the album, newest first")
    public record MemoryPageResponse(List<MemoryResponse> items, int page, int size, long total) {

        static MemoryPageResponse of(PageResult<MemoryView> result) {
            return new MemoryPageResponse(result.items().stream().map(MemoryResponse::of).toList(), result.page(),
                    result.size(), result.total());
        }
    }
}
