package br.com.viafluvial.acervodigital.common.error;

import br.com.viafluvial.acervodigital.domain.exception.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    ResponseEntity<org.springframework.http.ProblemDetail> handleDomain(DomainException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getHttpStatusCode());
        org.springframework.http.ProblemDetail problem = org.springframework.http.ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setTitle(ex.getCode());
        problem.setType(URI.create("urn:viafluvial:error:" + ex.getCode().toLowerCase()));
        problem.setProperty("timestamp", OffsetDateTime.now().toString());
        problem.setProperty("path", request.getRequestURI());
        return ResponseEntity.status(status).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<org.springframework.http.ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        org.springframework.http.ProblemDetail problem = org.springframework.http.ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados da requisição inválidos.");
        problem.setTitle("VALIDATION_ERROR");
        problem.setType(URI.create("urn:viafluvial:error:validation_error"));
        problem.setProperty("timestamp", OffsetDateTime.now().toString());
        problem.setProperty("path", request.getRequestURI());

        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> fields.put(err.getField(), err.getDefaultMessage()));
        problem.setProperty("errors", fields);

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<org.springframework.http.ProblemDetail> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        String detail = ex.getConstraintViolations().stream()
            .findFirst()
            .map(violation -> {
                String path = violation.getPropertyPath() == null ? "parametro" : violation.getPropertyPath().toString();
                String message = violation.getMessage() == null ? "valor invalido" : violation.getMessage();
                return path + ": " + message;
            })
            .orElse("Dados da requisição inválidos.");

        org.springframework.http.ProblemDetail problem = org.springframework.http.ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("VALIDATION_ERROR");
        problem.setType(URI.create("urn:viafluvial:error:validation_error"));
        problem.setProperty("timestamp", OffsetDateTime.now().toString());
        problem.setProperty("path", request.getRequestURI());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<org.springframework.http.ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String parameter = ex.getName() == null || ex.getName().isBlank() ? "parametro" : ex.getName();
        String rejectedValue = ex.getValue() == null ? "null" : String.valueOf(ex.getValue());
        String detail = "Parametro invalido para " + parameter + ": " + rejectedValue;

        org.springframework.http.ProblemDetail problem = org.springframework.http.ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("VALIDATION_ERROR");
        problem.setType(URI.create("urn:viafluvial:error:validation_error"));
        problem.setProperty("timestamp", OffsetDateTime.now().toString());
        problem.setProperty("path", request.getRequestURI());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<org.springframework.http.ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        org.springframework.http.ProblemDetail problem = org.springframework.http.ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage() == null ? "Acesso negado." : ex.getMessage());
        problem.setTitle("FORBIDDEN");
        problem.setType(URI.create("urn:viafluvial:error:forbidden"));
        problem.setProperty("timestamp", OffsetDateTime.now().toString());
        problem.setProperty("path", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<org.springframework.http.ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        String reason = ex.getClass().getSimpleName() + (ex.getMessage() != null && !ex.getMessage().isBlank() ? ": " + ex.getMessage() : "");
        LOGGER.error("Erro inesperado em {} {} -> {}", request.getMethod(), request.getRequestURI(), reason, ex);
        org.springframework.http.ProblemDetail problem = org.springframework.http.ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado.");
        problem.setTitle("INTERNAL_ERROR");
        problem.setType(URI.create("urn:viafluvial:error:internal_error"));
        problem.setProperty("timestamp", OffsetDateTime.now().toString());
        problem.setProperty("path", request.getRequestURI());
        problem.setProperty("reason", reason);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}
