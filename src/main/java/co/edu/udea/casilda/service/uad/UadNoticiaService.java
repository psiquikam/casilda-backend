package co.edu.udea.casilda.service.uad;

import co.edu.udea.casilda.dto.request.uad.ClasificacionUadRequest;
import co.edu.udea.casilda.dto.request.uad.DecisionEvaluacionRequest;
import co.edu.udea.casilda.dto.response.uad.UadResponses;
import co.edu.udea.casilda.exception.UadException;
import co.edu.udea.casilda.model.entity.Caso;
import co.edu.udea.casilda.model.entity.EvidenciaReporte;
import co.edu.udea.casilda.model.entity.ReporteCaso;
import co.edu.udea.casilda.model.entity.uad.UadCaso;
import co.edu.udea.casilda.model.entity.uad.UadEvento;
import co.edu.udea.casilda.model.entity.uad.UadNoticia;
import co.edu.udea.casilda.model.entity.uad.UadSolicitudInformacion;
import co.edu.udea.casilda.model.enums.TipoReportanteEnum;
import co.edu.udea.casilda.model.enums.uad.AccionNoticia;
import co.edu.udea.casilda.model.enums.uad.CapacidadUad;
import co.edu.udea.casilda.model.enums.uad.CatalogoUad;
import co.edu.udea.casilda.model.enums.uad.EquipoUad;
import co.edu.udea.casilda.model.enums.uad.EstadoGestionUad;
import co.edu.udea.casilda.model.enums.uad.EstadoNoticia;
import co.edu.udea.casilda.model.enums.uad.FaseUad;
import co.edu.udea.casilda.model.enums.uad.ModoCaso;
import co.edu.udea.casilda.model.enums.uad.TipoEventoUad;
import co.edu.udea.casilda.model.enums.uad.TipoNoticia;
import co.edu.udea.casilda.model.enums.uad.VinculacionInvestigado;
import co.edu.udea.casilda.repository.CasoRepository;
import co.edu.udea.casilda.repository.EvidenciaReporteRepository;
import co.edu.udea.casilda.repository.uad.UadCasoRepository;
import co.edu.udea.casilda.repository.uad.UadEventoRepository;
import co.edu.udea.casilda.repository.uad.UadNoticiaRepository;
import co.edu.udea.casilda.repository.uad.UadSolicitudInformacionRepository;
import co.edu.udea.casilda.service.ReporteEvidenciaStorageService;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Noticias disciplinarias y evaluación preliminar UAD — Entrega 1 de {@code openapi-uad.yaml}.
 *
 * <p>La autorización real vive aquí: la ruta solo exige un rol UAD; cada operación verifica equipo,
 * visibilidad de la noticia (UAD-NOT-05 a 09) y estado. Abrir o consultar nunca crea {@code casoId}
 * (UAD-EVA-03). Cada cambio deja un {@link UadEvento} append-only.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UadNoticiaService {

    static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final Set<EstadoNoticia> EVALUABLES = EnumSet.of(
            EstadoNoticia.RECIBIDA, EstadoNoticia.PENDIENTE_CLASIFICACION, EstadoNoticia.EN_EVALUACION_PRELIMINAR,
            EstadoNoticia.PENDIENTE_SUBSANACION, EstadoNoticia.SUBSANACION_RECIBIDA, EstadoNoticia.EN_REEVALUACION);
    private static final String CAS_ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final UadNoticiaRepository noticiaRepository;
    private final UadCasoRepository casoUadRepository;
    private final UadEventoRepository eventoRepository;
    private final UadSolicitudInformacionRepository solicitudRepository;
    private final CasoRepository casoRepository;
    private final EvidenciaReporteRepository evidenciaRepository;
    private final ReporteEvidenciaStorageService storage;
    private final UadUsuarioActual usuarioActual;
    private final UadCatalogoService catalogos;
    private final UadNoticiaMapper mapper;
    private final SecureRandom random = new SecureRandom();

    // ------------------------------------------------------------------ contexto

    @Transactional(readOnly = true)
    public UadResponses.ContextoUsuario contexto() {
        final UadUsuarioActual.Actor actor = usuarioActual.actual();
        final List<CapacidadUad> capacidades = actor.equipos().isEmpty()
                ? List.of()
                : List.of(CapacidadUad.VER_NDI, CapacidadUad.VER_NDA, CapacidadUad.EVALUAR_NOTICIA, CapacidadUad.VER_CASOS, CapacidadUad.GESTIONAR_CASO);
        return new UadResponses.ContextoUsuario(actor.equipos(), actor.coordinacion(), capacidades);
    }

    // ------------------------------------------------------------------ bandeja

    @Transactional(readOnly = true)
    public Page<UadResponses.NoticiaResumen> listar(
            final TipoNoticia tipo, final EstadoNoticia estado, final LocalDate desde, final LocalDate hasta, final String consecutivo,
            final Pageable pageable) {
        final UadUsuarioActual.Actor actor = usuarioActual.conEquipo();
        if (tipo == null) throw UadException.validacion(Map.of("tipo", "Indique NDI o NDA."));
        return noticiaRepository.findAll(filtro(actor.equipos(), tipo, estado, desde, hasta, consecutivo), pageable).map(mapper::resumen);
    }

    /** La visibilidad por equipo se filtra en la consulta, no en memoria (UAD-NOT-05 a 09). */
    static Specification<UadNoticia> filtro(
            final List<EquipoUad> equipos, final TipoNoticia tipo, final EstadoNoticia estado, final LocalDate desde, final LocalDate hasta,
            final String consecutivo) {
        return (root, query, cb) -> {
            final List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("tipo"), tipo));
            if (tipo == TipoNoticia.NDI) {
                final Expression<VinculacionInvestigado> efectiva =
                        cb.coalesce(root.get("vinculacionClasificada"), root.get("vinculacionDeclarada"));
                p.add(cb.or(cb.isNull(efectiva), efectiva.in(UadCompetencia.vinculacionesVisibles(equipos))));
            }
            if (estado != null) p.add(cb.equal(root.get("estado"), estado));
            if (desde != null) p.add(cb.greaterThanOrEqualTo(root.get("fechaRecepcion"), desde.atStartOfDay(BOGOTA).toInstant()));
            if (hasta != null) p.add(cb.lessThan(root.get("fechaRecepcion"), hasta.plusDays(1).atStartOfDay(BOGOTA).toInstant()));
            if (consecutivo != null && !consecutivo.isBlank()) {
                p.add(cb.like(cb.upper(root.get("consecutivo")), "%" + consecutivo.trim().toUpperCase() + "%"));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    // ------------------------------------------------------------------ detalle (GET seguro)

    @Transactional(readOnly = true)
    public UadResponses.NoticiaDetalle obtener(final UUID noticiaId) {
        final UadUsuarioActual.Actor actor = usuarioActual.conEquipo();
        return detalle(noticiaVisible(noticiaId, actor));
    }

    @Transactional(readOnly = true)
    public List<UadResponses.EventoHistorial> historial(final UUID noticiaId) {
        final UadUsuarioActual.Actor actor = usuarioActual.conEquipo();
        final UadNoticia n = noticiaVisible(noticiaId, actor);
        return eventoRepository.findByNoticiaIdOrderByFechaAscIdAsc(n.getId()).stream().map(mapper::evento).toList();
    }

    // ------------------------------------------------------------------ clasificación (UAD-NOT-07/08)

    @Transactional
    public UadResponses.Clasificacion clasificar(final UUID noticiaId, final ClasificacionUadRequest cuerpo) {
        final UadUsuarioActual.Actor actor = usuarioActual.conEquipo();
        final UadNoticia n = noticiaVisible(noticiaId, actor);
        if (!EVALUABLES.contains(n.getEstado())) {
            throw new UadException(HttpStatus.CONFLICT, "NOTICIA_NO_EVALUABLE", "La noticia ya no está en evaluación.");
        }
        exigirVersion(n, cuerpo.version());
        final VinculacionInvestigado anterior = n.vinculacionEfectiva();
        n.setVinculacionClasificada(cuerpo.vinculacionInvestigado());
        if (n.getEstado() == EstadoNoticia.RECIBIDA || n.getEstado() == EstadoNoticia.PENDIENTE_CLASIFICACION) {
            n.setEstado(EstadoNoticia.EN_EVALUACION_PRELIMINAR);
        }
        noticiaRepository.saveAndFlush(n);
        evento(n, null, TipoEventoUad.CLASIFICACION_VINCULACION,
                "Vinculación: " + (anterior == null ? "sin dato" : anterior) + " → " + cuerpo.vinculacionInvestigado() + ".", actor, null, null, null);
        final boolean visible = UadCompetencia.esVisiblePara(actor.equipos(), n.getTipo(), n.vinculacionEfectiva());
        return new UadResponses.Clasificacion(visible, visible ? detalle(n) : null);
    }

    // ------------------------------------------------------------------ casos asociados (UAD-EVA-06 a 10)

    @Transactional
    public List<UadResponses.CasoAsociado> casosAsociados(final UUID noticiaId) {
        final UadUsuarioActual.Actor actor = usuarioActual.conEquipo();
        final UadNoticia n = noticiaVisible(noticiaId, actor);
        if (!acciones(n).acciones().contains(AccionNoticia.CONSULTAR_CASOS_ASOCIADOS)) {
            throw new UadException(HttpStatus.CONFLICT, "NOTICIA_NO_EVALUABLE", "La consulta solo está disponible en evaluación preliminar.");
        }
        final List<UadResponses.CasoAsociado> casos = buscarCasosAsociados(n);
        // Se audita la consulta; consultar no vincula ni crea caso.
        evento(n, null, TipoEventoUad.CASOS_ASOCIADOS_CONSULTADOS, "Consulta de casos asociados: " + casos.size() + " resultado(s).", actor, null, null, null);
        return casos;
    }

    /**
     * PROVISIONAL (D-UAD-18): coincidencia por el documento de la persona afectada, que solo se conoce
     * en solicitud directa. Busca casos del Equipo de Atención y otras gestiones UAD. Datos mínimos.
     */
    List<UadResponses.CasoAsociado> buscarCasosAsociados(final UadNoticia n) {
        final String documento = documentoPersonaAfectada(n);
        if (documento == null) return List.of();
        final Map<String, UadResponses.CasoAsociado> porId = new LinkedHashMap<>();
        for (Caso c : casoRepository.findByCitaSolicitudAtencionSolicitanteNumeroDocumento(documento)) {
            final boolean cerrado = c.getEstadoCaso() != null && c.getEstadoCaso().getNombre() != null
                    && c.getEstadoCaso().getNombre().toUpperCase().contains("CERR");
            porId.putIfAbsent(c.getCodigo(), new UadResponses.CasoAsociado(
                    c.getCodigo(), "EQUIPO_ATENCION", c.getFechaCreacion() == null ? null : c.getFechaCreacion().toLocalDate(),
                    cerrado ? "CERRADO" : "ACTIVO"));
        }
        for (UadNoticia otra : noticiaRepository.findByReporteCasoNumeroDocumentoAndCasoIdIsNotNull(documento)) {
            if (otra.getId().equals(n.getId())) continue;
            final UadCaso caso = otra.getCasoUad();
            porId.putIfAbsent(otra.getCasoId(), new UadResponses.CasoAsociado(
                    otra.getCasoId(), "UAD", caso == null ? null : caso.getFechaAdmision().atZone(BOGOTA).toLocalDate(),
                    caso != null && caso.getEstadoGestion() == EstadoGestionUad.FINALIZADA ? "CERRADO" : "ACTIVO"));
        }
        return List.copyOf(porId.values());
    }

    private String documentoPersonaAfectada(final UadNoticia n) {
        final ReporteCaso r = n.getReporteCaso();
        if (r == null || r.getTipoReportante() == TipoReportanteEnum.TERCERO) return null;
        final String doc = r.getNumeroDocumento();
        return doc == null || doc.isBlank() ? null : doc.trim();
    }

    // ------------------------------------------------------------------ decisión (UAD-EVA-02 a 12)

    @Transactional
    public UadResponses.DecisionEvaluacion decidir(final UUID noticiaId, final DecisionEvaluacionRequest cuerpo) {
        final UadUsuarioActual.Actor actor = usuarioActual.conEquipo();
        final UadNoticia n = noticiaVisible(noticiaId, actor);
        if (!EVALUABLES.contains(n.getEstado())) {
            throw new UadException(HttpStatus.CONFLICT, "NOTICIA_YA_DECIDIDA", "Otra persona ya registró una decisión sobre esta noticia.");
        }
        exigirVersion(n, cuerpo.version());

        final List<String> casosAsociados = cuerpo.continuarTramite() != null
                && cuerpo.continuarTramite().modoCaso() == ModoCaso.VINCULAR_EXISTENTE
                ? buscarCasosAsociados(n).stream().map(UadResponses.CasoAsociado::casoId).toList()
                : List.of();
        final Map<String, String> errores = UadDecisionValidator.validar(cuerpo, new UadDecisionValidator.Contexto(
                n.getTipo(), n.getCasoId(), casosAsociados, n.getReporteCaso() != null,
                UadCompetencia.esDeterminada(n.vinculacionEfectiva()), LocalDate.now(BOGOTA),
                c -> catalogos.existe(CatalogoUad.TIPO_SOLICITUD_INFORMACION, c),
                c -> catalogos.existe(CatalogoUad.CAUSAL_INHIBITORIO, c),
                c -> catalogos.existe(CatalogoUad.ENTIDAD_REMISION, c)));
        if (!errores.isEmpty()) {
            final boolean incompatible = "Registre una sola decisión por vez.".equals(errores.get("tipo"));
            throw new UadException(HttpStatus.BAD_REQUEST, incompatible ? "DECISION_INCOMPATIBLE" : "VALIDACION", "Revise los datos marcados.", errores);
        }
        if (!acciones(n).acciones().contains(AccionNoticia.valueOf(cuerpo.tipo().name()))) {
            throw new UadException(HttpStatus.FORBIDDEN, "SIN_PERMISO", "Esta decisión no está disponible para su equipo.");
        }

        final EstadoNoticia anterior = n.getEstado();
        final List<UadResponses.NotificacionEstado> notificaciones = new ArrayList<>();
        UadCaso caso = null;
        switch (cuerpo.tipo()) {
            case CONTINUAR_TRAMITE -> caso = continuar(n, cuerpo.continuarTramite(), actor, notificaciones);
            case SOLICITAR_INFORMACION -> solicitar(n, cuerpo.solicitudInformacion(), actor, notificaciones);
            case INHIBITORIO -> n.setEstado(EstadoNoticia.INHIBITORIA);
            // Solo se registra: Casilda no envía nada a la entidad (UAD-REM-04/05).
            case REMISION_COMPETENCIA -> n.setEstado(EstadoNoticia.REMITIDA_POR_COMPETENCIA);
        }
        noticiaRepository.saveAndFlush(n);
        evento(n, null, TipoEventoUad.DECISION_REGISTRADA, descripcionDecision(cuerpo), actor, anterior.name(), n.getEstado().name(),
                vacioANulo(cuerpo.observaciones()));
        return new UadResponses.DecisionEvaluacion(
                detalle(n), caso == null ? null : mapper.caso(caso, List.of(n.getConsecutivo())), List.copyOf(notificaciones));
    }

    private UadCaso continuar(
            final UadNoticia n, final DecisionEvaluacionRequest.ContinuarTramite t, final UadUsuarioActual.Actor actor,
            final List<UadResponses.NotificacionEstado> notificaciones) {
        switch (t.modoCaso()) {
            case CREAR_NUEVO -> n.setCasoId(nuevoCasoId());
            case VINCULAR_EXISTENTE -> n.setCasoId(t.casoIdVincular().trim());
            case CONSERVAR_ASIGNADO -> {
                // UAD-ID-06: se conserva el ID asignado por otro actor.
            }
        }
        n.setExpedienteUad(vacioANulo(t.expedienteUad()));
        n.setEstado(EstadoNoticia.PROCEDE_TRAMITE);

        final VinculacionInvestigado v = n.vinculacionEfectiva();
        final UadCaso caso = new UadCaso();
        caso.setCasoId(n.getCasoId());
        caso.setExpedienteUad(n.getExpedienteUad());
        caso.setVinculacionInvestigado(v);
        caso.setVictimaNombre(nombreVictima(n));
        caso.setFechaAdmision(Instant.now());
        caso.setFaseActual(FaseUad.INSTRUCCION);
        caso.setEquipoResponsable(UadCompetencia.equipoInstructor(v));
        caso.setEstadoGestion(EstadoGestionUad.ACTIVA);
        casoUadRepository.saveAndFlush(caso);
        n.setCasoUad(caso);

        final TipoEventoUad tipoEvento = t.modoCaso() == ModoCaso.CREAR_NUEVO ? TipoEventoUad.CASO_CREADO
                : t.modoCaso() == ModoCaso.VINCULAR_EXISTENTE ? TipoEventoUad.CASO_VINCULADO : TipoEventoUad.DECISION_REGISTRADA;
        final String texto = switch (t.modoCaso()) {
            case CREAR_NUEVO -> "Se generó el ID de caso Casilda " + n.getCasoId() + ".";
            case VINCULAR_EXISTENTE -> "Vinculada al caso existente " + n.getCasoId() + " por tratarse de los mismos hechos. Se conserva el consecutivo "
                    + n.getConsecutivo() + ".";
            case CONSERVAR_ASIGNADO -> "Se conserva el ID de caso Casilda " + n.getCasoId() + " asignado por otro actor.";
        };
        evento(n, null, tipoEvento, texto, actor, null, null, null);
        evento(null, caso, TipoEventoUad.CASO_CREADO, "Caso admitido para trámite desde " + n.getConsecutivo() + ".", actor, null, null, null);

        // No hay servicio de correo en el backend: la notificación queda registrada como pendiente (D-UAD-10).
        notificaciones.add(caso.getVictimaNombre() == null && n.getTipo() == TipoNoticia.NDA
                ? new UadResponses.NotificacionEstado("ADMISION", "VICTIMA", "NO_APLICA", "Sin contacto autorizado de la víctima.")
                : new UadResponses.NotificacionEstado("ADMISION", "VICTIMA", "PENDIENTE", "Pendiente de envío: el servicio de correo aún no existe."));
        return caso;
    }

    private void solicitar(
            final UadNoticia n, final DecisionEvaluacionRequest.SolicitudInformacion s, final UadUsuarioActual.Actor actor,
            final List<UadResponses.NotificacionEstado> notificaciones) {
        final UadSolicitudInformacion solicitud = new UadSolicitudInformacion();
        solicitud.setNoticia(n);
        solicitud.setTipoSolicitudCodigo(s.tipoSolicitudCodigo());
        solicitud.setInformacionRequerida(s.informacionRequerida().trim());
        solicitud.setFechaSolicitud(Instant.now());
        solicitud.setFechaLimite(s.fechaLimite());
        solicitudRepository.save(solicitud);
        n.setEstado(EstadoNoticia.PENDIENTE_SUBSANACION);
        evento(n, null, TipoEventoUad.SOLICITUD_INFORMACION,
                "Se solicitó información adicional al reportante. Fecha límite: " + s.fechaLimite() + ".", actor, null, null, null);
        notificaciones.add(new UadResponses.NotificacionEstado("SOLICITUD_INFORMACION", "REPORTANTE", "PENDIENTE",
                "Pendiente de envío: el servicio de correo aún no existe."));
    }

    // ------------------------------------------------------------------ evidencias (UAD-PRU-03, UAD-SEG-08)

    public record Descarga(Resource recurso, String nombre, String tipo) {
    }

    @Transactional(readOnly = true)
    public Descarga descargarEvidencia(final Long evidenciaId) {
        final UadUsuarioActual.Actor actor = usuarioActual.conEquipo();
        final EvidenciaReporte e = evidenciaRepository.findById(evidenciaId).orElseThrow(UadException::noEncontrado);
        final UadNoticia n = noticiaRepository.findByReporteCasoId(e.getReporteCaso().getId()).orElseThrow(UadException::noEncontrado);
        // Visible si la noticia lo es o si su caso está en la bandeja del usuario: tras el traslado, el equipo
        // decisor ya no ve la noticia (solo el instructor), pero las pruebas siguen con el caso (UAD-PRU-04).
        final boolean porCaso = n.getCasoUad() != null && UadCasoService.esVisible(n.getCasoUad(), actor);
        if (!porCaso && !UadCompetencia.esVisiblePara(actor.equipos(), n.getTipo(), n.vinculacionEfectiva())) throw UadException.noEncontrado();
        // Trazabilidad de la descarga (UAD-SEG-08) en el log del servidor, no en el historial visible.
        log.info("UAD: descarga de evidencia {} de la noticia {} por {}", evidenciaId, n.getConsecutivo(), actor.email());
        return new Descarga(storage.cargar(e.getNombreAlmacenado()), e.getNombreOriginal(), e.getTipoContenido());
    }

    // ------------------------------------------------------------------ apoyo

    record Acciones(List<AccionNoticia> acciones, Map<AccionNoticia, String> motivos) {
    }

    /** Acciones habilitadas por estado, tipo y vinculación. La UI solo las muestra; aquí se exigen. */
    Acciones acciones(final UadNoticia n) {
        final List<AccionNoticia> acciones = new ArrayList<>();
        final Map<AccionNoticia, String> motivos = new EnumMap<>(AccionNoticia.class);
        if (n.getReporteCaso() != null && !n.getReporteCaso().getEvidencias().isEmpty()) acciones.add(AccionNoticia.DESCARGAR_EVIDENCIAS);
        if (!EVALUABLES.contains(n.getEstado())) {
            final String motivo = "La noticia ya tiene una decisión registrada.";
            for (AccionNoticia a : List.of(AccionNoticia.CLASIFICAR_VINCULACION, AccionNoticia.CONSULTAR_CASOS_ASOCIADOS, AccionNoticia.CONTINUAR_TRAMITE,
                    AccionNoticia.SOLICITAR_INFORMACION, AccionNoticia.INHIBITORIO, AccionNoticia.REMISION_COMPETENCIA)) {
                motivos.put(a, motivo);
            }
            return new Acciones(acciones, motivos);
        }
        acciones.add(AccionNoticia.CLASIFICAR_VINCULACION);
        acciones.add(AccionNoticia.INHIBITORIO);
        acciones.add(AccionNoticia.REMISION_COMPETENCIA);
        if (documentoPersonaAfectada(n) != null) acciones.add(AccionNoticia.CONSULTAR_CASOS_ASOCIADOS);
        else motivos.put(AccionNoticia.CONSULTAR_CASOS_ASOCIADOS, "No hay una persona afectada identificada para consultar.");
        if (UadCompetencia.esDeterminada(n.vinculacionEfectiva())) acciones.add(AccionNoticia.CONTINUAR_TRAMITE);
        else motivos.put(AccionNoticia.CONTINUAR_TRAMITE, "Primero clasifique la vinculación de la persona presuntamente responsable.");
        if (n.getTipo() == TipoNoticia.NDA || n.getReporteCaso() == null) {
            motivos.put(AccionNoticia.SOLICITAR_INFORMACION, "Solo se puede pedir información a un reportante identificado.");
        } else if (n.getEstado() == EstadoNoticia.PENDIENTE_SUBSANACION) {
            motivos.put(AccionNoticia.SOLICITAR_INFORMACION, "Ya hay una solicitud de información en curso.");
        } else {
            acciones.add(AccionNoticia.SOLICITAR_INFORMACION);
        }
        return new Acciones(acciones, motivos);
    }

    private UadResponses.NoticiaDetalle detalle(final UadNoticia n) {
        final Acciones a = acciones(n);
        final UadSolicitudInformacion solicitud = solicitudRepository.findFirstByNoticiaIdOrderByFechaSolicitudDesc(n.getId()).orElse(null);
        return mapper.detalle(n, solicitud, LocalDate.now(BOGOTA), a.acciones(), a.motivos());
    }

    /** 404 si no existe o no es visible para el equipo: no se revela cuál de los dos. */
    private UadNoticia noticiaVisible(final UUID id, final UadUsuarioActual.Actor actor) {
        final UadNoticia n = noticiaRepository.findById(id).orElseThrow(UadException::noEncontrado);
        if (!UadCompetencia.esVisiblePara(actor.equipos(), n.getTipo(), n.vinculacionEfectiva())) throw UadException.noEncontrado();
        return n;
    }

    private void exigirVersion(final UadNoticia n, final Long version) {
        if (version == null || !version.equals(n.getVersion())) throw UadException.conflictoVersion();
    }

    void evento(
            final UadNoticia noticia, final UadCaso caso, final TipoEventoUad tipo, final String descripcion, final UadUsuarioActual.Actor actor,
            final String estadoAnterior, final String estadoNuevo, final String observaciones) {
        final UadEvento e = new UadEvento();
        e.setNoticia(noticia);
        e.setCasoUad(caso);
        e.setTipo(tipo);
        e.setFecha(Instant.now());
        e.setDescripcion(descripcion);
        e.setProfesional(actor == null ? null : actor.nombre());
        e.setEquipo(actor == null ? null : actor.equipoPrincipal());
        e.setFase(caso == null ? null : caso.getFaseActual());
        e.setEstadoAnterior(estadoAnterior);
        e.setEstadoNuevo(estadoNuevo);
        e.setObservaciones(observaciones);
        eventoRepository.save(e);
    }

    /**
     * PROVISIONAL (D-UAD-15): ID de caso CASILDA con el formato de {@code ReporteCaso.codigo}
     * ({@code CAS-} + 10 caracteres), único entre las gestiones UAD. Falta definir un ID transversal único.
     */
    private String nuevoCasoId() {
        for (int intento = 0; intento < 5; intento++) {
            final StringBuilder sb = new StringBuilder("CAS-");
            for (int i = 0; i < 10; i++) sb.append(CAS_ALFABETO.charAt(random.nextInt(CAS_ALFABETO.length())));
            final String candidato = sb.toString();
            if (!casoUadRepository.existsByCasoId(candidato)) return candidato;
        }
        throw new IllegalStateException("No fue posible generar un ID de caso único");
    }

    private String nombreVictima(final UadNoticia n) {
        final ReporteCaso r = n.getReporteCaso();
        if (r == null) return null;
        if (r.getTipoReportante() == TipoReportanteEnum.TERCERO) {
            return UadNoticiaMapper.unir(r.getVictimaPrimerNombre(), r.getVictimaSegundoNombre(), r.getVictimaPrimerApellido(), r.getVictimaSegundoApellido());
        }
        return r.getUsuario() == null ? null : r.getUsuario().getNombre();
    }

    private String descripcionDecision(final DecisionEvaluacionRequest c) {
        return switch (c.tipo()) {
            case CONTINUAR_TRAMITE -> "Continuar trámite.";
            case SOLICITAR_INFORMACION -> "Solicitar información adicional.";
            case INHIBITORIO -> "Decisión inhibitoria. Causal: " + catalogos.nombreDe(CatalogoUad.CAUSAL_INHIBITORIO, c.inhibitorio().causalCodigo())
                    + ". " + c.inhibitorio().justificacion().trim();
            case REMISION_COMPETENCIA -> "Remisión por competencia a "
                    + catalogos.nombreDe(CatalogoUad.ENTIDAD_REMISION, c.remisionCompetencia().entidadDestinatariaCodigo())
                    + " (" + c.remisionCompetencia().fechaRemision() + ")."
                    + (c.remisionCompetencia().justificacion() == null || c.remisionCompetencia().justificacion().isBlank()
                    ? "" : " " + c.remisionCompetencia().justificacion().trim());
        };
    }

    private static String vacioANulo(final String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
