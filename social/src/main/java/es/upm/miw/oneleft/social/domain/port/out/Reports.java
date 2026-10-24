package es.upm.miw.oneleft.social.domain.port.out;

import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.model.PageResult;
import es.upm.miw.oneleft.social.domain.model.Report;
import es.upm.miw.oneleft.social.domain.model.ReportTarget;

import java.util.Optional;
import java.util.UUID;

/** The reports of people and plans (HU-033). */
public interface Reports {

    Report save(Report report);

    /** The report of {@code reporterId} about that person or plan that is still open, if any. */
    Optional<Report> findOpen(UUID reporterId, ReportTarget target, UUID targetId);

    /** The open reports, the oldest first. */
    PageResult<Report> open(PageQuery page);
}
