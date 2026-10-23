package uk.gov.defra.trade.imports.exceptions;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Answers application exceptions with RFC 7807 problem details that carry the request's trace id.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  private static final String MDC_TRACE_ID = "trace.id";

  /**
   * Handle validation errors (400 Bad Request).
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
    String traceId = MDC.get(MDC_TRACE_ID);
    log.warn("Validation error (trace: {}): {}", traceId, ex.getMessage(), ex);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        "Validation failed for one or more fields"
    );

    problemDetail.setType(URI.create("https://api.cdp.defra.cloud/problems/validation-error"));
    problemDetail.setTitle("Validation Error");

    if (traceId != null) {
      problemDetail.setProperty("traceId", traceId);
    }

    Map<String, String> errors = new HashMap<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      errors.put(error.getField(), error.getDefaultMessage());
    }
    problemDetail.setProperty("errors", errors);

    return problemDetail;
  }

  @ExceptionHandler({
      ConstraintViolationException.class,
      HttpMessageNotReadableException.class,
      MethodArgumentTypeMismatchException.class
  })
  public ProblemDetail handleBadRequestException(Exception ex) {
    String traceId = MDC.get(MDC_TRACE_ID);
    log.warn("Bad request (trace: {})", traceId, ex);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        "The request is not valid"
    );

    problemDetail.setType(URI.create("https://api.cdp.defra.cloud/problems/bad-request"));
    problemDetail.setTitle("Bad Request");

    if (traceId != null) {
      problemDetail.setProperty("traceId", traceId);
    }

    return problemDetail;
  }

  /**
   * Handle not found errors (404 Not Found).
   */
  @ExceptionHandler(NotFoundException.class)
  public ProblemDetail handleNotFoundException(NotFoundException ex) {
    String traceId = MDC.get(MDC_TRACE_ID);
    log.warn("Resource not found (trace: {}): {}", traceId, ex.getMessage(), ex);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.NOT_FOUND,
        ex.getMessage()
    );

    problemDetail.setType(URI.create("https://api.cdp.defra.cloud/problems/not-found"));
    problemDetail.setTitle("Resource Not Found");

    if (traceId != null) {
      problemDetail.setProperty("traceId", traceId);
    }

    return problemDetail;
  }

  /**
   * Handle conflict errors (409 Conflict).
   */
  @ExceptionHandler(ConflictException.class)
  public ProblemDetail handleConflictException(ConflictException ex) {
    String traceId = MDC.get(MDC_TRACE_ID);
    log.warn("Resource conflict (trace: {}): {}", traceId, ex.getMessage(), ex);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT,
        ex.getMessage()
    );

    problemDetail.setType(URI.create("https://api.cdp.defra.cloud/problems/conflict"));
    problemDetail.setTitle("Resource Conflict");

    if (traceId != null) {
      problemDetail.setProperty("traceId", traceId);
    }

    return problemDetail;
  }

  @ExceptionHandler(MdmUnavailableException.class)
  public ProblemDetail handleMdmUnavailableException(MdmUnavailableException ex) {
    String traceId = MDC.get(MDC_TRACE_ID);
    log.warn("MDM unavailable (trace: {})", traceId, ex);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.SERVICE_UNAVAILABLE,
        "Reference data is unavailable. Please try again later."
    );

    problemDetail.setType(URI.create("https://api.cdp.defra.cloud/problems/service-unavailable"));
    problemDetail.setTitle("Service Unavailable");

    if (traceId != null) {
      problemDetail.setProperty("traceId", traceId);
    }

    return problemDetail;
  }

  /**
   * Handle unexpected runtime errors (500 Internal Server Error).
   */
  @ExceptionHandler({
      RuntimeException.class,
      IllegalStateException.class,
      IllegalArgumentException.class
  })
  public ProblemDetail handleException(Exception ex) {
    String traceId = MDC.get(MDC_TRACE_ID);
    log.error("Unexpected error (trace: {}): {}", traceId, ex.getMessage(), ex);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred. Please try again later."
    );

    problemDetail.setType(URI.create("https://api.cdp.defra.cloud/problems/internal-error"));
    problemDetail.setTitle("Internal Server Error");

    if (traceId != null) {
      problemDetail.setProperty("traceId", traceId);
    }

    return problemDetail;
  }
}
