package co.edu.udea.casilda.service.uad;

import co.edu.udea.casilda.dto.response.uad.UadResponses;
import co.edu.udea.casilda.exception.UadException;
import co.edu.udea.casilda.model.entity.Usuario;
import co.edu.udea.casilda.model.entity.uad.UadNoticia;
import co.edu.udea.casilda.model.entity.uad.UadSolicitudInformacion;
import co.edu.udea.casilda.model.enums.uad.EstadoNoticia;
import co.edu.udea.casilda.model.enums.uad.TipoEventoUad;
import co.edu.udea.casilda.repository.UsuarioRepository;
import co.edu.udea.casilda.repository.uad.UadNoticiaRepository;
import co.edu.udea.casilda.repository.uad.UadSolicitudInformacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * «Mis reportes» y respuesta a la solicitud de información (UAD-NOT-02, UAD-EVA-04/05) — Entrega 3 de
 * {@code openapi-uad.yaml}.
 *
 * <p><strong>El dueño se filtra siempre en la consulta</strong> ({@code ReporteCaso.usuario}): un reporte ajeno
 * responde 404, igual que uno inexistente (D-UAD-16). Las NDA y los reportes hechos sin sesión no tienen
 * dueño y nunca aparecen aquí (UAD-NOT-04, D-UAD-26).</p>
 *
 * <p>Alcance de esta versión: solo los reportes que llegaron a la UAD. Los demás actores de la Ruta se
 * incorporan cuando definan qué estado puede ver la persona usuaria.</p>
 */
@Service
@RequiredArgsConstructor
public class UadMisReportesService {

    public static final int LIMITE_RESPUESTA = 4000;
    static final String ACTOR_UAD = "UAD";

    private final UadNoticiaRepository noticiaRepository;
    private final UadSolicitudInformacionRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final UadNoticiaService noticiaService;
    private final UadNoticiaMapper mapper;

    @Transactional(readOnly = true)
    public Page<UadResponses.MiReporteResumen> listar(final Pageable pageable) {
        final Long usuarioId = usuarioActual().getId();
        return noticiaRepository.findByReporteCasoUsuarioId(usuarioId, pageable).map(n -> resumen(n, solicitudVigente(n)));
    }

    @Transactional(readOnly = true)
    public UadResponses.MiReporteDetalle obtener(final String codigo) {
        final UadNoticia n = noticiaPropia(codigo);
        return detalle(n, solicitudVigente(n));
    }

    /**
     * Registra la respuesta del reportante y pasa la noticia a {@code SUBSANACION_RECIBIDA}. Responder después
     * de la fecha límite está permitido: el vencimiento no cierra la noticia (UAD-EVA-05).
     */
    @Transactional
    public UadResponses.MiReporteDetalle responder(final String codigo, final UUID solicitudId, final String respuesta, final boolean conEvidencias) {
        final UadNoticia n = noticiaPropia(codigo);
        final UadSolicitudInformacion s = solicitudRepository.findById(solicitudId)
                .filter(x -> x.getNoticia().getId().equals(n.getId()))
                .orElseThrow(UadException::noEncontrado);

        final String texto = respuesta == null ? "" : respuesta.trim();
        if (texto.isEmpty()) throw UadException.validacion(Map.of("respuesta", "Escriba su respuesta."));
        if (texto.length() > LIMITE_RESPUESTA) {
            throw UadException.validacion(Map.of("respuesta", "Máximo " + LIMITE_RESPUESTA + " caracteres."));
        }
        // PENDIENTE (D-UAD-09): tipos, tamaños y custodia de archivos sin aprobar. No se descartan en silencio.
        if (conEvidencias) {
            throw new UadException(HttpStatus.BAD_REQUEST, "EVIDENCIAS_NO_HABILITADAS",
                    "Por ahora la respuesta solo admite texto.", Map.of("evidencias", "Por ahora la respuesta solo admite texto."));
        }
        if (s.isRespondida()) {
            throw new UadException(HttpStatus.CONFLICT, "SOLICITUD_YA_RESPONDIDA", "Esta solicitud ya fue respondida.");
        }
        if (n.getEstado() != EstadoNoticia.PENDIENTE_SUBSANACION) {
            throw new UadException(HttpStatus.CONFLICT, "SOLICITUD_NO_VIGENTE", "Esta solicitud ya no está pendiente de respuesta.");
        }

        s.setRespuesta(texto);
        s.setFechaRespuesta(Instant.now());
        s.setRespondida(true);
        solicitudRepository.save(s);
        n.setEstado(EstadoNoticia.SUBSANACION_RECIBIDA);
        noticiaRepository.saveAndFlush(n);
        // Sin profesional ni equipo: el evento lo origina el reportante, no la UAD.
        noticiaService.evento(n, null, TipoEventoUad.SUBSANACION_RECIBIDA, "El reportante respondió la solicitud de información.", null,
                EstadoNoticia.PENDIENTE_SUBSANACION.name(), EstadoNoticia.SUBSANACION_RECIBIDA.name(), null);
        return detalle(n, s);
    }

    // ------------------------------------------------------------------ apoyo

    /**
     * PROVISIONAL (D-UAD-32): estado en lenguaje para la persona usuaria. No distingue inhibitoria de remisión ni
     * revela pasos internos; el texto definitivo lo valida UAD jurídica.
     */
    static String estadoParaReportante(final EstadoNoticia estado) {
        return switch (estado) {
            case RECIBIDA, PENDIENTE_CLASIFICACION -> "Recibido";
            case EN_EVALUACION_PRELIMINAR, SUBSANACION_RECIBIDA, EN_REEVALUACION -> "En revisión";
            case PENDIENTE_SUBSANACION -> "Información adicional solicitada";
            case PROCEDE_TRAMITE -> "En trámite";
            case INHIBITORIA, REMITIDA_POR_COMPETENCIA -> "Revisión finalizada";
        };
    }

    private UadResponses.MiReporteResumen resumen(final UadNoticia n, final UadSolicitudInformacion s) {
        return new UadResponses.MiReporteResumen(
                n.getReporteCaso().getCodigo(), n.getConsecutivo(), ACTOR_UAD, estadoParaReportante(n.getEstado()),
                n.getFechaRecepcion(), ultimaActuacion(s), pendiente(n, s));
    }

    private UadResponses.MiReporteDetalle detalle(final UadNoticia n, final UadSolicitudInformacion s) {
        return new UadResponses.MiReporteDetalle(
                n.getReporteCaso().getCodigo(), n.getConsecutivo(), ACTOR_UAD, estadoParaReportante(n.getEstado()),
                n.getFechaRecepcion(), ultimaActuacion(s), pendiente(n, s),
                mapper.solicitud(s, LocalDate.now(UadNoticiaService.BOGOTA)));
    }

    private static boolean pendiente(final UadNoticia n, final UadSolicitudInformacion s) {
        return s != null && !s.isRespondida() && n.getEstado() == EstadoNoticia.PENDIENTE_SUBSANACION;
    }

    /** Solo lo que la persona ya conoce: cuándo le pidieron información o cuándo respondió. Nunca actividad interna. */
    private static Instant ultimaActuacion(final UadSolicitudInformacion s) {
        if (s == null) return null;
        return s.getFechaRespuesta() != null ? s.getFechaRespuesta() : s.getFechaSolicitud();
    }

    private UadSolicitudInformacion solicitudVigente(final UadNoticia n) {
        return solicitudRepository.findFirstByNoticiaIdOrderByFechaSolicitudDesc(n.getId()).orElse(null);
    }

    private UadNoticia noticiaPropia(final String codigo) {
        final Long usuarioId = usuarioActual().getId();
        if (codigo == null || codigo.isBlank()) throw UadException.noEncontrado();
        return noticiaRepository.findByReporteCasoCodigoAndReporteCasoUsuarioId(codigo.trim(), usuarioId).orElseThrow(UadException::noEncontrado);
    }

    private Usuario usuarioActual() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new UadException(HttpStatus.UNAUTHORIZED, "SIN_SESION", "Inicie sesión para continuar.");
        }
        return usuarioRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new UadException(HttpStatus.UNAUTHORIZED, "SIN_SESION", "Inicie sesión para continuar."));
    }
}
