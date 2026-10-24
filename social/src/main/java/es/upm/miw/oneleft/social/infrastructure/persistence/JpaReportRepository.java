package es.upm.miw.oneleft.social.infrastructure.persistence;

import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.model.PageResult;
import es.upm.miw.oneleft.social.domain.model.Report;
import es.upm.miw.oneleft.social.domain.model.ReportStatus;
import es.upm.miw.oneleft.social.domain.model.ReportTarget;
import es.upm.miw.oneleft.social.domain.port.out.Reports;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Output adapter: the reports, in PostgreSQL (HU-033). */
@Repository
public class JpaReportRepository implements Reports {

    private final SpringDataReportRepository reports;

    public JpaReportRepository(SpringDataReportRepository reports) {
        this.reports = reports;
    }

    @Override
    public Report save(Report report) {
        return reports.save(ReportEntity.of(report)).toDomain();
    }

    @Override
    public Optional<Report> findOpen(UUID reporterId, ReportTarget target, UUID targetId) {
        return reports.findFirstByReporterIdAndTargetAndTargetIdAndStatus(reporterId, target, targetId,
                ReportStatus.OPEN).map(ReportEntity::toDomain);
    }

    @Override
    public PageResult<Report> open(PageQuery page) {
        var rows = reports.findByStatusOrderByReportedAtAscIdAsc(ReportStatus.OPEN,
                PageRequest.of(page.page(), page.size()));
        return new PageResult<>(rows.map(ReportEntity::toDomain).getContent(), page.page(), page.size(),
                rows.getTotalElements());
    }
}
