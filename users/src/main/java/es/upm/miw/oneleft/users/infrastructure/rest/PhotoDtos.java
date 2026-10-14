package es.upm.miw.oneleft.users.infrastructure.rest;

import es.upm.miw.oneleft.users.domain.model.PhotoUpload;
import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTOs of the profile photos API.
 */
public final class PhotoDtos {

    private PhotoDtos() {
    }

    /** Checked by the domain, which answers with the code of each problem ({@code photo.format}...) */
    @Schema(description = "The photo about to be uploaded, already cropped and resized (up to 1 MiB)")
    public record PhotoUploadRequest(
            @Schema(example = "image/webp", allowableValues = {"image/webp", "image/jpeg"}) String contentType,
            @Schema(example = "48213") long size) {
    }

    @Schema(description = "Signed link to upload the photo straight to the storage, with exactly these headers")
    public record PhotoUploadResponse(@Schema(example = "photos/5b0f7c2e-…/9a1c….webp") String key, URI url,
                                      @Schema(example = "PUT") String method, Map<String, String> headers,
                                      Instant expiresAt) {

        static PhotoUploadResponse of(PhotoUpload upload) {
            return new PhotoUploadResponse(upload.key(), upload.url(), "PUT", upload.headers(), upload.expiresAt());
        }
    }

    @Schema(description = "The photo just uploaded, to make it mine")
    public record ConfirmPhotoRequest(String key) {
    }

    @Schema(description = "Signed link to see the photo of a person, valid for an hour")
    public record PhotoResponse(UUID userId, URI url) {
    }
}
