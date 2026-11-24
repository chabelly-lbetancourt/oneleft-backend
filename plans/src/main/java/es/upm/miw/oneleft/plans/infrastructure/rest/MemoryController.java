package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.domain.model.PageQuery;
import es.upm.miw.oneleft.plans.domain.port.in.PlanMemoriesUseCase;
import es.upm.miw.oneleft.plans.infrastructure.rest.MemoryDtos.ConfirmMemoryRequest;
import es.upm.miw.oneleft.plans.infrastructure.rest.MemoryDtos.MemoryPageResponse;
import es.upm.miw.oneleft.plans.infrastructure.rest.MemoryDtos.MemoryResponse;
import es.upm.miw.oneleft.plans.infrastructure.rest.MemoryDtos.MemoryUploadRequest;
import es.upm.miw.oneleft.plans.infrastructure.rest.MemoryDtos.MemoryUploadResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** The album of a plan (HU-052): photos its group shares once it is over, through signed links of the storage. */
@RestController
@RequestMapping(PlanController.PLANS)
@Tag(name = "Memories", description = "Album of a plan: photos shared by its group once it is over (HU-052)")
public class MemoryController {

    public static final String MEMORIES = PlanController.PLAN + "/memories";
    public static final String MEMORY_UPLOADS = MEMORIES + "/uploads";
    public static final String MEMORY = MEMORIES + "/{memoryId}";

    private static final String NOT_OF_THE_GROUP = "plan.notParticipant: neither the organizer nor a participant";

    private final PlanMemoriesUseCase memories;

    public MemoryController(PlanMemoriesUseCase memories) {
        this.memories = memories;
    }

    @PostMapping(MEMORY_UPLOADS)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Link to upload a photo to the album",
            description = "Only the organizer and the participants, once the plan is over. A signed link, valid for "
                    + "5 minutes, that only accepts a PUT of this type and size. The photo goes straight to the "
                    + "storage; then add it to the album with POST /{planId}/memories.")
    @ApiResponse(responseCode = "201", description = "Signed link")
    @ApiResponse(responseCode = "400", description = "memory.format (not WebP or JPEG), memory.empty or "
            + "memory.tooBig (more than 2 MiB)", content = @Content)
    @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content)
    @ApiResponse(responseCode = "403", description = NOT_OF_THE_GROUP, content = @Content)
    @ApiResponse(responseCode = "404", description = "plan.notFound", content = @Content)
    @ApiResponse(responseCode = "409", description = "memory.planNotFinished, memory.personLimit (10 photos each) "
            + "or memory.planLimit (100 photos in the album)", content = @Content)
    @ApiResponse(responseCode = "503", description = "memory.unavailable: the storage does not answer",
            content = @Content)
    public MemoryUploadResponse startUpload(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                            @PathVariable UUID planId, @RequestBody MemoryUploadRequest request) {
        return MemoryUploadResponse.of(
                memories.startUpload(planId, userId(jwt), request.contentType(), request.size()));
    }

    @PostMapping(MEMORIES)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add the uploaded photo to the album",
            description = "Checks in the storage that the photo is there and that its content really is a WebP or a "
                    + "JPEG; anything else is deleted. Adding the same photo again returns it as it is.")
    @ApiResponse(responseCode = "201", description = "The photo, with a signed link to see it")
    @ApiResponse(responseCode = "400", description = "memory.key (not a key given to me for this plan), "
            + "memory.notUploaded, memory.notAPhoto (the content is not the photo it says), memory.format, "
            + "memory.empty or memory.tooBig", content = @Content)
    @ApiResponse(responseCode = "403", description = NOT_OF_THE_GROUP, content = @Content)
    @ApiResponse(responseCode = "404", description = "plan.notFound", content = @Content)
    @ApiResponse(responseCode = "409", description = "memory.planNotFinished, memory.personLimit or memory.planLimit",
            content = @Content)
    @ApiResponse(responseCode = "503", description = "memory.unavailable", content = @Content)
    public MemoryResponse confirmUpload(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                        @PathVariable UUID planId, @RequestBody ConfirmMemoryRequest request) {
        return MemoryResponse.of(memories.confirmUpload(planId, userId(jwt), request.key()));
    }

    @GetMapping(MEMORIES)
    @Operation(summary = "The album of a plan",
            description = "Only the organizer and the participants, newest first. Pages start at 0 and have up to 50 "
                    + "photos (20 by default). Each photo comes with a signed link that expires in an hour.")
    @ApiResponse(responseCode = "200", description = "A page of the album")
    @ApiResponse(responseCode = "400", description = "page.invalid", content = @Content)
    @ApiResponse(responseCode = "403", description = NOT_OF_THE_GROUP, content = @Content)
    @ApiResponse(responseCode = "404", description = "plan.notFound", content = @Content)
    public MemoryPageResponse memories(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                       @PathVariable UUID planId,
                                       @RequestParam(required = false) Integer page,
                                       @RequestParam(required = false) Integer size) {
        var query = new PageQuery(page == null ? 0 : page, size == null ? PageQuery.DEFAULT_SIZE : size);
        return MemoryPageResponse.of(memories.memories(planId, userId(jwt), query));
    }

    @DeleteMapping(MEMORY)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a photo of mine from the album",
            description = "Only whoever uploaded it. The photo is deleted from the storage too.")
    @ApiResponse(responseCode = "204", description = "Removed")
    @ApiResponse(responseCode = "403", description = NOT_OF_THE_GROUP + ", or memory.notYours", content = @Content)
    @ApiResponse(responseCode = "404", description = "plan.notFound or memory.notFound", content = @Content)
    @ApiResponse(responseCode = "503", description = "memory.unavailable", content = @Content)
    public void remove(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable UUID planId,
                       @PathVariable UUID memoryId) {
        memories.remove(planId, userId(jwt), memoryId);
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
