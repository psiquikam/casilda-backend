package co.edu.udea.casilda.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Getter
public class AtencionAlmaException extends RuntimeException {
    private final HttpStatus status;
    private final String codigo;
    private final Map<String, ?> detalles;

    public AtencionAlmaException(HttpStatus status, String codigo, String mensaje, Map<String, ?> detalles) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
        this.detalles = detalles;
    }
}
