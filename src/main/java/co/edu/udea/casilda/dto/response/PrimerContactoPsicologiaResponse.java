package co.edu.udea.casilda.dto.response;

/** Resultado de consolidar el primer contacto de Psicología. */
public record PrimerContactoPsicologiaResponse(
        String numeroCaso, String idAtencion, int version, String estadoAtencion, boolean vinculado) {
}
