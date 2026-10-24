package es.upm.miw.oneleft.social.application;

import es.upm.miw.oneleft.social.domain.model.Caller;
import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.model.PageResult;
import es.upm.miw.oneleft.social.domain.model.Report;
import es.upm.miw.oneleft.social.domain.model.ReportTarget;
import es.upm.miw.oneleft.social.domain.model.ValidationException;
import es.upm.miw.oneleft.social.domain.port.in.ReportCommand;
import es.upm.miw.oneleft.social.domain.port.in.ReportUseCase;
import es.upm.miw.oneleft.social.domain.port.out.Directory;
import es.upm.miw.oneleft.social.domain.port.out.People;
import es.upm.miw.oneleft.social.domain.port.out.Reports;
import es.upm.miw.oneleft.social.domain.port.out.SafetyEvents;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Reporting people and plans (HU-033). Each report is kept for the administration panel (HU-013) and announced to the
 * moderation (HU-011); a person who reports the same thing again while it is open gets the same report back.
 */
@Service
@Transactional
public class ReportService implements ReportUseCase {

    private final Registration registration;
    private final Reports reports;
    private final SafetyEvents events;
    private final Clock clock;

    public ReportService(People people, Directory directory, Reports reports, SafetyEvents events, Clock clock) {
        this.registration = new Registration(people, directory);
        this.reports = reports;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public Report report(Caller me, ReportCommand command) {
        if (command == null) {
            throw new ValidationException("report.target", "Say which person or plan is reported");
        }
        var reported = command.target() == ReportTarget.PERSON ? command.targetId() : command.organizerId();
        var report = Report.file(me.userId(), command.target(), command.targetId(), reported, command.reason(),
                command.comment(), clock);
        var open = reports.findOpen(me.userId(), report.target(), report.targetId());
        if (open.isPresent()) {
            return open.get();
        }
        registration.register(me);
        if (report.target() == ReportTarget.PERSON) {
            // Only people with an account are reported (a 404 otherwise)
            registration.known(report.reportedUserId());
        }
        var saved = reports.save(report);
        events.reported(saved);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Report> openReports(PageQuery page) {
        return reports.open(page);
    }
}
