package co.edu.udea.casilda.service.uad;

import co.edu.udea.casilda.dto.request.uad.ExpedienteRequest;
import co.edu.udea.casilda.dto.request.uad.RegistrarActuacionesRequest;
import co.edu.udea.casilda.dto.request.uad.RemisionCompetenciaCasoRequest;
import co.edu.udea.casilda.dto.request.uad.TrasladoRequest;
import co.edu.udea.casilda.dto.response.uad.UadResponses;
import co.edu.udea.casilda.exception.UadException;
import co.edu.udea.casilda.model.entity.uad.UadCaso;
import co.edu.udea.casilda.model.entity.uad.UadEvento;
import co.edu.udea.casilda.model.entity.uad.UadNoticia;
import co.edu.udea.casilda.model.enums.uad.CatalogoUad;
import co.edu.udea.casilda.model.enums.uad.EquipoUad;
import co.edu.udea.casilda.model.enums.uad.EstadoGestionUad;
import co.edu.udea.casilda.model.enums.uad.FaseUad;
import co.edu.udea.casilda.model.enums.uad.TipoEventoUad;
import co.edu.udea.casilda.repository.uad.UadCasoRepository;
import co.edu.udea.casilda.repository.uad.UadEventoRepository;
import co.edu.udea.casilda.repository.uad.UadNoticiaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Casos UAD y «Gestionar caso»: Entrega 2 de {@code openapi-uad.yaml}.
 *
 * <p>Un caso es visible solo para el equipo responsable en la fase actual: el instructor en instrucción y el
 * decisor en decisión (UAD-CAS-04 a 08). Si no existe o no es visible, responde 404 sin revelar cuál de los
 * dos. Cada acción exige la {@code version} leída y deja un {@link UadEvento} append-only. Ninguna acción cierra
 * el caso CASILDA global (UAD-ID-10, UAD-SEG-04) ni envía correos (D-UAD-10).</p>
 */
@Service
@RequiredArgsConstructor
public class UadCasoService {

    private static final String SIN_CORREO = "Pendiente de envío: el servicio de correo aún no existe.";

    private final UadCasoRepository casoRepository;
    private final UadNoticiaRepository noticiaRepository;
    private final UadEventoRepository eventoRepository;
    private final UadUsuarioActual usuarioActual;
    private final UadCatalogoService catalogos;
    private final UadNoticiaMapper mapper;
    private final EntityManager entityManager;

    // ------------------------------------------------------------------ lectura

    @Transactional(readOnly = true)
    public Page<UadResponses.CasoUadResumen> listar(final FaseUad fase, final Pageable pageable) {
        final UadUsuarioActual.Actor actor = usuarioActual.conEquipo();
        final Page<UadCaso> pagina = fase == null
                ? casoRepository.findByEstadoGestionAndEquipoResponsableIn(EstadoGestionUad.ACTIVA, actor.equipos(), pageable)
                : casoRepository.findByEstadoGestionAndEquipoResponsableInAndFaseActual(EstadoGestionUad.ACTIVA, actor.equipos(), fase, pageable);
        final Map<UUID, List<String>> consecutivos = consecutivosPorCaso(pagina.map(UadCaso::getId).getContent());
        return pagina.map(c -> mapper.caso(c, consecutivos.getOrDefault(c.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public UadResponses.CasoUadDetalle obtener(final UUID casoUadId) {
        return detalle(casoVisible(casoUadId, usuarioActual.conEquipo()));
    }

    @Transactional(readOnly = true)
    public List<UadResponses.ActuacionDisponible> catalogoActuaciones(final UUID casoUadId) {
        final UadCaso c = casoVisible(casoUadId, usuarioActual.conEquipo());
        return UadCatalogoActuaciones.paraCaso(c.getFaseActual(), c.getEquipoResponsable(), c.getEstadoGestion());
    }

    @Transactional(readOnly = true)
    public List<UadResponses.EventoHistorial> historial(final UUID casoUadId) {
        final UadCaso c = casoVisible(casoUadId, usuarioActual.conEquipo());
        return eventoRepository.findByCasoUadIdOrderByFechaAscIdAsc(c.getId()).stream().map(mapper::evento).toList();
    }

    /** Pruebas de las noticias admitidas en el caso: se conservan a través de equipos y fases (UAD-PRU-04). */
    @Transactional(readOnly = true)
    public List<UadResponses.EvidenciaMetadato> evidencias(final UUID casoUadId) {
        final UadCaso c = casoVisible(casoUadId, usuarioActual.conEquipo());
        return noticiaRepository.findByCasoUadIdInOrderByFechaRecepcionAsc(List.of(c.getId())).stream()
                .map(UadNoticia::getReporteCaso)
                .filter(Objects::nonNull)
                .flatMap(r -> r.getEvidencias().stream())
                .map(e -> new UadResponses.EvidenciaMetadato(
                        e.getId().toString(), e.getNombreOriginal(), e.getTipoContenido(), e.getTamanoBytes(), e.getFechaRegistro()))
                .toList();
    }

    // ------------------------------------------------------------------ expediente (UAD-ID-07)

    @Transactional
    public UadResponses.ResultadoAccionCaso registrarExpediente(final UUID casoUadId, final ExpedienteRequest cuerpo) {
        final UadUsuarioActual.Actor actor = usuarioActual.conEquipo();
        final UadCaso c = casoVisible(casoUadId, actor);
        exigirActivo(c);
        exigirVersion(c, cuerpo.version());
        final String nuevo = vacioANulo(cuerpo.expedienteUad());
        if (Objects.equals(nuevo, c.getExpedienteUad())) {
            throw UadException.validacion(Map.of("expedienteUad", "Es el mismo número que ya está registrado."));
        }
        final String anterior = c.getExpedienteUad();
        c.setExpedienteUad(nuevo);
        // La noticia muestra el mismo expediente: se mantiene sincronizada con su caso.
        noticiaRepository.findByCasoUadIdInOrderByFechaRecepcionAsc(List.of(c.getId())).forEach(n -> n.setExpedienteUad(nuevo));
        casoRepository.saveAndFlush(c);
        // Valor anterior y nuevo en el historial: corrección auditada, no sobrescritura silenciosa.
        final UadEvento e = evento(c, TipoEventoUad.EXPEDIENTE_ACTUALIZADO,
                "Expediente UAD: " + textoExpediente(anterior) + " → " + textoExpediente(nuevo) + ".",
                actor, c.getFaseActual(), anterior, nuevo, null);
        return resultado(c, actor, List.of(e), List.of());
    }

    // ------------------------------------------------------------------ actuaciones ordinarias (UAD-HIS-01 a 07)

    @Transactional
    public UadResponses.ResultadoAccionCaso registrarActuaciones(final UUID casoUadId, final RegistrarActuacionesRequest cuerpo) {
        final UadUsuarioActual.Actor actor = usuarioActual.conEquipo();
        final UadCaso c = casoVisible(casoUadId, actor);
        exigirActivo(c);
        exigirVersion(c, cuerpo.version());
        final Map<String, String> errores = UadCatalogoActuaciones.validar(cuerpo.actuaciones(),
                UadCatalogoActuaciones.paraCaso(c.getFaseActual(), c.getEquipoResponsable(), c.getEstadoGestion()));
        if (!errores.isEmpty()) {
            throw new UadException(HttpStatus.BAD_REQUEST, "ACTUACION_NO_DISPONIBLE", "Revise los datos marcados.", errores);
        }
        // Una ordinaria no cambia fase ni estado, pero sí la versión: otra persona que tenga el caso abierto
        // debe recargar antes de actuar sobre un historial que ya cambió. PESSIMISTIC_FORCE_INCREMENT sube la
        // versión de inmediato (la optimista lo haría al confirmar y la respuesta llevaría la versión vieja).
        entityManager.lock(c, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        final String observaciones = vacioANulo(cuerpo.observaciones());
        final List<UadEvento> eventos = cuerpo.actuaciones().stream()
                .map(a -> evento(c, TipoEventoUad.ACTUACION, a.nombre() + ".", actor, c.getFaseActual(), null, null, observaciones))
                .toList();
        return resultado(c, actor, eventos, List.of());
    }

    // ------------------------------------------------------------------ traslado a decisión (UAD-CAS-08 a 10)

    /**
     * Solo exige fase de instrucción, gestión activa y confirmación expresa. Requisitos jurídicos adicionales
     * (p. ej. pliego registrado) están pendientes (D-UAD-21) y no se inventan.
     */
    @Transactional
    public UadResponses.ResultadoAccionCaso finalizarInstruccion(final UUID casoUadId, final TrasladoRequest cuerpo) {
        final UadUsuarioActual.Actor actor = usuarioActual.conEquipo();
        final UadCaso c = casoVisible(casoUadId, actor);
        exigirActivo(c);
        if (c.getFaseActual() != FaseUad.INSTRUCCION) {
            throw new UadException(HttpStatus.CONFLICT, "FASE_NO_PERMITE", "El caso no está en instrucción.");
        }
        exigirVersion(c, cuerpo.version());
        if (!Boolean.TRUE.equals(cuerpo.confirmaTraslado())) {
            throw UadException.validacion(Map.of("confirmaTraslado", "Confirme expresamente el paso a la etapa de decisión."));
        }
        final EquipoUad instructor = c.getEquipoResponsable();
        final EquipoUad decisor = UadCompetencia.equipoDecisor(c.getVinculacionInvestigado());
        if (decisor == null) {
            throw new IllegalStateException("Caso UAD " + c.getId() + " sin vinculación determinada");
        }
        // Conserva casoId, expediente, admisión, evidencias e historial (UAD-CAS-09).
        c.setFaseActual(FaseUad.DECISION);
        c.setEquipoResponsable(decisor);
        c.setFechaUltimoTraslado(Instant.now());
        casoRepository.saveAndFlush(c);
        final UadEvento e = evento(c, TipoEventoUad.TRASLADO_FASE,
                "Finalizó la etapa de instrucción. El caso pasó de " + etiqueta(instructor) + " (instrucción) a " + etiqueta(decisor) + " (decisión).",
                actor, instructor, FaseUad.INSTRUCCION, FaseUad.INSTRUCCION.name(), FaseUad.DECISION.name(), vacioANulo(cuerpo.observaciones()));
        final UadResponses.NotificacionEstado notificacion = c.getVictimaNombre() == null
                ? new UadResponses.NotificacionEstado("CAMBIO_FASE", "VICTIMA", "NO_APLICA", "Sin contacto autorizado de la víctima.")
                : new UadResponses.NotificacionEstado("CAMBIO_FASE", "VICTIMA", "PENDIENTE", SIN_CORREO);
        return resultado(c, actor, List.of(e), List.of(notificacion));
    }

    // ------------------------------------------------------------------ remisión por competencia (UAD-REM-01 a 06)

    /** Solo registra: Casilda no envía nada a la entidad. La gestión UAD termina; el caso CASILDA no se cierra. */
    @Transactional
    public UadResponses.ResultadoAccionCaso remitirPorCompetencia(final UUID casoUadId, final RemisionCompetenciaCasoRequest cuerpo) {
        final UadUsuarioActual.Actor actor = usuarioActual.conEquipo();
        final UadCaso c = casoVisible(casoUadId, actor);
        exigirActivo(c);
        exigirVersion(c, cuerpo.version());
        final Map<String, String> errores = new LinkedHashMap<>();
        if (!catalogos.existe(CatalogoUad.ENTIDAD_REMISION, cuerpo.entidadDestinatariaCodigo())) {
            errores.put("entidadDestinatariaCodigo", "Seleccione una entidad de la lista.");
        }
        if (cuerpo.fechaRemision().isAfter(LocalDate.now(UadNoticiaService.BOGOTA))) {
            errores.put("fechaRemision", "La fecha de remisión no puede ser futura.");
        }
        if (!errores.isEmpty()) throw UadException.validacion(errores);

        c.setEstadoGestion(EstadoGestionUad.FINALIZADA);
        c.setMotivoFinGestion("REMITIDA_POR_COMPETENCIA");
        casoRepository.saveAndFlush(c);
        final UadEvento e = evento(c, TipoEventoUad.REMISION_COMPETENCIA,
                "Remisión por competencia a " + catalogos.nombreDe(CatalogoUad.ENTIDAD_REMISION, cuerpo.entidadDestinatariaCodigo())
                        + " (" + cuerpo.fechaRemision() + ").",
                actor, c.getFaseActual(), EstadoGestionUad.ACTIVA.name(), EstadoGestionUad.FINALIZADA.name(), vacioANulo(cuerpo.justificacion()));
        return resultado(c, actor, List.of(e), List.of());
    }

    // ------------------------------------------------------------------ apoyo

    /** 404 si no existe o no es del equipo y la fase del usuario: no se revela cuál de los dos. */
    UadCaso casoVisible(final UUID id, final UadUsuarioActual.Actor actor) {
        final UadCaso c = casoRepository.findById(id).orElseThrow(UadException::noEncontrado);
        if (!esVisible(c, actor)) throw UadException.noEncontrado();
        return c;
    }

    static boolean esVisible(final UadCaso c, final UadUsuarioActual.Actor actor) {
        return actor.equipos().contains(c.getEquipoResponsable());
    }

    private static void exigirActivo(final UadCaso c) {
        if (c.getEstadoGestion() != EstadoGestionUad.ACTIVA) {
            throw new UadException(HttpStatus.CONFLICT, "CASO_NO_ACTIVO", UadCatalogoActuaciones.GESTION_FINALIZADA);
        }
    }

    private static void exigirVersion(final UadCaso c, final Long version) {
        if (version == null || !version.equals(c.getVersion())) {
            throw new UadException(HttpStatus.CONFLICT, "CONFLICTO_VERSION", "Otra persona modificó el caso mientras usted lo gestionaba.");
        }
    }

    private UadResponses.ResultadoAccionCaso resultado(
            final UadCaso c, final UadUsuarioActual.Actor actor, final List<UadEvento> eventos, final List<UadResponses.NotificacionEstado> notificaciones) {
        final boolean visible = esVisible(c, actor);
        return new UadResponses.ResultadoAccionCaso(visible, visible ? detalle(c) : null,
                eventos.stream().map(mapper::evento).toList(), List.copyOf(notificaciones));
    }

    UadResponses.CasoUadDetalle detalle(final UadCaso c) {
        final List<String> consecutivos = consecutivosPorCaso(List.of(c.getId())).getOrDefault(c.getId(), List.of());
        return new UadResponses.CasoUadDetalle(
                c.getId().toString(), c.getCasoId(), c.getExpedienteUad(), consecutivos, c.getVictimaNombre(), c.getVinculacionInvestigado(),
                c.getFechaAdmision(), c.getFaseActual(), c.getEquipoResponsable(), c.getEstadoGestion(), c.getVersion(),
                c.getMotivoFinGestion(), c.getFechaUltimoTraslado(),
                UadCompetencia.equipoInstructor(c.getVinculacionInvestigado()), UadCompetencia.equipoDecisor(c.getVinculacionInvestigado()));
    }

    private Map<UUID, List<String>> consecutivosPorCaso(final List<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return noticiaRepository.findByCasoUadIdInOrderByFechaRecepcionAsc(ids).stream()
                .collect(Collectors.groupingBy(n -> n.getCasoUad().getId(), Collectors.mapping(UadNoticia::getConsecutivo, Collectors.toList())));
    }

    private UadEvento evento(
            final UadCaso c, final TipoEventoUad tipo, final String descripcion, final UadUsuarioActual.Actor actor, final FaseUad fase,
            final String estadoAnterior, final String estadoNuevo, final String observaciones) {
        return evento(c, tipo, descripcion, actor, c.getEquipoResponsable(), fase, estadoAnterior, estadoNuevo, observaciones);
    }

    /** {@code equipo} es el que actuó: el responsable del caso en el momento de la acción. */
    private UadEvento evento(
            final UadCaso c, final TipoEventoUad tipo, final String descripcion, final UadUsuarioActual.Actor actor, final EquipoUad equipo,
            final FaseUad fase, final String estadoAnterior, final String estadoNuevo, final String observaciones) {
        final UadEvento e = new UadEvento();
        e.setCasoUad(c);
        e.setTipo(tipo);
        e.setFecha(Instant.now());
        e.setDescripcion(descripcion);
        e.setProfesional(actor.nombre());
        e.setEquipo(equipo);
        e.setFase(fase);
        e.setEstadoAnterior(estadoAnterior);
        e.setEstadoNuevo(estadoNuevo);
        e.setObservaciones(observaciones);
        return eventoRepository.save(e);
    }

    private static String etiqueta(final EquipoUad equipo) {
        return equipo == EquipoUad.EQUIPO_3 ? "Equipo 3" : "Equipo 4";
    }

    private static String textoExpediente(final String valor) {
        return valor == null ? "sin registrar" : valor;
    }

    private static String vacioANulo(final String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
