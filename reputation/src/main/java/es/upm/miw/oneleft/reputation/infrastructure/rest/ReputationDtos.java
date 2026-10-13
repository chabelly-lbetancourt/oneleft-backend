package es.upm.miw.oneleft.reputation.infrastructure.rest;

import es.upm.miw.oneleft.reputation.domain.model.Badge;
import es.upm.miw.oneleft.reputation.domain.model.BadgeProgress;
import es.upm.miw.oneleft.reputation.domain.model.LedgerEntry;
import es.upm.miw.oneleft.reputation.domain.model.PageResult;
import es.upm.miw.oneleft.reputation.domain.model.Reason;
import es.upm.miw.oneleft.reputation.domain.model.Reputation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ReputationDtos {

    private ReputationDtos() {
    }

    @Schema(description = "The score of someone: the sum of their entries, 0 without any")
    public record ReputationResponse(UUID userId, @Schema(example = "35") int score) {

        static ReputationResponse of(Reputation reputation) {
            return new ReputationResponse(reputation.userId(), reputation.score());
        }
    }

    @Schema(description = "An entry of the ledger: why the reputation went up or down")
    public record EntryResponse(Reason reason, @Schema(example = "10") int points,
                                @Schema(description = "The plan that caused it, when it was a plan") UUID planId,
                                @Schema(example = "Padel 2 vs 2") String planTitle, Instant occurredAt) {

        static EntryResponse of(LedgerEntry entry) {
            var fromPlan = entry.planTitle() != null;
            return new EntryResponse(entry.reason(), entry.points(), fromPlan ? entry.sourceId() : null,
                    entry.planTitle(), entry.occurredAt());
        }
    }

    @Schema(description = "A page of the ledger, the latest first")
    public record LedgerResponse(List<EntryResponse> items, int page, int size, long total) {

        static LedgerResponse of(PageResult<LedgerEntry> result) {
            return new LedgerResponse(result.items().stream().map(EntryResponse::of).toList(), result.page(),
                    result.size(), result.total());
        }
    }

    @Schema(description = "The scores rebuilt from the ledger")
    public record RebuildResponse(@Schema(description = "People with a score") int people) {
    }

    @Schema(description = "A badge for a milestone (HU-044): how far, and since when it was earned")
    public record BadgeResponse(Badge badge, @Schema(example = "7") int progress, @Schema(example = "10") int goal,
                                boolean earned, Instant earnedAt) {

        static BadgeResponse of(BadgeProgress badge) {
            return new BadgeResponse(badge.badge(), badge.progress(), badge.goal(), badge.earned(),
                    badge.earnedAt());
        }
    }
}
