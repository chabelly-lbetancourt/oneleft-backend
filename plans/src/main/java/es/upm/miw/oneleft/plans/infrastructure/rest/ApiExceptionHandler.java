package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException;
import es.upm.miw.oneleft.plans.domain.model.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps domain exceptions to HTTP responses in the Problem Details format (RFC 9457). Each problem carries a stable
 * {@code code} property that clients use to show the message in the user's language.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    static final String CODE = "code";
    static final String GENERIC_VALIDATION = "validation";

    @ExceptionHandler(PlanNotFoundException.class)
    ProblemDetail notFound(PlanNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage(), PlanNotFoundException.CODE);
    }

    @ExceptionHandler(ValidationException.class)
    ProblemDetail invalid(ValidationException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), exception.code());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), GENERIC_VALIDATION);
    }

    private static ProblemDetail problem(HttpStatus status, String detail, String code) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty(CODE, code);
        return problem;
    }
}
