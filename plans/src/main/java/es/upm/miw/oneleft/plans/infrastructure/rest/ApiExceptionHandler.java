package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones del dominio a Problem Details (RFC 9457).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(PlanNotFoundException.class)
    ProblemDetail notFound(PlanNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
