package es.upm.miw.oneleft.social.infrastructure.persistence;

import es.upm.miw.oneleft.social.domain.model.ReportStatus;
import es.upm.miw.oneleft.social.domain.model.ReportTarget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataReportRepository extends JpaRepository<ReportEntity, UUID> {

    Optional<ReportEntity> findFirstByReporterIdAndTargetAndTargetIdAndStatus(UUID reporterId, ReportTarget target,
                                                                              UUID targetId, ReportStatus status);

    Page<ReportEntity> findByStatusOrderByReportedAtAscIdAsc(ReportStatus status, Pageable page);
}
