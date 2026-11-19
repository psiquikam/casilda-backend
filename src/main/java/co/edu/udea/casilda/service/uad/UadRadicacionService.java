package co.edu.udea.casilda.service.uad;

import co.edu.udea.casilda.dto.request.uad.RadicacionNdaRequest;
import co.edu.udea.casilda.dto.response.uad.UadResponses;
import co.edu.udea.casilda.model.entity.ReporteCaso;
import co.edu.udea.casilda.model.entity.uad.UadEvento;
import co.edu.udea.casilda.model.entity.uad.UadNoticia;
import co.edu.udea.casilda.model.enums.uad.EstadoNoticia;
import co.edu.udea.casilda.model.enums.uad.TipoEventoUad;
import co.edu.udea.casilda.model.enums.uad.TipoNoticia;
import co.edu.udea.casilda.model.enums.uad.TipoOrigenNoticia;
import co.edu.udea.casilda.model.enums.uad.VinculacionInvestigado;
import co.edu.udea.casilda.repository.ReporteCasoRepository;
import co.edu.udea.casilda.repository.uad.UadEventoRepository;
import co.edu.udea.casilda.repository.uad.UadNoticiaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

/**
 * Entrada de noticias a la UAD.
 *
 * <ul>
 *   <li><strong>NDA</strong> (queja anónima, UAD-FOR-12): no lee el {@code SecurityContext} ni guarda IP,
 *       user-agent o autor. El controlador la invoca dentro de {@link UadSinIdentidad}, así que la auditoría
 *       de {@code Auditable} registra {@code ANONYMOUS} aunque llegue un token válido (UAD-NOT-04, UAD-SEG-06).</li>
 *   <li><strong>NDI</strong> desde {@code POST /reportes} (UAD-NOT-02/03): escucha
 *       {@link ReporteCasoRegistradoEvent} <em>después</em> del commit del reporte. Si la creación de la
 *       noticia falla, el reporte ya quedó guardado y el error se registra en el log: el reporte de la
 *       persona nunca se pierde por la UAD.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UadRadicacionService {

    /** Código de la ruta de activación UAD en el catálogo {@code rutaactivacion}. */
    public static final String RUTA_UAD = "UAD_3_4";

    private final UadNoticiaRepository noticiaRepository;
    private final UadEventoRepository eventoRepository;
    private final ReporteCasoRepository reporteCasoRepository;
    private final UadConsecutivoService consecutivos;

    /** Evento que publica {@code ReporteCasoService} al guardar un reporte. */
    public record ReporteCasoRegistradoEvent(Long reporteCasoId) {
    }

    @Transactional
    public UadResponses.RadicacionNda radicarNda(final RadicacionNdaRequest cuerpo) {
        final UadNoticia n = new UadNoticia();
        n.setTipo(TipoNoticia.NDA);
        n.setConsecutivo(consecutivos.siguiente(TipoNoticia.NDA));
        n.setOrigenTipo(TipoOrigenNoticia.FORMULARIO_ANONIMO);
        n.setFechaRecepcion(Instant.now());
        n.setEstado(EstadoNoticia.RECIBIDA);
        n.setVinculacionDeclarada(cuerpo.vinculacionDeclarada());
        n.setDescripcionHechos(cuerpo.descripcionHechos().trim());
        n.setLugarOcurrencia(texto(cuerpo.lugarOcurrencia()));
        n.setPresuntoResponsable(texto(cuerpo.presuntoResponsable()));
        noticiaRepository.save(n);
        // Evento sin profesional ni equipo: nadie queda asociado a la radicación anónima.
        registrar(n, "Se registró la noticia " + n.getConsecutivo() + " (formulario de queja anónima).");
        return new UadResponses.RadicacionNda(n.getConsecutivo(), n.getFechaRecepcion());
    }

    /**
     * Crea la NDI de un reporte dirigido a la UAD. PROVISIONAL (D-UAD-17): aplica si la ruta es
     * {@code UAD_3_4} o si la persona autorizó remitir a la UAD ({@code agresorRemitirUad}). Idempotente.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alRegistrarReporte(final ReporteCasoRegistradoEvent evento) {
        try {
            reporteCasoRepository.findById(evento.reporteCasoId()).ifPresent(this::crearNdiSiCorresponde);
        } catch (RuntimeException ex) {
            log.error("UAD: no se pudo crear la NDI del reporte {}; el reporte quedó guardado", evento.reporteCasoId(), ex);
        }
    }

    void crearNdiSiCorresponde(final ReporteCaso r) {
        final boolean rutaUad = r.getRutaActivacion() != null && RUTA_UAD.equalsIgnoreCase(r.getRutaActivacion().getCodigo());
        if (!rutaUad && !Boolean.TRUE.equals(r.getAgresorRemitirUad())) return;
        if (noticiaRepository.findByReporteCasoId(r.getId()).isPresent()) return;

        final VinculacionInvestigado vinculacion = UadMapeoVinculacion.desdeVinculoUdeA(
                r.getAgresorVinculoUdeA() == null ? null : r.getAgresorVinculoUdeA().getId());
        final UadNoticia n = new UadNoticia();
        n.setTipo(TipoNoticia.NDI);
        n.setConsecutivo(consecutivos.siguiente(TipoNoticia.NDI));
        n.setOrigenTipo(TipoOrigenNoticia.FORMULARIO_WEB_CASILDA);
        n.setFechaRecepcion(r.getFechaRegistro() == null ? Instant.now() : r.getFechaRegistro());
        n.setEstado(UadCompetencia.esDeterminada(vinculacion) ? EstadoNoticia.RECIBIDA : EstadoNoticia.PENDIENTE_CLASIFICACION);
        n.setVinculacionDeclarada(vinculacion);
        n.setReporteCaso(r);
        n.setDescripcionHechos(r.getDescripcionHechos());
        n.setLugarOcurrencia(lugar(r));
        n.setPresuntoResponsable(presunto(r));
        noticiaRepository.save(n);
        registrar(n, "Se registró la noticia " + n.getConsecutivo() + " desde el formulario web Casilda.");
        log.info("UAD: NDI {} creada desde el reporte {}", n.getConsecutivo(), r.getId());
    }

    private void registrar(final UadNoticia n, final String descripcion) {
        final UadEvento e = new UadEvento();
        e.setNoticia(n);
        e.setTipo(TipoEventoUad.NOTICIA_RECIBIDA);
        e.setFecha(n.getFechaRecepcion());
        e.setDescripcion(descripcion);
        e.setEstadoNuevo(n.getEstado().name());
        eventoRepository.save(e);
    }

    private static String lugar(final ReporteCaso r) {
        if ("externo".equalsIgnoreCase(r.getTipoLugar())) return r.getLugarDetalleExterno();
        return UadNoticiaMapper.unir(r.getCampus() == null ? null : r.getCampus().getNombre(), r.getFacultad(), r.getBloque(), r.getEspacio());
    }

    private static String presunto(final ReporteCaso r) {
        final String nombre = UadNoticiaMapper.unir(r.getAgresorPrimerNombre(), r.getAgresorSegundoNombre(), r.getAgresorPrimerApellido(),
                r.getAgresorSegundoApellido());
        final String info = texto(r.getAgresorInformacionAdicional());
        final String completo = nombre == null ? info : (info == null ? nombre : nombre + ". " + info);
        return completo == null ? null : (completo.length() <= 300 ? completo : completo.substring(0, 300));
    }

    private static String texto(final String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
