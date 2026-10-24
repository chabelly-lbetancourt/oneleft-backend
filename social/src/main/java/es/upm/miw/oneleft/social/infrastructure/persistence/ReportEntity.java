package es.upm.miw.oneleft.social.infrastructure.persistence;

import es.upm.miw.oneleft.social.domain.model.Report;
import es.upm.miw.oneleft.social.domain.model.ReportReason;
import es.upm.miw.oneleft.social.domain.model.ReportStatus;
import es.upm.miw.oneleft.social.domain.model.ReportTarget;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** A report of a person or a plan (HU-033). */
@Entity
@Table(name = "report")
public class ReportEntity {

    @Id
    private UUID id;

    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private ReportTarget target;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "reported_user_id", nullable = false)
    private UUID reportedUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Column(name = "reported_at", nullable = false)
    private Instant reportedAt;

    protected ReportEntity() {
    }

    static ReportEntity of(Report report) {
        var entity = new ReportEntity();
        entity.id = report.id();
        entity.reporterId = report.reporterId();
        entity.target = report.target();
        entity.targetId = report.targetId();
        entity.reportedUserId = report.reportedUserId();
        entity.reason = report.reason();
        entity.comment = report.comment();
        entity.status = report.status();
        entity.reportedAt = report.reportedAt();
        return entity;
    }

    Report toDomain() {
        return new Report(id, reporterId, target, targetId, reportedUserId, reason, comment, status, reportedAt);
    }
}
