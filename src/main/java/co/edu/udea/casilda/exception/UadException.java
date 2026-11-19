package co.edu.udea.casilda.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Error de negocio del módulo UAD con {@code codigo} estable (p. ej. {@code CONFLICTO_VERSION},
 * {@code NOTICIA_YA_DECIDIDA}, {@code SIN_EQUIPO_UAD}) para que el frontend no dependa del texto.
 * Lo traduce {@link UadExceptionHandler}.
 */
@Getter
public class UadException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;
    private final Map<String, String> validationErrors;

    public UadException(final HttpStatus status, final String codigo, final String message) {
        this(status, codigo, message, null);
    }

    public UadException(final HttpStatus status, final String codigo, final String message, final Map<String, String> validationErrors) {
        super(message);
        this.status = status;
        this.codigo = codigo;
        this.validationErrors = validationErrors;
    }

    public static UadException noEncontrado() {
        // Mismo 404 si no existe o si no es visible: el servidor no revela cuál de los dos.
        return new UadException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "No encontrado.");
    }

    public static UadException conflictoVersion() {
        return new UadException(HttpStatus.CONFLICT, "CONFLICTO_VERSION", "Otra persona modificó la noticia mientras usted la evaluaba.");
    }

    public static UadException validacion(final Map<String, String> errores) {
        return new UadException(HttpStatus.BAD_REQUEST, "VALIDACION", "Revise los datos marcados.", errores);
    }
}
