package es.upm.miw.oneleft.social.domain.model;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A report of a person or a plan (HU-033), with its reason, for the moderation (HU-011) and the administration panel
 * (HU-013). {@code reportedUserId} is who it is about: the person, or the organizer of the plan as the app shows it,
 * which the moderation checks before confirming it.
 */
public record Report(UUID id, UUID reporterId, ReportTarget target, UUID targetId, UUID reportedUserId,
                     ReportReason reason, String comment, ReportStatus status, Instant reportedAt) {

    public static final int MAX_COMMENT = 500;

    public static Report file(UUID reporterId, ReportTarget target, UUID targetId, UUID reportedUserId,
                              ReportReason reason, String comment, Clock clock) {
        if (target == null || targetId == null) {
            throw new ValidationException("report.target", "Say which person or plan is reported");
        }
        if (reportedUserId == null) {
            throw new ValidationException("report.person", "Say who organizes the reported plan");
        }
        if (reason == null) {
            throw new ValidationException("report.reason", "Choose why it is reported");
        }
        var text = comment == null || comment.isBlank() ? null : comment.strip();
        if (text != null && text.length() > MAX_COMMENT) {
            throw new ValidationException("report.comment", "The comment has at most " + MAX_COMMENT + " characters");
        }
        if (reason == ReportReason.OTHER && text == null) {
            throw new ValidationException("report.comment", "Say what happened when the reason is another one");
        }
        if (Objects.equals(reporterId, reportedUserId)) {
            throw new ValidationException("report.self", "Nobody reports themselves");
        }
        return new Report(UUID.randomUUID(), reporterId, target, targetId, reportedUserId, reason, text,
                ReportStatus.OPEN, clock.instant());
    }
}
