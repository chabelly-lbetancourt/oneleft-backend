package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.DraftRequest;
import es.upm.miw.oneleft.plans.domain.model.Level;
import es.upm.miw.oneleft.plans.domain.model.PlanDraft;
import es.upm.miw.oneleft.plans.domain.port.in.DraftPlanUseCase;
import es.upm.miw.oneleft.plans.infrastructure.rest.PlanDtos.MeetingPointDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping(PlanDraftController.DRAFT)
@Tag(name = "Plans", description = "Publish and look up plans")
public class PlanDraftController {

    public static final String DRAFT = "/api/v1/plans/draft";

    private final DraftPlanUseCase drafts;

    public PlanDraftController(DraftPlanUseCase drafts) {
        this.drafts = drafts;
    }

    @PostMapping
    @Operation(summary = "Draft a plan from a sentence (HU-010)",
            description = "A language model reads what the person wrote or dictated and the draft is checked against "
                    + "the rules of a plan. Nothing is published or stored: the person reviews the draft and "
                    + "publishes it with POST /api/v1/plans. What could not be understood comes back in `missing`.")
    @ApiResponse(responseCode = "200", description = "The draft, possibly with fields missing")
    @ApiResponse(responseCode = "400", description = "draft.text, coordinates.outOfRange", content = @Content)
    @ApiResponse(responseCode = "503", description = "draft.unavailable: the language model cannot be used now",
            content = @Content)
    public DraftResponse draft(@Valid @RequestBody DraftBody body) {
        return DraftResponse.of(drafts.draft(DraftRequest.of(body.text(), body.latitude(), body.longitude(),
                body.timeZone())));
    }

    @Schema(description = "The sentence and where the person is")
    public record DraftBody(
            @NotBlank @Size(max = DraftRequest.MAX_LENGTH)
            @Schema(example = "Pádel mañana a las 7 en las pistas de la Albufera, falta uno") String text,
            @NotNull @DecimalMin("-90") @DecimalMax("90") @Schema(example = "40.39") Double latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") @Schema(example = "-3.63") Double longitude,
            @Schema(description = "IANA time zone of the person; Europe/Madrid if absent", example = "Europe/Madrid")
            String timeZone) {
    }

    @Schema(description = "A plan understood from a sentence: to review before publishing")
    public record DraftResponse(Activity activity, String title, String description,
                                @Schema(description = "The place as the person named it") String place,
                                MeetingPointDto meetingPoint, Instant startsAt, Integer spots, Level level,
                                Integer minParticipants,
                                @Schema(description = "Fields to fill in: activity, title, meetingPoint, startsAt, "
                                        + "spots") List<String> missing,
                                @Schema(description = "startsAt.window, meetingPoint.notFound") List<String> warnings) {

        static DraftResponse of(PlanDraft draft) {
            var point = draft.meetingPoint();
            return new DraftResponse(draft.activity(), draft.title(), draft.description(), draft.place(),
                    point == null ? null : MeetingPointDto.of(point), draft.startsAt(), draft.spots(), draft.level(),
                    draft.minParticipants(), draft.missing(), draft.warnings());
        }
    }
}
