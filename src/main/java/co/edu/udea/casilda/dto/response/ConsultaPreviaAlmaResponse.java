package co.edu.udea.casilda.dto.response;

import java.time.Instant;
import java.time.OffsetDateTime;

public record ConsultaPreviaAlmaResponse(
        boolean existe,
        Persona persona,
        SeguimientoActivo seguimientoActivo,
        boolean tieneCasoCerrado,
        CasoCerrado ultimoCasoCerrado) {

    public record Persona(Long idPersona, String primerNombre, String segundoNombre,
                          String primerApellido, String segundoApellido) {}
    public record SeguimientoActivo(boolean activo, OffsetDateTime proximoSeguimiento, String profesional) {}
    public record CasoCerrado(String numeroCaso, Instant fechaCierre) {}
}
