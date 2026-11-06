package co.edu.udea.casilda.exception;

import lombok.Getter;

import java.util.Map;

/**
 * Excepción para validaciones de negocio asociadas a campos del request.
 */
@Getter
public class FieldValidationException extends RuntimeException {

    private final Map<String, String> validationErrors;

    public FieldValidationException(final Map<String, String> validationErrors) {
        super("Datos inválidos");
        this.validationErrors = validationErrors;
    }
}
