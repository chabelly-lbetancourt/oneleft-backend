package es.upm.miw.oneleft.reputation.infrastructure.rest;

import es.upm.miw.oneleft.reputation.domain.model.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Maps domain exceptions to HTTP responses in the Problem Details format (RFC 9457). Each problem carries a stable
 * {@code code} property that clients use to show the message in the user's language.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    static final String CODE = "code";
    static final String GENERIC_VALIDATION = "validation";

    @ExceptionHandler(ValidationException.class)
    ProblemDetail invalid(ValidationException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), exception.code());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class, MethodArgumentNotValidException.class,
            MissingServletRequestParameterException.class})
    ProblemDetail badRequest(Exception exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), GENERIC_VALIDATION);
    }

    private static ProblemDetail problem(HttpStatus status, String detail, String code) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty(CODE, code);
        return problem;
    }
}
