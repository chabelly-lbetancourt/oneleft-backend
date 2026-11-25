package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.IssuedTrustedLink;
import es.upm.miw.oneleft.plans.domain.model.PlanStatus;
import es.upm.miw.oneleft.plans.domain.model.TrustedLink;
import es.upm.miw.oneleft.plans.domain.model.TrustedPlanView;
import es.upm.miw.oneleft.plans.infrastructure.rest.PlanDtos.MeetingPointDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTOs of the links for trusted contacts (HU-034).
 */
public final class TrustedLinkDtos {

    private TrustedLinkDtos() {
    }

    @Schema(description = "A link just created. The address is only given now: it cannot be asked for again")
    public record CreatedTrustedLinkResponse(
            UUID id, UUID planId,
            @Schema(description = "What to send to the trusted contact: it opens the plan in the web app, without "
                    + "an account", example = "https://oneleft.example/trusted/3q2-7wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
            URI url,
            @Schema(description = "The secret of the link, the last part of the address") String token,
            Instant createdAt,
            @Schema(description = "When it stops working: when the plan is over") Instant expiresAt) {

        static CreatedTrustedLinkResponse of(IssuedTrustedLink issued, URI url) {
            var link = issued.link();
            return new CreatedTrustedLinkResponse(link.id(), link.planId(), url, issued.token(), link.createdAt(),
                    link.expiresAt());
        }
    }

    @Schema(description = "A link of mine that still works, to revoke it. Without its address")
    public record TrustedLinkResponse(UUID id, UUID planId, Instant createdAt, Instant expiresAt) {

        static TrustedLinkResponse of(TrustedLink link) {
            return new TrustedLinkResponse(link.id(), link.planId(), link.createdAt(), link.expiresAt());
        }
    }

    @Schema(description = "My links for a plan that still work, oldest first: at most 5")
    public record TrustedLinksResponse(List<TrustedLinkResponse> items) {

        static TrustedLinksResponse of(List<TrustedLink> links) {
            return new TrustedLinksResponse(links.stream().map(TrustedLinkResponse::of).toList());
        }
    }

    @Schema(description = "The plan as a trusted contact sees it: where, when and with whom. No identifiers and "
            + "nobody of the group but the organizer")
    public record TrustedPlanResponse(
            String title, Activity activity,
            @Schema(description = "The name of the activity when it is OTHER (HU-068)") String activityOther,
            @Schema(description = "The exact meeting point") MeetingPointDto meetingPoint,
            Instant startsAt,
            @Schema(description = "When the plan is over: 3 hours after it starts") Instant endsAt,
            @Schema(description = "CANCELLED tells the contact that the plan is not happening") PlanStatus status,
            String organizerName,
            @Schema(description = "The name of whoever shared the link") String sharedBy,
            @Schema(description = "When the link stops working") Instant expiresAt) {

        static TrustedPlanResponse of(TrustedPlanView view) {
            return new TrustedPlanResponse(view.title(), view.activity(), view.activityOther(),
                    MeetingPointDto.of(view.meetingPoint()), view.startsAt(), view.endsAt(), view.status(),
                    view.organizerName(), view.sharedBy(), view.expiresAt());
        }
    }
}
