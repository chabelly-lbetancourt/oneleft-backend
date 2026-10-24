package es.upm.miw.oneleft.social.domain.port.in;

import es.upm.miw.oneleft.social.domain.model.Caller;
import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.model.PageResult;
import es.upm.miw.oneleft.social.domain.model.Report;

/**
 * Reporting a person or a plan (HU-033). The reports wait for the moderation (HU-011) and the administration panel
 * (HU-013); reporting the same thing again while the report is open changes nothing.
 */
public interface ReportUseCase {

    Report report(Caller me, ReportCommand command);

    /** The open reports, the oldest first: for the administration. */
    PageResult<Report> openReports(PageQuery page);
}
