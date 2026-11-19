package co.edu.udea.casilda.service.uad;

import co.edu.udea.casilda.dto.response.uad.UadResponses;
import co.edu.udea.casilda.model.entity.ReporteCaso;
import co.edu.udea.casilda.model.entity.uad.UadCaso;
import co.edu.udea.casilda.model.entity.uad.UadEvento;
import co.edu.udea.casilda.model.entity.uad.UadNoticia;
import co.edu.udea.casilda.model.entity.uad.UadSolicitudInformacion;
import co.edu.udea.casilda.model.enums.TipoReportanteEnum;
import co.edu.udea.casilda.model.enums.uad.AccionNoticia;
import co.edu.udea.casilda.model.enums.uad.TipoNoticia;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Entidades UAD → DTO del contrato. Aplica la <strong>minimización de datos</strong> en el servidor:
 * en una NDA nunca sale identidad ni contacto (UAD-NOT-04, UAD-SEG-06), y en una NDI solo los datos
 * que el reporte trae.
 */
@Component
public class UadNoticiaMapper {

    static final int LIMITE_RESUMEN_HECHOS = 400;

    public UadResponses.NoticiaResumen resumen(final UadNoticia n) {
        final ReporteCaso r = n.getTipo() == TipoNoticia.NDI ? n.getReporteCaso() : null;
        return new UadResponses.NoticiaResumen(
                n.getId().toString(),
                n.getConsecutivo(),
                n.getTipo(),
                n.getCasoId(),
                new UadResponses.Origen(n.getOrigenTipo(), n.getActorRemitente()),
                n.getFechaRecepcion(),
                n.getEstado(),
                n.vinculacionEfectiva(),
                UadCompetencia.esDeterminada(n.vinculacionEfectiva()),
                n.getVersion(),
                null,
                new UadResponses.DetalleExpandible(
                        r == null ? null : r.getContactoWhatsapp(),
                        r == null ? null : correoContacto(r),
                        recortar(n.getDescripcionHechos())));
    }

    public UadResponses.NoticiaDetalle detalle(
            final UadNoticia n,
            final UadSolicitudInformacion solicitud,
            final LocalDate hoy,
            final List<AccionNoticia> acciones,
            final Map<AccionNoticia, String> motivos) {
        final ReporteCaso r = n.getTipo() == TipoNoticia.NDI ? n.getReporteCaso() : null;
        return new UadResponses.NoticiaDetalle(
                n.getId().toString(),
                n.getConsecutivo(),
                n.getTipo(),
                n.getCasoId(),
                n.getExpedienteUad(),
                n.getCasoUad() == null ? null : n.getCasoUad().getId().toString(),
                new UadResponses.Origen(n.getOrigenTipo(), n.getActorRemitente()),
                n.getFechaRecepcion(),
                n.getEstado(),
                n.getVersion(),
                n.getVinculacionDeclarada(),
                n.getVinculacionClasificada(),
                UadCompetencia.equiposConVisibilidad(n.getTipo(), n.vinculacionEfectiva()),
                r == null ? null : reportante(r),
                r == null ? null : personaAfectada(r),
                new UadResponses.Hechos(n.getDescripcionHechos(), n.getLugarOcurrencia(), n.getPresuntoResponsable()),
                r == null ? List.of() : r.getEvidencias().stream()
                        .map(e -> new UadResponses.EvidenciaMetadato(
                                e.getId().toString(), e.getNombreOriginal(), e.getTipoContenido(), e.getTamanoBytes(), e.getFechaRegistro()))
                        .toList(),
                solicitud == null ? null : new UadResponses.SolicitudInformacionVigente(
                        solicitud.getId().toString(),
                        solicitud.getTipoSolicitudCodigo(),
                        solicitud.getInformacionRequerida(),
                        solicitud.getFechaSolicitud(),
                        solicitud.getFechaLimite(),
                        !solicitud.isRespondida() && solicitud.getFechaLimite().isBefore(hoy),
                        solicitud.isRespondida()),
                null,
                acciones,
                motivos);
    }

    public UadResponses.EventoHistorial evento(final UadEvento e) {
        return new UadResponses.EventoHistorial(
                e.getId().toString(), e.getTipo(), e.getFecha(), e.getDescripcion(), e.getProfesional(), e.getEquipo(), e.getFase(),
                e.getEstadoAnterior(), e.getEstadoNuevo(), e.getObservaciones());
    }

    public UadResponses.CasoUadResumen caso(final UadCaso c, final List<String> consecutivos) {
        return new UadResponses.CasoUadResumen(
                c.getId().toString(), c.getCasoId(), c.getExpedienteUad(), consecutivos, c.getVictimaNombre(), c.getVinculacionInvestigado(),
                c.getFechaAdmision(), c.getFaseActual(), c.getEquipoResponsable(), c.getEstadoGestion(), c.getVersion());
    }

    /**
     * El reporte no guarda el nombre de quien reporta; si hubo sesión, se toma del usuario (PROVISIONAL:
     * nombre completo en {@code primerNombre}). Solicitud directa = la persona afectada reporta.
     */
    private UadResponses.Reportante reportante(final ReporteCaso r) {
        final boolean directa = r.getTipoReportante() != TipoReportanteEnum.TERCERO;
        return new UadResponses.Reportante(
                directa ? "DIRECTA" : "INDIRECTA",
                r.getUsuario() == null ? null : r.getUsuario().getNombre(),
                null,
                null,
                null,
                r.getTipoDocumento() == null ? null : r.getTipoDocumento().getNombre(),
                r.getNumeroDocumento(),
                r.getContactoWhatsapp(),
                r.getUsuario() == null ? null : r.getUsuario().getEmail(),
                r.getContactoCorreo());
    }

    /** Solo en solicitud indirecta: en la directa la afectada es quien reporta (UAD-FOR-03). */
    private UadResponses.PersonaAfectada personaAfectada(final ReporteCaso r) {
        if (r.getTipoReportante() != TipoReportanteEnum.TERCERO) return null;
        final String nombre = unir(r.getVictimaPrimerNombre(), r.getVictimaSegundoNombre(), r.getVictimaPrimerApellido(), r.getVictimaSegundoApellido());
        // `victimaCargo` apunta a la tabla de cargos, no al vínculo con la UdeA (P-REP-02): no se expone como vínculo.
        return nombre == null ? null : new UadResponses.PersonaAfectada(nombre, null);
    }

    private String correoContacto(final ReporteCaso r) {
        return r.getContactoCorreo() != null ? r.getContactoCorreo() : (r.getUsuario() == null ? null : r.getUsuario().getEmail());
    }

    static String unir(final String... partes) {
        final String texto = Stream.of(partes).filter(Objects::nonNull).map(String::trim).filter(p -> !p.isEmpty())
                .collect(Collectors.joining(" "));
        return texto.isEmpty() ? null : texto;
    }

    static String recortar(final String texto) {
        if (texto == null) return null;
        return texto.length() <= LIMITE_RESUMEN_HECHOS ? texto : texto.substring(0, LIMITE_RESUMEN_HECHOS);
    }
}
