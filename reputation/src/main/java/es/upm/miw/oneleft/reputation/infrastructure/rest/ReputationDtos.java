package es.upm.miw.oneleft.reputation.infrastructure.rest;

import es.upm.miw.oneleft.reputation.domain.model.Badge;
import es.upm.miw.oneleft.reputation.domain.model.BadgeProgress;
import es.upm.miw.oneleft.reputation.domain.model.LedgerEntry;
import es.upm.miw.oneleft.reputation.domain.model.Level;
import es.upm.miw.oneleft.reputation.domain.model.PageResult;
import es.upm.miw.oneleft.reputation.domain.model.PointsLimit;
import es.upm.miw.oneleft.reputation.domain.model.Reason;
import es.upm.miw.oneleft.reputation.domain.model.Reputation;
import es.upm.miw.oneleft.reputation.domain.model.Streak;
import es.upm.miw.oneleft.reputation.domain.model.StreakRules;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ReputationDtos {

    private ReputationDtos() {
    }

    @Schema(description = "The score of someone: the sum of their entries, 0 without any, and its level (HU-062)")
    public record ReputationResponse(UUID userId, @Schema(example = "135") int score,
                                     @Schema(description = "NEW 0-99 (also below 0), REGULAR 100-499, "
                                             + "NEIGHBOURHOOD_REFERENCE 500-1499, LEGEND 1500+",
                                             example = "REGULAR") Level level,
                                     @Schema(description = "The score at which the next level starts; null at LEGEND",
                                             example = "500") Integer nextLevelAt) {

        static ReputationResponse of(Reputation reputation) {
            return new ReputationResponse(reputation.userId(), reputation.score(), reputation.level(),
                    reputation.nextLevelAt().orElse(null));
        }
    }

    @Schema(description = "My weekly streak (HU-062): the weeks in a row with at least one plan checked in")
    public record StreakResponse(@Schema(example = "3") int weeks,
                                 @Schema(description = "This week already has a plan") boolean thisWeek,
                                 @Schema(description = "The points of the next week of the streak", example = "20")
                                 int nextBonus,
                                 @Schema(description = "The most a week of streak gives", example = "25")
                                 int maxBonus) {

        static StreakResponse of(Streak streak) {
            return new StreakResponse(streak.weeks(), streak.thisWeek(), streak.nextBonus(), StreakRules.MAX_BONUS);
        }
    }

    @Schema(description = "An entry of the ledger: why the reputation went up or down")
    public record EntryResponse(Reason reason, @Schema(example = "10") int points,
                                @Schema(description = "The plan that caused it, when it was a plan") UUID planId,
                                @Schema(example = "Padel 2 vs 2") String planTitle, Instant occurredAt,
                                @Schema(description = "Why it gave fewer points than its reason is worth (HU-066), "
                                        + "or null") PointsLimit limitedBy) {

        static EntryResponse of(LedgerEntry entry) {
            var fromPlan = entry.planTitle() != null;
            return new EntryResponse(entry.reason(), entry.points(), fromPlan ? entry.sourceId() : null,
                    entry.planTitle(), entry.occurredAt(), entry.limitedBy());
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
