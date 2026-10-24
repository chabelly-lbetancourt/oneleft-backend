package es.upm.miw.oneleft.social.application;

import es.upm.miw.oneleft.social.InMemorySafety;
import es.upm.miw.oneleft.social.InMemorySocial;
import es.upm.miw.oneleft.social.domain.model.Caller;
import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.model.Report;
import es.upm.miw.oneleft.social.domain.model.ReportReason;
import es.upm.miw.oneleft.social.domain.model.ReportStatus;
import es.upm.miw.oneleft.social.domain.model.ReportTarget;
import es.upm.miw.oneleft.social.domain.model.SocialException;
import es.upm.miw.oneleft.social.domain.model.ValidationException;
import es.upm.miw.oneleft.social.domain.port.in.ReportCommand;
import es.upm.miw.oneleft.social.domain.port.out.Directory;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-033: reports of people and plans, with their reason, for the moderation. */
class ReportServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-24T18:00:00Z"), ZoneOffset.UTC);
    private final Caller lucia = new Caller(UUID.randomUUID(), "Lucía");
    private final Caller ana = new Caller(UUID.randomUUID(), "Ana");
    private final UUID nobody = UUID.randomUUID();
    private final InMemorySocial social = new InMemorySocial();
    private final InMemorySafety safety = new InMemorySafety(social);
    private final Directory directory = userId -> {
        if (userId.equals(ana.userId()) || userId.equals(lucia.userId())) {
            return userId.equals(ana.userId()) ? "Ana" : "Lucía";
        }
        throw new SocialException(SocialException.Reason.PERSON_NOT_FOUND, "Unknown");
    };
    private final ReportService service = new ReportService(social, directory, safety, safety, CLOCK);

    private static ReportCommand person(UUID userId, ReportReason reason, String comment) {
        return new ReportCommand(ReportTarget.PERSON, userId, null, reason, comment);
    }

    @Test
    void aPersonIsReportedOnceWhileTheReportIsOpen() {
        var report = service.report(lucia, person(ana.userId(), ReportReason.HARASSMENT, "  Insults in the chat  "));

        assertThat(report.reporterId()).isEqualTo(lucia.userId());
        assertThat(report.reportedUserId()).isEqualTo(ana.userId());
        assertThat(report.comment()).isEqualTo("Insults in the chat");
        assertThat(report.status()).isEqualTo(ReportStatus.OPEN);
        assertThat(report.reportedAt()).isEqualTo(CLOCK.instant());
        assertThat(safety.events).containsExactly("report.created " + report.id());
        assertThat(social.people).containsKeys(lucia.userId(), ana.userId());
        // The same report comes back, and the moderation is not told twice
        assertThat(service.report(lucia, person(ana.userId(), ReportReason.SPAM, null))).isEqualTo(report);
        assertThat(safety.events).hasSize(1);
    }

    @Test
    void aPlanIsReportedAboutItsOrganizer() {
        var plan = UUID.randomUUID();

        var report = service.report(lucia, new ReportCommand(ReportTarget.PLAN, plan, ana.userId(),
                ReportReason.FAKE, null));

        assertThat(report.target()).isEqualTo(ReportTarget.PLAN);
        assertThat(report.targetId()).isEqualTo(plan);
        assertThat(report.reportedUserId()).isEqualTo(ana.userId());
        assertThatThrownBy(() -> service.report(lucia, new ReportCommand(ReportTarget.PLAN, UUID.randomUUID(), null,
                ReportReason.FAKE, null)))
                .isInstanceOf(ValidationException.class)
                .hasFieldOrPropertyWithValue("code", "report.person");
    }

    @Test
    void theTargetTheReasonAndTheCommentAreChecked() {
        assertThatThrownBy(() -> service.report(lucia, null)).hasFieldOrPropertyWithValue("code", "report.target");
        assertThatThrownBy(() -> service.report(lucia, person(null, ReportReason.SPAM, null)))
                .hasFieldOrPropertyWithValue("code", "report.target");
        assertThatThrownBy(() -> service.report(lucia, person(ana.userId(), null, null)))
                .hasFieldOrPropertyWithValue("code", "report.reason");
        // Another reason needs a comment that says what happened
        assertThatThrownBy(() -> service.report(lucia, person(ana.userId(), ReportReason.OTHER, "  ")))
                .hasFieldOrPropertyWithValue("code", "report.comment");
        assertThatThrownBy(() -> service.report(lucia, person(ana.userId(), ReportReason.SPAM,
                "a".repeat(Report.MAX_COMMENT + 1))))
                .hasFieldOrPropertyWithValue("code", "report.comment");
        assertThatThrownBy(() -> service.report(lucia, person(lucia.userId(), ReportReason.SPAM, null)))
                .hasFieldOrPropertyWithValue("code", "report.self");
        // Only people with an account are reported
        assertThatThrownBy(() -> service.report(lucia, person(nobody, ReportReason.SPAM, null)))
                .isInstanceOf(SocialException.class)
                .hasFieldOrPropertyWithValue("reason", SocialException.Reason.PERSON_NOT_FOUND);
        assertThat(safety.reports).isEmpty();
        assertThat(safety.events).isEmpty();
    }

    @Test
    void theOpenReportsComeTheOldestFirst() {
        var later = new ReportService(social, directory, safety, safety,
                Clock.offset(CLOCK, Duration.ofMinutes(5)));
        var second = later.report(ana, person(lucia.userId(), ReportReason.SPAM, null));
        var first = service.report(lucia, person(ana.userId(), ReportReason.DANGEROUS, null));

        assertThat(service.openReports(new PageQuery(0, 20)).items()).containsExactly(first, second);
    }
}
