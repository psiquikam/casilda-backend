package co.edu.udea.casilda.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Errores del módulo UAD. Devuelve los mismos campos que {@code ErrorResponse}/{@code ValidationErrorResponse}
 * más {@code codigo} (extensión propuesta en {@code openapi-uad.yaml}), sin cambiar el formato global.
 * Va antes que {@link GlobalExceptionHandler} y solo aplica a los controladores de {@code controller.uad}.
 */
@RestControllerAdvice(basePackages = "co.edu.udea.casilda.controller.uad")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UadExceptionHandler {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UadErrorResponse(
            LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String path,
            String codigo,
            Map<String, String> validationErrors
    ) {
    }

    @ExceptionHandler(UadException.class)
    public ResponseEntity<UadErrorResponse> handleUad(final UadException ex, final WebRequest request) {
        return respuesta(ex.getStatus(), ex.getCodigo(), ex.getMessage(), ex.getValidationErrors(), request);
    }

    /** Dos transacciones sobre la misma noticia o caso: la segunda pierde con 409 (bloqueo optimista). */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<UadErrorResponse> handleVersion(final ObjectOptimisticLockingFailureException ex, final WebRequest request) {
        return respuesta(HttpStatus.CONFLICT, "CONFLICTO_VERSION", "La información cambió mientras trabajaba.", null, request);
    }

    private ResponseEntity<UadErrorResponse> respuesta(
            final HttpStatus status, final String codigo, final String mensaje, final Map<String, String> errores, final WebRequest request) {
        final UadErrorResponse cuerpo = new UadErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensaje,
                request.getDescription(false).replace("uri=", ""),
                codigo,
                errores);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
