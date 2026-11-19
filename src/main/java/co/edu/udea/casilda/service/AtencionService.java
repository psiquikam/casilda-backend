package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.request.*;
import co.edu.udea.casilda.dto.response.AtencionResponse;
import co.edu.udea.casilda.exception.ResourceNotFoundException;
import co.edu.udea.casilda.model.entity.*;
import co.edu.udea.casilda.model.enums.EstadoCitaEnum;
import co.edu.udea.casilda.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;

/**
 * Servicio para gestión de atenciones
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AtencionService {

    private final AtencionRepository atencionRepository;
    private final SeguimientoAtencionRepository seguimientoAtencionRepository;
    private final CompromisoService compromisoService;
    private final AuditoriaCasoService auditoriaCasoService;
    private final CitaRepository citaRepository;

    // Repositories de entidades relacionadas
    private final PersonaRepository personaRepository;
    private final CasoRepository casoRepository;
    private final UsuarioRepository usuarioRepository;

    // Repositories de maestros
    private final TipoServicioRepository tipoServicioRepository;
    private final TipoSeguimientoRepository tipoSeguimientoRepository;
    private final AccionRepository accionRepository;
    private final ActividadRepository actividadRepository;
    private final EstadoSeguimientoRepository estadoSeguimientoRepository;
    private final MotivoEstadoSeguimientoRepository motivoEstadoSeguimientoRepository;
    private final SexoRepository sexoRepository;
    private final ArchivoConsentimientoRepository archivoConsentimientoRepository;
    private final EstadoAtencionRepository estadoAtencionRepository;
    private final LugarEntrevistaRepository lugarEntrevistaRepository;
    private final RutaAtencionRepository rutaAtencionRepository;
    private final RemisionAtencionRepository remisionAtencionRepository;
    private final MedidaProteccionRepository medidaProteccionRepository;
    private final TipoMedidaRepository tipoMedidaRepository;
    private final SubTipoMedidaRepository subTipoMedidaRepository;
    private final ResponsableMedidaProteccionRepository responsableMedidaProteccionRepository;
    private final ArchivoSeguimientoAtencionRepository archivoSeguimientoAtencionRepository;
    private final ApreciacionAtencionRepository apreciacionAtencionRepository;
    private final TipoApreciacionRepository tipoApreciacionRepository;
    private final CompromisoPersonaAtendidaRepository compromisoPersonaAtendidaRepository;
    private final CompromisoProfesionalRepository compromisoProfesionalRepository;
    private final EstadoCasoRepository estadoCasoRepository;

    /**
     * Pestaña 5 - Registro de atención: crea/actualiza la atención con
     * tipoServicio, lugarEntrevista, consentimiento.
     */
    @Transactional
    public AtencionResponse registrarPestanaAtencion(PestanaRegistroAtencionRequest request) {
        log.info("Pestaña 5: Registrando atención para caso ID: {}", request.getCasoId());

        Usuario usuario = obtenerUsuarioAutenticadoRequerido();

        Caso caso = casoRepository.findById(request.getCasoId())
                .orElseThrow(() -> new ResourceNotFoundException("Caso no encontrado con ID: " + request.getCasoId()));

        Cita cita = request.getCitaId() == null
                ? caso.getCita()
                : citaRepository.findById(request.getCitaId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Cita no encontrada con ID: " + request.getCitaId()));
        if (caso.getCita() == null || cita == null) {
            throw new IllegalArgumentException("El caso debe estar asociado a una solicitud con cita.");
        }
        if (!cita.getSolicitudAtencion().getId().equals(caso.getCita().getSolicitudAtencion().getId())) {
            throw new IllegalArgumentException("La cita no pertenece a la solicitud asociada al caso.");
        }
        if (!EstadoCitaEnum.ATENDIDA.getId().equals(cita.getEstadoCita().getId())) {
            throw new IllegalArgumentException("Solo se puede registrar una atención después de atender la cita.");
        }

        AtencionRegistroRequest atencionRequest = new AtencionRegistroRequest();
        atencionRequest.setCasoId(request.getCasoId());
        atencionRequest.setIdAtencion(request.getIdAtencion());
        atencionRequest.setIdTipoServicio(request.getIdTipoServicio());
        atencionRequest.setIdLugarEntrevista(request.getIdLugarEntrevista());
        atencionRequest.setArchivoConsentimientoNombre(request.getArchivoConsentimientoNombre());
        atencionRequest.setArchivoConsentimientoTipo(request.getArchivoConsentimientoTipo());
        atencionRequest.setArchivoConsentimientoContenido(request.getArchivoConsentimientoContenido());

        Atencion atencion;
        if (request.getIdAtencion() != null) {
            atencion = atencionRepository.findById(request.getIdAtencion())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Atención no encontrada con ID: " + request.getIdAtencion()));
            if (!atencion.getCaso().getId().equals(caso.getId())) {
                throw new IllegalArgumentException("La atención no pertenece al caso indicado.");
            }
            if (atencion.getCita() != null && !atencion.getCita().getId().equals(cita.getId())) {
                throw new IllegalArgumentException("La atención ya está asociada a otra cita.");
            }
        } else {
            atencion = atencionRepository.findByCasoIdAndCitaId(caso.getId(), cita.getId()).orElseGet(() -> {
                Atencion nuevaAtencion = new Atencion();
                nuevaAtencion.setCaso(caso);
                nuevaAtencion.setCita(cita);
                nuevaAtencion.setUsuarioCreacion(usuario);
                nuevaAtencion.setFechaCreacion(LocalDateTime.now());
                nuevaAtencion.setEstadoAtencion(estadoAtencionRepository.findById(1)
                        .orElseThrow(() -> new ResourceNotFoundException("Estado de atención no encontrado con ID: 1")));
                return nuevaAtencion;
            });
        }

        atencion.setCita(cita);
        atencion.setUsuarioActualizacion(usuario);
        atencion.setFechaActualizacion(LocalDateTime.now());

        if (request.getIdTipoServicio() != null) {
            atencion.setTipoServicio(tipoServicioRepository.findById(request.getIdTipoServicio()).orElse(null));
        }
        if (request.getIdLugarEntrevista() != null) {
            atencion.setLugarEntrevista(
                    lugarEntrevistaRepository.findById(request.getIdLugarEntrevista()).orElse(null));
        }
        atencionRepository.save(atencion);

        if (request.getArchivoConsentimientoContenido() != null &&
                !request.getArchivoConsentimientoContenido().isBlank()) {
            RegistroAtencionCompleteRequest wrapper = new RegistroAtencionCompleteRequest();
            wrapper.setAtencion(atencionRequest);
            crearArchivoConsentimiento(atencion, wrapper);
        }

        log.info("Pestaña 5: Atención guardada con ID: {}", atencion.getId());

        return mapToResponse(atencion);

    }

    @Transactional
    public void registrarPestanaApreciaciones(PestanaApreciacionesRequest request) {
        log.info("Pestaña 6: Guardando apreciaciones para atención ID: {}", request.getIdAtencion());

        Usuario usuario = obtenerUsuarioAutenticadoRequerido();

        Atencion atencion = obtenerAtencionRequerida(request.getIdAtencion());
        atencion.setUsuarioActualizacion(usuario);
        atencion.setFechaActualizacion(LocalDateTime.now());
        atencionRepository.save(atencion);

        // Delete existing ones
        List<ApreciacionAtencion> existentes = apreciacionAtencionRepository.findByIdIdAtencion(atencion.getId());
        apreciacionAtencionRepository.deleteAll(existentes);

        // Save new ones
        if (request.getApreciaciones() != null) {
            for (PestanaApreciacionesRequest.ApreciacionRequest r : request.getApreciaciones()) {
                if (r.getIdTipoApreciacion() != null) {
                    TipoApreciacion tipo = tipoApreciacionRepository.findById(r.getIdTipoApreciacion())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Tipo de apreciación no encontrado con ID: " + r.getIdTipoApreciacion()));

                    ApreciacionAtencionId id = new ApreciacionAtencionId(atencion.getId(), r.getIdTipoApreciacion());
                    ApreciacionAtencion aa = ApreciacionAtencion.builder()
                            .id(id)
                            .atencion(atencion)
                            .tipoApreciacion(tipo)
                            .descripcion(r.getDescripcion() != null ? r.getDescripcion() : "")
                            .build();
                    apreciacionAtencionRepository.save(aa);
                }
            }
        }
    }

    /**
     * Pestaña 7 - Acuerdos y compromisos: actualiza logroAcuerdo,
     * Activación de ruta, y Remisiones.
     */
    @Transactional
    public void registrarPestanaAcuerdos(PestanaAcuerdosRequest request) {
        log.info("Pestaña 7: Guardando acuerdos, rutas y remisiones para atención ID: {}", request.getIdAtencion());
        Usuario usuario = obtenerUsuarioAutenticadoRequerido();

        Atencion atencion = obtenerAtencionRequerida(request.getIdAtencion());
        atencion.setUsuarioActualizacion(usuario);
        atencion.setFechaActualizacion(LocalDateTime.now());

        if (request.getLogroAcuerdo() != null) {
            atencion.setLogroAcuerdo(request.getLogroAcuerdo());
        }
        atencionRepository.save(atencion);

        // Guardar Activación de rutas
        rutaAtencionRepository.deleteByIdAtencion(atencion.getId());
        if (request.getRutas() != null) {
            for (RutaAtencionRequest r : request.getRutas()) {
                if (r.getIdTipoRutaActivacion() != null && r.getIdRutaActivacion() != null) {
                    RutaAtencion ra = new RutaAtencion(atencion.getId(), r.getIdTipoRutaActivacion(),
                            r.getIdRutaActivacion());
                    rutaAtencionRepository.save(ra);
                }
            }
        }

        // Guardar Remisiones
        remisionAtencionRepository.deleteByIdAtencion(atencion.getId());
        if (request.getRemisiones() != null) {
            for (RemisionAtencionRequest r : request.getRemisiones()) {
                if (r.getIdTipoRemision() != null) {
                    RemisionAtencion ra = new RemisionAtencion(
                            atencion.getId(),
                            r.getIdTipoRemision(),
                            r.getCual(),
                            r.getFecha() != null ? r.getFecha() : LocalDateTime.now());
                    remisionAtencionRepository.save(ra);
                }
            }
        }
    }

    /**
     * Pestaña 8 - Medidas de protección.
     */
    @Transactional
    public void registrarPestanaMedidasProteccion(PestanaMedidasProteccionRequest request) {
        log.info("Pestaña 8: Guardando medidas de protección para atención ID: {}", request.getIdAtencion());
        Usuario usuario = obtenerUsuarioAutenticadoRequerido();

        Atencion atencion = obtenerAtencionRequerida(request.getIdAtencion());
        atencion.setUsuarioActualizacion(usuario);
        atencion.setFechaActualizacion(LocalDateTime.now());
        atencionRepository.save(atencion);

        // Limpiar medidas existentes para esta atención
        medidaProteccionRepository.deleteByAtencionId(atencion.getId());

        // Registrar las nuevas medidas
        if (request.getMedidas() != null) {
            for (MedidaProteccionRequest m : request.getMedidas()) {
                if (m.getTipoMedidaId() != null && m.getSubtipoMedidaId() != null && m.getResponsableId() != null) {
                    MedidaProteccion mp = new MedidaProteccion();
                    mp.setAtencion(atencion);

                    TipoMedida tm = tipoMedidaRepository.findById(m.getTipoMedidaId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Tipo de medida no encontrado: " + m.getTipoMedidaId()));
                    mp.setTipoMedida(tm);

                    SubTipoMedida stm = subTipoMedidaRepository.findById(m.getSubtipoMedidaId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Subtipo de medida no encontrado: " + m.getSubtipoMedidaId()));
                    mp.setSubtipoMedida(stm);

                    ResponsableMedidaProteccion resp = responsableMedidaProteccionRepository
                            .findById(m.getResponsableId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Responsable de medida no encontrado: " + m.getResponsableId()));
                    mp.setResponsable(resp);

                    mp.setFechaRegistro(m.getFechaRegistro() != null ? m.getFechaRegistro() : LocalDateTime.now());
                    mp.setDescripcion(m.getDescripcion() != null ? m.getDescripcion() : "");
                    medidaProteccionRepository.save(mp);
                }
            }
        }
    }

    /**
     * Pestaña 9 - Otros compromisos: crear compromisos de persona y profesional.
     */
    @Transactional
    public AtencionResponse registrarPestanaOtrosCompromisos(PestanaOtrosCompromisosRequest request) {
        log.info("Pestaña 9: Creando compromisos para atención ID: {}", request.getIdAtencion());

        Atencion atencion = obtenerAtencionRequerida(request.getIdAtencion());

        // Limpiar compromisos de persona
        List<CompromisoPersonaAtendida> existentesPersona = compromisoPersonaAtendidaRepository
                .findByIdatencion(atencion.getId());
        compromisoPersonaAtendidaRepository.deleteAll(existentesPersona);

        // Crear compromisos de persona atendida
        if (request.getPersona() != null && !request.getPersona().isEmpty()) {
            for (CompromisoPersonaAtendidaRequest comproPersona : request.getPersona()) {
                comproPersona.setIdatencion(atencion.getId());
                compromisoService.crearCompromisoPersona(comproPersona);
                log.debug("Compromiso de persona creado para atención ID: {}", atencion.getId());
            }
        }

        // Limpiar compromisos profesionales
        List<CompromisoProfesional> existentesProfesional = compromisoProfesionalRepository
                .findByIdatencion(atencion.getId());
        compromisoProfesionalRepository.deleteAll(existentesProfesional);

        // Crear compromisos profesionales
        if (request.getProfesional() != null && !request.getProfesional().isEmpty()) {
            for (CompromisoProfesionalRequest comproProfesional : request.getProfesional()) {
                comproProfesional.setIdatencion(atencion.getId());
                compromisoService.crearCompromisoProfesional(comproProfesional);
                log.debug("Compromiso profesional creado para atención ID: {}", atencion.getId());
            }
        }

        log.info("Pestaña 9: Compromisos creados para atención ID: {}", atencion.getId());
        return mapToResponse(atencion);
    }

    /**
     * Pestaña 10 - Seguimientos
     */
    @Transactional
    public void registrarPestanaSeguimientos(PestanaSeguimientosRequest request) {
        log.info("Pestaña 10: Creando seguimientos para atención ID: {}", request.getIdAtencion());

        Atencion atencion = obtenerAtencionRequerida(request.getIdAtencion());

        // Limpiar seguimientos existentes
        List<SeguimientoAtencion> existentes = seguimientoAtencionRepository
                .findByAtencionIdOrderByFechaDesc(atencion.getId());
        seguimientoAtencionRepository.deleteAll(existentes);

        if (request.getSeguimientos() != null && !request.getSeguimientos().isEmpty()) {
            for (SeguimientoAtencionRequest segRequest : request.getSeguimientos()) {
                crearSeguimiento(atencion, segRequest);
            }
        }

        log.info("Pestaña 10: Seguimientos creados para atención ID: {}", atencion.getId());
    }

    @Transactional
    public void registrarPestanaEstadoCaso(PestanaEstadoCasoRequest request) {
        log.info("Pestaña 11: Actualizando estado de caso para caso ID: {}", request.getIdCaso());

        Usuario usuario = obtenerUsuarioAutenticadoRequerido();

        Caso caso = casoRepository.findByIdForUpdate(request.getIdCaso())
                .orElseThrow(() -> new ResourceNotFoundException("Caso no encontrado con ID: " + request.getIdCaso()));

        if (request.getIdEstadoCaso() != null) {
            EstadoCaso nuevoEstado = estadoCasoRepository.findById(request.getIdEstadoCaso())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Estado de caso no encontrado con ID: " + request.getIdEstadoCaso()));
            boolean cambioEstado = !java.util.Objects.equals(
                    caso.getEstadoCaso() == null ? null : caso.getEstadoCaso().getId(),
                    nuevoEstado.getId());
            boolean cerradoAntes = caso.getEstadoCaso() != null
                    && "Cerrado".equalsIgnoreCase(caso.getEstadoCaso().getNombre());
            boolean cerradoAhora = "Cerrado".equalsIgnoreCase(nuevoEstado.getNombre());
            if (cerradoAhora && !cerradoAntes) {
                caso.setFechaCierre(Instant.now());
            } else if (!cerradoAhora) {
                caso.setFechaCierre(null);
            }
            caso.setEstadoCaso(nuevoEstado);
            caso.setUsuarioActualizacion(usuario);
            caso.setFechaActualizacion(LocalDateTime.now());
            casoRepository.save(caso);
            if (cambioEstado) {
                auditoriaCasoService.registrar(caso, usuario, "CAMBIO_ESTADO", "Se cambió el estado del caso.");
            }
        }

        Atencion atencion = atencionRepository.findByCasoId(caso.getId()).orElse(null);
        if (atencion != null) {
            atencion.setUsuarioActualizacion(usuario);
            atencion.setFechaActualizacion(LocalDateTime.now());
            atencionRepository.save(atencion);
        }

        log.info("Pestaña 11: Estado de caso actualizado para caso ID: {}", caso.getId());
    }

    /**
     * Actualiza los datos de una Persona
     */
    private void actualizarPersona(Persona persona, PersonaAtencionRequest personaRequest) {
        log.info("Actualizando persona ID: {}", persona.getId());

        // Resolver maestros por ID
        if (personaRequest.getIdSexo() != null) {
            Sexo sexo = sexoRepository.findById(personaRequest.getIdSexo()).orElse(null);
            persona.setSexo(sexo);
        }

        // Procesar correos
        if (personaRequest.getCorreos() != null && !personaRequest.getCorreos().isEmpty()) {
            log.info("Actualizando correos de persona ID: {}", persona.getId());
            persona.getCorreos().clear(); // Elimina automáticamente por orphanRemoval
            for (CorreoSolicitanteRequest correoReq : personaRequest.getCorreos()) {
                CorreoPersona correo = new CorreoPersona();
                correo.setIdpersona(persona.getId());
                correo.setIdtipo(correoReq.getTipoId());
                correo.setPersona(persona);
                correo.setCorreo(correoReq.getCorreo());
                persona.getCorreos().add(correo);
            }
        }

        // Procesar telefonos
        if (personaRequest.getTelefonos() != null && !personaRequest.getTelefonos().isEmpty()) {
            log.info("Actualizando telefonos de persona ID: {}", persona.getId());
            persona.getTelefonos().clear(); // Elimina automáticamente por orphanRemoval
            for (TelefonoSolicitanteRequest telefonoReq : personaRequest.getTelefonos()) {
                TelefonoPersona telefono = new TelefonoPersona();
                telefono.setIdpersona(persona.getId());
                telefono.setIdtipo(telefonoReq.getTipoId());
                telefono.setPersona(persona);
                telefono.setTelefono(telefonoReq.getTelefono());
                persona.getTelefonos().add(telefono);
            }
        }

        personaRepository.save(persona);
    }

    /**
     * Crea un seguimiento de atención
     */
    private SeguimientoAtencion crearSeguimiento(Atencion atencion, SeguimientoAtencionRequest request) {
        log.info("Creando seguimiento para atención ID: {}", atencion.getId());

        // Resolver maestros
        TipoSeguimiento tipoSeguimiento = tipoSeguimientoRepository.findById(request.getIdTipoSeguimiento())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TipoSeguimiento no encontrado con ID: " + request.getIdTipoSeguimiento()));

        Accion accion = accionRepository.findById(request.getIdAccion())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Acción no encontrada con ID: " + request.getIdAccion()));

        Actividad actividad = actividadRepository.findById(request.getIdActividad())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Actividad no encontrada con ID: " + request.getIdActividad()));

        EstadoSeguimiento estadoSeguimiento = estadoSeguimientoRepository.findById(request.getIdEstadoSeguimiento())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "EstadoSeguimiento no encontrado con ID: " + request.getIdEstadoSeguimiento()));

        MotivoEstadoSeguimiento motivoEstadoSeguimiento = motivoEstadoSeguimientoRepository
                .findById(request.getIdMotivoEstadoSeguimiento())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MotivoEstadoSeguimiento no encontrado con ID: " + request.getIdMotivoEstadoSeguimiento()));

        // Crear el seguimiento
        SeguimientoAtencion seguimiento = new SeguimientoAtencion();
        seguimiento.setAtencion(atencion);
        seguimiento.setTipoSeguimiento(tipoSeguimiento);
        seguimiento.setFecha(request.getFecha());
        seguimiento.setAccion(accion);
        seguimiento.setActividad(actividad);
        seguimiento.setDescripcion(request.getDescripcion());
        seguimiento.setEstadoSeguimiento(estadoSeguimiento);
        seguimiento.setMotivoEstadoSeguimiento(motivoEstadoSeguimiento);

        seguimiento = seguimientoAtencionRepository.save(seguimiento);

        // Persistir archivo de seguimiento si se proporciona
        if (request.getArchivoContenido() != null && !request.getArchivoContenido().isBlank()) {
            crearArchivoSeguimiento(seguimiento, request);
        }

        return seguimiento;
    }

    /**
     * Mapea una Atencion a AtencionResponse
     */
    private AtencionResponse mapToResponse(Atencion atencion) {
        return AtencionResponse.builder()
                .id(atencion.getId())
                .build();
    }

    /**
     * Crea un archivo de consentimiento desde el contenido base64
     */
    private void crearArchivoConsentimiento(Atencion atencion, RegistroAtencionCompleteRequest request) {
        try {
            log.info("Creando archivo de consentimiento para atención ID: {}", atencion.getId());
            AtencionRegistroRequest atencionRequest = request.getAtencion();

            ArchivoConsentimiento archivo = new ArchivoConsentimiento();
            archivo.setAtencion(atencion);
            archivo.setNombre(atencionRequest.getArchivoConsentimientoNombre() != null
                    ? atencionRequest.getArchivoConsentimientoNombre()
                    : "consentimiento.pdf");
            archivo.setTipoContenido(atencionRequest.getArchivoConsentimientoTipo() != null
                    ? atencionRequest.getArchivoConsentimientoTipo()
                    : "application/pdf");
            archivo.setContenido(
                    java.util.Base64.getDecoder().decode(atencionRequest.getArchivoConsentimientoContenido()));

            archivoConsentimientoRepository.save(archivo);
        } catch (IllegalArgumentException e) {
            log.warn("Base64 inválido para archivo de consentimiento: {}", e.getMessage());
        }
    }

    /**
     * Crea un archivo de seguimiento desde el contenido base64
     */
    private void crearArchivoSeguimiento(SeguimientoAtencion seguimiento, SeguimientoAtencionRequest request) {
        try {
            log.info("Creando archivo de seguimiento para seguimiento ID: {}", seguimiento.getId());

            ArchivoSeguimientoAtencion archivo = new ArchivoSeguimientoAtencion();
            archivo.setSeguimientoAtencion(seguimiento);
            archivo.setNombre(request.getArchivoNombre() != null ? request.getArchivoNombre() : "seguimiento.pdf");
            archivo.setTipoContenido(request.getArchivoTipo() != null ? request.getArchivoTipo() : "application/pdf");
            archivo.setContenido(java.util.Base64.getDecoder().decode(request.getArchivoContenido()));

            archivoSeguimientoAtencionRepository.save(archivo);
        } catch (IllegalArgumentException e) {
            log.warn("Base64 inválido para archivo de seguimiento: {}", e.getMessage());
        }
    }

    private Usuario obtenerUsuarioAutenticadoRequerido() {
        Usuario usuario = obtenerUsuarioAutenticado();
        if (usuario == null) {
            throw new ResourceNotFoundException("Usuario no autenticado");
        }
        return usuario;
    }

    private Atencion obtenerAtencionRequerida(Long idAtencion) {
        if (idAtencion != null) {
            return atencionRepository.findById(idAtencion)
                    .orElseThrow(() -> new ResourceNotFoundException("Atención no encontrada con ID: " + idAtencion));
        }

        throw new IllegalArgumentException("Debe proporcionar idAtencion o citaId");
    }

    /**
     * Obtiene el usuario autenticado desde el contexto de seguridad
     */
    private Usuario obtenerUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String email = authentication.getName();
        if (email == null || email.isBlank()) {
            return null;
        }

        return usuarioRepository.findByEmail(email).orElse(null);
    }

    @Transactional(readOnly = true)
    public AtencionResponse obtenerAtencionPorCita(Long citaId) {
        Atencion atencion = atencionRepository.findByCitaId(citaId)
                .or(() -> atencionRepository.findByCasoCitaId(citaId))
                .orElseThrow(() -> new ResourceNotFoundException("Atención no encontrada para la cita ID: " + citaId));
        return mapToResponse(atencion);
    }
}
