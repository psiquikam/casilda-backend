package co.edu.udea.casilda.dto.response.uad;

import co.edu.udea.casilda.model.enums.uad.AccionNoticia;
import co.edu.udea.casilda.model.enums.uad.ActorRuta;
import co.edu.udea.casilda.model.enums.uad.CapacidadUad;
import co.edu.udea.casilda.model.enums.uad.EquipoUad;
import co.edu.udea.casilda.model.enums.uad.EstadoGestionUad;
import co.edu.udea.casilda.model.enums.uad.EstadoNoticia;
import co.edu.udea.casilda.model.enums.uad.FaseUad;
import co.edu.udea.casilda.model.enums.uad.TipoEventoUad;
import co.edu.udea.casilda.model.enums.uad.TipoNoticia;
import co.edu.udea.casilda.model.enums.uad.TipoOrigenNoticia;
import co.edu.udea.casilda.model.enums.uad.VinculacionInvestigado;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Respuestas del módulo UAD. Los nombres de campo replican {@code openapi-uad.yaml} (y
 * {@code uad-api.types.ts} del frontend): cambiar uno exige cambiar el contrato.
 */
public final class UadResponses {

    private UadResponses() {
    }

    public record ContextoUsuario(List<EquipoUad> equipos, boolean coordinacion, List<CapacidadUad> capacidades) {
    }

    public record Origen(TipoOrigenNoticia tipo, ActorRuta actorRemitente) {
    }

    public record DetalleExpandible(String telefono, String correo, String descripcionHechos) {
    }

    /** Siempre nulo por ahora: la detección de revisión simultánea está pendiente (D-UAD-31). */
    public record RevisionConcurrente(Instant desde, EquipoUad equipo) {
    }

    public record NoticiaResumen(
            String noticiaId,
            String consecutivo,
            TipoNoticia tipo,
            String casoId,
            Origen origen,
            Instant fechaRecepcion,
            EstadoNoticia estado,
            VinculacionInvestigado vinculacionDeclarada,
            boolean clasificada,
            Long version,
            RevisionConcurrente revisionConcurrente,
            DetalleExpandible detalleExpandible
    ) {
    }

    public record Reportante(
            String tipoSolicitud,
            String primerNombre,
            String segundoNombre,
            String primerApellido,
            String segundoApellido,
            String tipoDocumento,
            String numeroDocumento,
            String celular,
            String correoInstitucional,
            String correoPersonal
    ) {
    }

    public record PersonaAfectada(String nombreCompleto, String vinculoUniversidad) {
    }

    public record Hechos(String descripcion, String lugarOcurrencia, String presuntoResponsable) {
    }

    public record EvidenciaMetadato(String evidenciaId, String nombreArchivo, String tipoMime, Long tamanoBytes, Instant fechaCarga) {
    }

    public record SolicitudInformacionVigente(
            String solicitudId,
            String tipoSolicitudCodigo,
            String informacionRequerida,
            Instant fechaSolicitud,
            LocalDate fechaLimite,
            boolean vencida,
            boolean respondida
    ) {
    }

    public record NoticiaDetalle(
            String noticiaId,
            String consecutivo,
            TipoNoticia tipo,
            String casoId,
            String expedienteUad,
            String casoUadId,
            Origen origen,
            Instant fechaRecepcion,
            EstadoNoticia estado,
            Long version,
            VinculacionInvestigado vinculacionDeclarada,
            VinculacionInvestigado vinculacionClasificada,
            List<EquipoUad> equiposCompetentes,
            Reportante reportante,
            PersonaAfectada personaAfectada,
            Hechos hechos,
            List<EvidenciaMetadato> evidencias,
            SolicitudInformacionVigente solicitudInformacion,
            RevisionConcurrente revisionConcurrente,
            List<AccionNoticia> accionesDisponibles,
            Map<AccionNoticia, String> motivosNoDisponible
    ) {
    }

    public record Clasificacion(boolean visibleParaUsuario, NoticiaDetalle noticia) {
    }

    public record CasoAsociado(String casoId, String actor, LocalDate fechaApertura, String estadoGestionActor) {
    }

    public record NotificacionEstado(String evento, String destinatario, String estado, String motivo) {
    }

    public record CasoUadResumen(
            String casoUadId,
            String casoId,
            String expedienteUad,
            List<String> consecutivosNoticia,
            String victimaNombre,
            VinculacionInvestigado vinculacionInvestigado,
            Instant fechaAdmision,
            FaseUad faseActual,
            EquipoUad equipoResponsable,
            EstadoGestionUad estadoGestion,
            Long version
    ) {
    }

    public record DecisionEvaluacion(NoticiaDetalle noticia, CasoUadResumen casoUad, List<NotificacionEstado> notificaciones) {
    }

    public record EventoHistorial(
            String eventoId,
            TipoEventoUad tipo,
            Instant fecha,
            String descripcion,
            String profesional,
            EquipoUad equipo,
            FaseUad fase,
            String estadoAnterior,
            String estadoNuevo,
            String observaciones
    ) {
    }

    /** Solo consecutivo y fecha: nunca datos de quien envía la NDA. */
    public record RadicacionNda(String consecutivo, Instant fechaRecepcion) {
    }
}
