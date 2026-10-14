package es.upm.miw.oneleft.users.infrastructure.rest;

import es.upm.miw.oneleft.users.domain.port.in.ManagePhotoUseCase;
import es.upm.miw.oneleft.users.infrastructure.config.KeycloakJwt;
import es.upm.miw.oneleft.users.infrastructure.rest.PhotoDtos.ConfirmPhotoRequest;
import es.upm.miw.oneleft.users.infrastructure.rest.PhotoDtos.PhotoResponse;
import es.upm.miw.oneleft.users.infrastructure.rest.PhotoDtos.PhotoUploadRequest;
import es.upm.miw.oneleft.users.infrastructure.rest.PhotoDtos.PhotoUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(UserController.USERS)
@Tag(name = "Photos", description = "Profile photo through signed links of the storage (HU-020)")
public class PhotoController {

    public static final String MY_PHOTO = "/me/photo";
    public static final String MY_PHOTO_UPLOADS = MY_PHOTO + "/uploads";
    public static final String PHOTOS = "/photos";

    private final ManagePhotoUseCase photos;

    public PhotoController(ManagePhotoUseCase photos) {
        this.photos = photos;
    }

    @PostMapping(MY_PHOTO_UPLOADS)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Link to upload my photo",
            description = "A signed link, valid for 5 minutes, that only accepts a PUT of this type and size. "
                    + "The photo goes straight to the storage; then confirm it with PUT /me/photo.")
    @ApiResponse(responseCode = "201", description = "Signed link")
    @ApiResponse(responseCode = "400", description = "Not WebP or JPEG, empty or bigger than 1 MiB",
            content = @Content)
    @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content)
    public PhotoUploadResponse startUpload(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                           @RequestBody PhotoUploadRequest request) {
        return PhotoUploadResponse.of(
                photos.startUpload(KeycloakJwt.toIdentity(jwt), request.contentType(), request.size()));
    }

    @PutMapping(MY_PHOTO)
    @Operation(summary = "Make the uploaded photo mine", description = "Replaces and deletes the previous one.")
    @ApiResponse(responseCode = "200", description = "Signed link to see it, valid for an hour")
    @ApiResponse(responseCode = "400", description = "Not my key, or nothing valid was uploaded there",
            content = @Content)
    @ApiResponse(responseCode = "503", description = "The storage does not answer", content = @Content)
    public PhotoResponse confirmUpload(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                       @RequestBody ConfirmPhotoRequest request) {
        var identity = KeycloakJwt.toIdentity(jwt);
        var url = photos.confirmUpload(identity, request.key());
        return new PhotoResponse(UUID.fromString(identity.subject()), url);
    }

    @DeleteMapping(MY_PHOTO)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove my photo")
    @ApiResponse(responseCode = "204", description = "Removed, or there was none")
    @ApiResponse(responseCode = "503", description = "The storage does not answer", content = @Content)
    public void removeMyPhoto(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        photos.removeMyPhoto(KeycloakJwt.toIdentity(jwt));
    }

    @GetMapping(PHOTOS)
    @Operation(summary = "Photos of several people",
            description = "One request for a whole list (up to 50). Whoever has no photo is left out.")
    @ApiResponse(responseCode = "200", description = "Signed links, valid for an hour")
    @ApiResponse(responseCode = "400", description = "More than 50 people", content = @Content)
    public List<PhotoResponse> photosOf(@RequestParam List<UUID> ids) {
        return photos.photosOf(ids).entrySet().stream()
                .map(photo -> new PhotoResponse(photo.getKey(), photo.getValue()))
                .toList();
    }
}
