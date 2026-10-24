package es.upm.miw.oneleft.social.infrastructure.rest;

import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.port.in.ReportUseCase;
import es.upm.miw.oneleft.social.infrastructure.config.KeycloakJwt;
import es.upm.miw.oneleft.social.infrastructure.rest.SafetyDtos.ReportPageResponse;
import es.upm.miw.oneleft.social.infrastructure.rest.SafetyDtos.ReportRequest;
import es.upm.miw.oneleft.social.infrastructure.rest.SafetyDtos.ReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Reporting people and plans (HU-033), and the open reports for the administration (HU-013). */
@RestController
@RequestMapping(ReportController.REPORTS)
@Tag(name = "Safety", description = "Blocking and reporting people and plans")
public class ReportController {

    public static final String REPORTS = SocialController.SOCIAL + "/reports";

    private final ReportUseCase reports;

    public ReportController(ReportUseCase reports) {
        this.reports = reports;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Report a person or a plan",
            description = "It waits for the moderation. Reporting the same thing again while it is open gives the "
                    + "same report back.")
    @ApiResponse(responseCode = "201", description = "The report")
    @ApiResponse(responseCode = "400",
            description = "report.target, report.person, report.reason, report.comment or report.self",
            content = @Content)
    @ApiResponse(responseCode = "404", description = "person.notFound", content = @Content)
    public ReportResponse report(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                 @Valid @RequestBody ReportRequest request) {
        return ReportResponse.of(reports.report(KeycloakJwt.caller(jwt), request.toCommand()));
    }

    @GetMapping
    @Operation(summary = "The open reports (administration)", description = "The oldest first. Only for admins.")
    @ApiResponse(responseCode = "200", description = "A page of the open reports")
    @ApiResponse(responseCode = "403", description = "Not an administrator", content = @Content)
    public ReportPageResponse open(@RequestParam(required = false) Integer page,
                                   @RequestParam(required = false) Integer size) {
        return ReportPageResponse.of(reports.openReports(
                new PageQuery(page == null ? 0 : page, size == null ? PageQuery.DEFAULT_SIZE : size)));
    }
}
