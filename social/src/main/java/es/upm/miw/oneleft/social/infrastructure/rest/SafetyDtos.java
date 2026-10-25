package es.upm.miw.oneleft.social.infrastructure.rest;

import es.upm.miw.oneleft.social.domain.model.BlockedPerson;
import es.upm.miw.oneleft.social.domain.model.PageResult;
import es.upm.miw.oneleft.social.domain.model.Report;
import es.upm.miw.oneleft.social.domain.model.ReportReason;
import es.upm.miw.oneleft.social.domain.model.ReportStatus;
import es.upm.miw.oneleft.social.domain.model.ReportTarget;
import es.upm.miw.oneleft.social.domain.port.in.ReportCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Blocks and reports (HU-033). */
public final class SafetyDtos {

    private SafetyDtos() {
    }

    @Schema(description = "Someone I blocked and since when")
    public record BlockedPersonResponse(UUID userId, @Schema(example = "Diego") String name, Instant since) {

        static BlockedPersonResponse of(BlockedPerson person) {
            return new BlockedPersonResponse(person.userId(), person.name(), person.since());
        }
    }

    public record BlockedPageResponse(List<BlockedPersonResponse> items, int page, int size, long total) {

        static BlockedPageResponse of(PageResult<BlockedPerson> result) {
            return new BlockedPageResponse(result.items().stream().map(BlockedPersonResponse::of).toList(),
                    result.page(), result.size(), result.total());
        }
    }

    @Schema(description = "A report of a person or a plan")
    public record ReportRequest(@NotNull ReportTarget target,
                                @NotNull @Schema(description = "The person or the plan") UUID targetId,
                                @Schema(description = "Who organizes the plan (only for PLAN)") UUID organizerId,
                                @NotNull ReportReason reason,
                                @Size(max = Report.MAX_COMMENT)
                                @Schema(description = "What happened: required with OTHER") String comment) {

        ReportCommand toCommand() {
            return new ReportCommand(target, targetId, organizerId, reason, comment);
        }
    }

    @Schema(description = "My report, as it was saved")
    public record ReportResponse(UUID id, ReportTarget target, UUID targetId, ReportReason reason, ReportStatus status,
                                 Instant reportedAt) {

        static ReportResponse of(Report report) {
            return new ReportResponse(report.id(), report.target(), report.targetId(), report.reason(),
                    report.status(), report.reportedAt());
        }
    }

    @Schema(description = "A report for the administration (HU-013): who reported what, about whom and why")
    public record OpenReportResponse(UUID id, UUID reporterId, ReportTarget target, UUID targetId,
                                     UUID reportedUserId, ReportReason reason, String comment, Instant reportedAt) {

        static OpenReportResponse of(Report report) {
            return new OpenReportResponse(report.id(), report.reporterId(), report.target(), report.targetId(),
                    report.reportedUserId(), report.reason(), report.comment(), report.reportedAt());
        }
    }

    public record ReportPageResponse(List<OpenReportResponse> items, int page, int size, long total) {

        static ReportPageResponse of(PageResult<Report> result) {
            return new ReportPageResponse(result.items().stream().map(OpenReportResponse::of).toList(),
                    result.page(), result.size(), result.total());
        }
    }
}
