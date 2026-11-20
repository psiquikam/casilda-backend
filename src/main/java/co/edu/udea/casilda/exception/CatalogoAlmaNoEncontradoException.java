package co.edu.udea.casilda.exception;

public class CatalogoAlmaNoEncontradoException extends RuntimeException {
    public CatalogoAlmaNoEncontradoException() {
        super("El catálogo solicitado no existe.");
    }
}
