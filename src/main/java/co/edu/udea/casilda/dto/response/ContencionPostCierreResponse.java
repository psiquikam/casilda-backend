package co.edu.udea.casilda.dto.response;

import co.edu.udea.casilda.model.entity.ContencionPostCierre;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record ContencionPostCierreResponse(
        String idContencion, String idAtencion, String numeroCaso, String tipoSeguimiento,
        String nota, OffsetDateTime fechaRegistro, String autor) {

    public static ContencionPostCierreResponse desde(ContencionPostCierre contencion) {
        return new ContencionPostCierreResponse(
                "CON-%04d".formatted(contencion.getId()), contencion.getAtencion().getCodigo(),
                contencion.getCaso().getCodigo(), "POST_CIERRE", contencion.getNota(),
                contencion.getFechaRegistro().atZone(ZoneId.of("America/Bogota")).toOffsetDateTime(),
                contencion.getAutor().getNombre());
    }
}
