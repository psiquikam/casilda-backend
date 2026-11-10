package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.request.*;
import co.edu.udea.casilda.dto.response.ContactoTelefonicoResponse;
import co.edu.udea.casilda.dto.response.CorreoBusquedaResponse;
import co.edu.udea.casilda.dto.response.PersonaSearchResponse;
import co.edu.udea.casilda.dto.response.DiscapacidadPersonaResponse;
import co.edu.udea.casilda.dto.response.ProfesionalResponse;
import co.edu.udea.casilda.dto.response.SolicitudAcompanamientoResponse;
import co.edu.udea.casilda.dto.response.TelefonoBusquedaResponse;
import co.edu.udea.casilda.exception.ResourceNotFoundException;
import co.edu.udea.casilda.model.entity.*;
import co.edu.udea.casilda.model.enums.EstadoCitaEnum;
import co.edu.udea.casilda.model.enums.EstadoSolicitud;
import co.edu.udea.casilda.model.enums.TipoCorreoEnum;
import co.edu.udea.casilda.model.enums.TipoSolicitud;
import co.edu.udea.casilda.model.enums.TipoTelefonoEnum;
import co.edu.udea.casilda.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de solicitudes de acompañamiento
 * Usa arquitectura relacional: Persona → Caso → SolicitudAtencion
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SolicitudAcompanamientoService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Repositorios principales
    private final PersonaRepository personaRepository;
    private final CasoRepository casoRepository;
    private final RemisionRepository remisionRepository;
    private final SolicitudAtencionRepository solicitudAtencionRepository;
    private final EntityManager entityManager;

    // Repositorios auxiliares
    private final CorreoPersonaRepository correoPersonaRepository;
    private final TelefonoPersonaRepository telefonoPersonaRepository;

    // Repositorios de maestros
    private final TipoIdentificacionRepository tipoIdentificacionRepository;
    private final IdentidadGeneroRepository identidadGeneroRepository;
    private final CampusRepository campusRepository;
    private final UnidadAdministrativaRepository unidadAdministrativaRepository;
    private final UnidadAcademicaRepository unidadAcademicaRepository;
    private final TipoSolicitudRepository tipoSolicitudRepository;
    private final EstadoSolicitudRepository estadoSolicitudRepository;
    private final CargoRepository cargoRepository;
    private final TipoCorreoRepository tipoCorreoRepository;
    private final TipoTelefonoRepository tipoTelefonoRepository;
    private final AsignacionRepository asignacionRepository;
    private final GrupoProfesionalRepository grupoProfesionalRepository;
    private final TipoAsignacionRepository tipoAsignacionRepository;
    private final TipoServicioRepository tipoServicioRepository;
    private final ContactoTelefonicoRepository contactoTelefonicoRepository;
    private final ResultadoContactoTelefonicoRepository resultadoContactoRepository;
    private final EstadoCitaRepository estadoCitaRepository;
    private final CitaRepository citaRepository;
    private final ParametroSistemaRepository parametroSistemaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MedioSolicitudRepository medioSolicitudRepository;
    private final PresuntoAgresorRepository presuntoAgresorRepository;
    private final VinculoUdeARepository vinculoUdeARepository;
    private final VinculoAgresorVictimaRepository vinculoAgresorVictimaRepository;

    /**
     * Crea una nueva solicitud de acompañamiento usando arquitectura relacional
     */
    @Transactional
    public SolicitudAcompanamientoResponse crearSolicitud(SolicitudAcompanamientoRequest request) {
        log.info("Creando solicitud de acompañamiento tipo ID: {}", request.getTipoSolicitudId());

        // Validar tipo de solicitud
        TipoSolicitud.fromId(request.getTipoSolicitudId());

        // Paso 1: Obtener/crear solicitante
        Persona solicitante = buscarOCrearPersona(request.getDatosSolicitante());

        // Paso 2: Crear Remisión si es reporte indirecto
        Remision remision = null;
        if (TipoSolicitud.esIndirecta(request.getTipoSolicitudId()) && request.getDatosRemitente() != null) {
            remision = crearRemision(request.getDatosRemitente());
        }

        // Paso 3: Crear SolicitudAtencion
        SolicitudAtencion solicitud = crearSolicitudAtencion(remision, solicitante, request);

        log.info("Solicitud creada exitosamente con ID: {}", solicitud.getId());

        // Retornar solo datos de la solicitud, sin datos de caso
        return buildResponse(solicitud, remision);
    }

    /**
     * Obtiene una solicitud por ID
     */
    @Transactional(readOnly = true)
    public SolicitudAcompanamientoResponse obtenerPorId(Long id) {
        log.info("Obteniendo solicitud con ID: {}", id);
        SolicitudAtencion solicitud = solicitudAtencionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con ID: " + id));

        return buildResponse(solicitud, solicitud.getRemision());
    }

    /**
     * Busca los datos de una persona por documento para autocompletar formulario.
     * Solo consulta la tabla persona y sus relaciones directas (correos/teléfonos).
     */
    @Transactional(readOnly = true)
    public PersonaSearchResponse buscarPersonaPorDocumento(String numeroDocumento, Integer tipoDocumentoId) {
        log.info("Buscando persona por documento: {} y tipoDocumentoId: {}", numeroDocumento, tipoDocumentoId);

        Optional<Persona> personaOptional = tipoDocumentoId != null
                ? personaRepository.findByNumeroDocumentoAndTipoIdentificacion_Id(numeroDocumento, tipoDocumentoId)
                : personaRepository.findByNumeroDocumento(numeroDocumento);

        if (tipoDocumentoId != null && personaOptional.isEmpty()) {
            Optional<Persona> personaPorDocumento = personaRepository.findByNumeroDocumento(numeroDocumento);
            if (personaPorDocumento.isPresent()) {
                Persona personaExistente = personaPorDocumento.get();
                Integer tipoRealId = personaExistente.getTipoIdentificacion() != null
                        ? personaExistente.getTipoIdentificacion().getId()
                        : null;
                String tipoRealNombre = personaExistente.getTipoIdentificacion() != null
                        ? personaExistente.getTipoIdentificacion().getNombre()
                        : "Sin tipo";

                throw new IllegalArgumentException(
                        "El documento " + numeroDocumento + " existe, pero corresponde al tipo de documento "
                                + tipoRealNombre + " (ID: " + tipoRealId + "). Verifique el tipo seleccionado.");
            }
        }

        Persona persona = personaOptional
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró persona con documento: " + numeroDocumento));

        return PersonaSearchResponse.builder()
                .id(persona.getId())
                .primerNombre(persona.getPrimerNombre())
                .segundoNombre(persona.getSegundoNombre())
                .primerApellido(persona.getPrimerApellido())
                .segundoApellido(persona.getSegundoApellido())
                .tipoDocumentoId(
                        persona.getTipoIdentificacion() != null ? persona.getTipoIdentificacion().getId() : null)
                .numeroDocumento(persona.getNumeroDocumento())
                .fechaNacimiento(
                        persona.getFechaNacimiento() != null ? persona.getFechaNacimiento().format(DATE_FORMATTER)
                                : null)
                .correos(persona.getCorreos() == null ? List.of()
                        : persona.getCorreos().stream()
                                .map(c -> CorreoBusquedaResponse.builder()
                                        .tipoId(c.getTipoCorreo() != null ? c.getTipoCorreo().getId() : c.getIdtipo())
                                        .tipo(c.getTipoCorreo() != null ? c.getTipoCorreo().getNombre() : null)
                                        .correo(c.getCorreo())
                                        .build())
                                .collect(Collectors.toList()))
                .telefonos(persona.getTelefonos() == null ? List.of()
                        : persona.getTelefonos().stream()
                                .map(t -> TelefonoBusquedaResponse.builder()
                                        .tipoId(t.getTipoTelefono() != null ? t.getTipoTelefono().getId()
                                                : t.getIdtipo())
                                        .tipo(t.getTipoTelefono() != null ? t.getTipoTelefono().getNombre() : null)
                                        .telefono(t.getTelefono())
                                        .build())
                                .collect(Collectors.toList()))
                .sexoId(persona.getSexo() != null ? persona.getSexo().getId() : null)
                .ciudadNacimientoId(persona.getCiudadNacimiento() != null ? persona.getCiudadNacimiento().getId() : null)
                .departamentoNacimientoId(persona.getCiudadNacimiento() != null && persona.getCiudadNacimiento().getDepartamento() != null
                        ? persona.getCiudadNacimiento().getDepartamento().getId() : null)
                .discapacidades(persona.getDiscapacidades() == null ? List.of()
                        : persona.getDiscapacidades().stream()
                                .map(d -> DiscapacidadPersonaResponse.builder()
                                        .idSubTipoDiscapacidad(d.getIdsubtipodiscapacidad())
                                        .subTipo(d.getSubTipoDiscapacidad() != null ? d.getSubTipoDiscapacidad().getNombre() : null)
                                        .tipo(d.getSubTipoDiscapacidad() != null && d.getSubTipoDiscapacidad().getTipo() != null
                                                ? d.getSubTipoDiscapacidad().getTipo().getNombre() : null)
                                        .descripcion(d.getDescripcion())
                                        .build())
                                .collect(Collectors.toList()))
                .build();
    }

    /**
     * Lista todas las solicitudes
     */
    @Transactional(readOnly = true)
    public List<SolicitudAcompanamientoResponse> listarTodas(Integer idEstadoSolicitud) {
        log.info("Listando solicitudes de acompañamiento con filtro idEstadoSolicitud={}", idEstadoSolicitud);

        List<SolicitudAtencion> solicitudes = idEstadoSolicitud == null
                ? solicitudAtencionRepository.findAll()
                : solicitudAtencionRepository.findByEstadoSolicitudIdOrderByFechaCreacionDesc(idEstadoSolicitud);

        return solicitudes.stream()
                .map(solicitud -> buildResponse(solicitud, solicitud.getRemision()))
                .collect(Collectors.toList());
    }

    /**
     * Lista solicitudes paginadas
     */
    @Transactional(readOnly = true)
    public Page<SolicitudAcompanamientoResponse> listarPaginadas(int page, int size, Integer idEstadoSolicitud) {
        log.info("Listando solicitudes paginadas. page={}, size={}, idEstadoSolicitud={}", page, size,
                idEstadoSolicitud);

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<SolicitudAtencion> solicitudes = idEstadoSolicitud == null
                ? solicitudAtencionRepository.findAllByOrderByFechaCreacionDesc(pageable)
                : solicitudAtencionRepository.findByEstadoSolicitudIdOrderByFechaCreacionDesc(idEstadoSolicitud,
                        pageable);

        return solicitudes.map(solicitud -> buildResponse(solicitud, solicitud.getRemision()));
    }

    // ===========================
    // Métodos auxiliares privados
    // ===========================

    /**
     * Busca una persona existente por número de documento o crea una nueva
     */
    private Persona buscarOCrearPersona(DatosSolicitanteRequest datos) {
        // Buscar persona existente
        Optional<Persona> existente = personaRepository.findByNumeroDocumento(datos.getNumeroDocumento());
        if (existente.isPresent()) {
            log.info("Persona encontrada con documento: {}", datos.getNumeroDocumento());
            Persona personaExistente = existente.get();
            if (datos.getVinculoUniversidadId() != null) {
                personaExistente.setVinculoUdea(buscarVinculoUdea(datos.getVinculoUniversidadId()));
                personaRepository.save(personaExistente);
            }
            return personaExistente;
        }

        log.info("Creando nueva persona con documento: {}", datos.getNumeroDocumento());

        // Crear nueva persona
        Persona persona = new Persona();
        persona.setPrimerNombre(datos.getPrimerNombre());
        persona.setSegundoNombre(datos.getSegundoNombre());
        persona.setPrimerApellido(datos.getPrimerApellido());
        persona.setSegundoApellido(datos.getSegundoApellido());
        persona.setNumeroDocumento(datos.getNumeroDocumento());

        // Usar la fecha de nacimiento proporcionada
        if (datos.getFechaNacimiento() != null) {
            persona.setFechaNacimiento(datos.getFechaNacimiento().atStartOfDay());
        }

        // Lookup FK a tablas maestras usando IDs
        persona.setTipoIdentificacion(tipoIdentificacionRepository.findById(datos.getTipoDocumentoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TipoIdentificacion no encontrado con ID: " + datos.getTipoDocumentoId())));

        if (datos.getVinculoUniversidadId() != null) {
            persona.setVinculoUdea(buscarVinculoUdea(datos.getVinculoUniversidadId()));
        }

        Persona personaGuardada = personaRepository.save(persona);

        // Crear correos
        crearCorreos(personaGuardada, datos);

        // Crear teléfonos
        crearTelefonos(personaGuardada, datos);

        // Re-fetchear para que las colecciones correos/teléfonos estén cargadas en
        // memoria
        return personaRepository.findById(personaGuardada.getId()).orElse(personaGuardada);
    }

    /**
     * Crea registros de correos para la persona
     * Si la lista correos no está vacía, la usa; si no, cae a los campos
     * individuales.
     */
    private void crearCorreos(Persona persona, DatosSolicitanteRequest datos) {
        List<TipoCorreo> tiposCorreo = tipoCorreoRepository.findAll();

        if (datos.getCorreos() != null && !datos.getCorreos().isEmpty()) {
            for (CorreoSolicitanteRequest req : datos.getCorreos()) {
                if (req.getCorreo() == null || req.getCorreo().isBlank())
                    continue;
                TipoCorreo tipo = tiposCorreo.stream()
                        .filter(t -> t.getId().equals(req.getTipoId()))
                        .findFirst()
                        .orElse(tiposCorreo.stream().findFirst()
                                .orElseThrow(() -> new ResourceNotFoundException(
                                        "No hay tipos de correo en la base de datos")));
                CorreoPersona correo = new CorreoPersona();
                correo.setIdpersona(persona.getId());
                correo.setIdtipo(tipo.getId());
                correo.setPersona(persona);
                correo.setTipoCorreo(tipo);
                correo.setCorreo(req.getCorreo());
                // Removed descripcion field as it was deleted from entity
                correoPersonaRepository.save(correo);
            }
            return;
        }

    }

    /**
     * Crea registros de teléfonos para la persona
     * Si la lista telefonos no está vacía, la usa; si no, cae a los campos
     * individuales.
     */
    private void crearTelefonos(Persona persona, DatosSolicitanteRequest datos) {
        List<TipoTelefono> tiposTelefono = tipoTelefonoRepository.findAll();

        if (datos.getTelefonos() != null && !datos.getTelefonos().isEmpty()) {
            for (TelefonoSolicitanteRequest req : datos.getTelefonos()) {
                if (req.getTelefono() == null || req.getTelefono().isBlank())
                    continue;
                TipoTelefono tipo = tiposTelefono.stream()
                        .filter(t -> t.getId().equals(req.getTipoId()))
                        .findFirst()
                        .orElse(tiposTelefono.stream().findFirst()
                                .orElseThrow(() -> new ResourceNotFoundException(
                                        "No hay tipos de teléfono en la base de datos")));
                TelefonoPersona telefono = new TelefonoPersona();
                telefono.setIdpersona(persona.getId());
                telefono.setIdtipo(tipo.getId());
                telefono.setPersona(persona);
                telefono.setTipoTelefono(tipo);
                telefono.setTelefono(req.getTelefono());
                telefonoPersonaRepository.save(telefono);
            }
            return;
        }

    }

    /**
     * Crea una remisión para reportes indirectos
     */
    private Remision crearRemision(DatosRemitenteRequest datos) {
        Usuario usuarioAutenticado = obtenerUsuarioAutenticado();

        // Crear persona simple para remitente (sin validación de documento)
        Persona remitente = new Persona();
        remitente.setPrimerNombre(datos.getPrimerNombre());
        remitente.setSegundoNombre(datos.getSegundoNombre());
        remitente.setPrimerApellido(datos.getPrimerApellido());
        remitente.setSegundoApellido(datos.getSegundoApellido());
        remitente.setNumeroDocumento(datos.getNumeroDocumento());
        remitente.setTipoIdentificacion(tipoIdentificacionRepository.findById(datos.getTipoDocumentoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TipoIdentificacion no encontrado con ID: " + datos.getTipoDocumentoId())));

        Persona remitenteGuardado = personaRepository.save(remitente);

        // Crear remisión
        Remision remision = new Remision();
        remision.setRemitente(remitenteGuardado);
        remision.setUsuarioCreacion(usuarioAutenticado);
        remision.setUsuarioActualizacion(usuarioAutenticado);

        remision.setCargo(cargoRepository.findById(datos.getCargoId())
                .orElseThrow(() -> new ResourceNotFoundException("Cargo no encontrado con ID: " + datos.getCargoId())));

        if (datos.getUnidadAdministrativaId() != null) {
            remision.setUnidadAdministrativa(unidadAdministrativaRepository.findById(datos.getUnidadAdministrativaId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Unidad administrativa no encontrada con ID: " + datos.getUnidadAdministrativaId())));
        }

        if (datos.getUnidadAcademicaId() != null) {
            remision.setUnidadAcademica(unidadAcademicaRepository.findById(datos.getUnidadAcademicaId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Unidad académica no encontrada con ID: " + datos.getUnidadAcademicaId())));
        }

        if (datos.getCampusId() != null) {
            remision.setCampus(campusRepository.findById(datos.getCampusId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Campus no encontrado con ID: " + datos.getCampusId())));
        }

        return remisionRepository.save(remision);
    }

    /**
     * Crea la solicitud de atención
     */
    private SolicitudAtencion crearSolicitudAtencion(Remision remision, Persona solicitante,
            SolicitudAcompanamientoRequest request) {
        SolicitudAtencion solicitud = new SolicitudAtencion();
        Usuario usuarioAutenticado = obtenerUsuarioAutenticado();

        // Asociar remisión
        solicitud.setRemision(remision);
        solicitud.setSolicitante(solicitante);

        // Establecer identidad de género en la solicitud (no en caso)
        solicitud.setIdentidadGenero(
                identidadGeneroRepository.findById(request.getDatosSolicitante().getIdentidadGeneroId())
                        .orElseThrow(() -> new ResourceNotFoundException("IdentidadGenero no encontrada con ID: "
                                + request.getDatosSolicitante().getIdentidadGeneroId())));

        solicitud.setUsuarioCreacion(usuarioAutenticado);
        solicitud.setUsuarioActualizacion(usuarioAutenticado);

        solicitud.setTipoSolicitud(tipoSolicitudRepository.findById(request.getTipoSolicitudId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TipoSolicitud no encontrado con ID: " + request.getTipoSolicitudId())));

        // Establecer medio de solicitud
        if (request.getMedioSolicitudId() != null) {
            solicitud.setMedioSolicitud(medioSolicitudRepository.findById(request.getMedioSolicitudId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Medio de solicitud no encontrado con ID: " + request.getMedioSolicitudId())));
        }

        // Establecer estado inicial en SIN_ASIGNAR
        solicitud.setEstadoSolicitud(estadoSolicitudRepository.findById(EstadoSolicitud.SIN_ASIGNAR.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "EstadoSolicitud no encontrado con ID: " + EstadoSolicitud.SIN_ASIGNAR.getId())));

        solicitud.setObservacionesTelefono(request.getObservacionesTelefono());
        solicitud.setObservacionesCorreo(request.getObservacionesCorreo());
        solicitud.setDescripcionHechos(request.getDescripcionHechos().trim());
        solicitud.setLugarOcurrencia(normalizarTextoOpcional(request.getLugarOcurrencia()));

        SolicitudAtencion guardada = solicitudAtencionRepository.save(solicitud);
        guardarPresuntoAgresor(guardada, request.getAgresorIdentificacion(), request.getAgresorNombre(),
                request.getAgresorVinculoUdeaId(), request.getAgresorOtroVinculoUdea(),
                request.getAgresorVinculoVictimaId(), request.getAgresorOtroVinculoVictima(),
                request.getAgresorCorreo(), request.getAgresorRemitirUad());
        return guardada;
    }

    private VinculoUdeA buscarVinculoUdea(Integer id) {
        return vinculoUdeARepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo con la universidad no encontrado con ID: " + id));
    }

    private boolean esVinculoOtro(String nombre) {
        return nombre != null && nombre.trim().toLowerCase().startsWith("otr");
    }

    /**
     * Crea, actualiza o elimina la presunta persona agresora de la solicitud.
     * Los campos "otro vínculo" solo se guardan cuando el vínculo seleccionado es "Otro".
     */
    private void guardarPresuntoAgresor(SolicitudAtencion solicitud, String identificacion, String nombre,
            Integer vinculoUdeaId, String otroVinculoUdea, Integer vinculoVictimaId, String otroVinculoVictima,
            String correo, String remitirUad) {
        boolean sinDatos = !tieneTexto(identificacion) && !tieneTexto(nombre) && vinculoUdeaId == null
                && vinculoVictimaId == null && !tieneTexto(correo) && !tieneTexto(otroVinculoUdea)
                && !tieneTexto(otroVinculoVictima) && !tieneTexto(remitirUad);

        Optional<PresuntoAgresor> existente = presuntoAgresorRepository.findBySolicitudAtencionId(solicitud.getId());
        if (sinDatos) {
            existente.ifPresent(agresor -> {
                solicitud.setPresuntoAgresor(null);
                presuntoAgresorRepository.delete(agresor);
            });
            return;
        }

        PresuntoAgresor agresor = existente.orElseGet(PresuntoAgresor::new);
        agresor.setSolicitudAtencion(solicitud);
        agresor.setIdentificacion(normalizarTextoOpcional(identificacion));
        agresor.setNombre(normalizarTextoOpcional(nombre));
        agresor.setCorreo(normalizarTextoOpcional(correo));
        agresor.setRemitirUad(normalizarTextoOpcional(remitirUad));

        VinculoUdeA vinculoUdea = vinculoUdeaId != null ? buscarVinculoUdea(vinculoUdeaId) : null;
        agresor.setVinculoUdeA(vinculoUdea);
        agresor.setOtroVinculoUdeA(
                vinculoUdea != null && esVinculoOtro(vinculoUdea.getNombre())
                        ? normalizarTextoOpcional(otroVinculoUdea)
                        : null);

        VinculoAgresorVictima vinculoVictima = vinculoVictimaId != null
                ? vinculoAgresorVictimaRepository.findById(vinculoVictimaId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Vínculo agresor-víctima no encontrado con ID: " + vinculoVictimaId))
                : null;
        agresor.setVinculoAgresorVictima(vinculoVictima);
        agresor.setOtroVinculoAgresorVictima(
                vinculoVictima != null && esVinculoOtro(vinculoVictima.getNombre())
                        ? normalizarTextoOpcional(otroVinculoVictima)
                        : null);

        solicitud.setPresuntoAgresor(presuntoAgresorRepository.save(agresor));
    }

    /**
     * Construye el response DTO completo con todos los campos del componente de
     * consulta
     */
    private SolicitudAcompanamientoResponse buildResponse(SolicitudAtencion solicitud, Remision remision) {

        Persona solicitante = solicitud.getSolicitante();
        if (solicitante == null) {
            // Fallback: si no hay remitente, usar la primera persona disponible
            log.warn("No se encontró remitente en la solicitud ID: {}", solicitud.getId());
            solicitante = new Persona();
        }

        Persona remitentePersona = remision != null ? remision.getRemitente() : null;

        // Reparto vigente: la asignación más reciente (fechaCreacion e id descendentes)
        Asignacion asignacionVigente = solicitud.getAsignaciones() == null ? null
                : solicitud.getAsignaciones().stream()
                        .max(Comparator
                                .comparing(Asignacion::getFechaCreacion,
                                        Comparator.nullsFirst(Comparator.<LocalDateTime>naturalOrder()))
                                .thenComparing(Asignacion::getId,
                                        Comparator.nullsFirst(Comparator.<Long>naturalOrder())))
                        .orElse(null);
        GrupoProfesional grupoVigente = asignacionVigente != null ? asignacionVigente.getGrupoProfesional() : null;
        TipoAsignacion tipoAsignacionVigente = asignacionVigente != null ? asignacionVigente.getTipoAsignacion()
                : null;
        TipoServicio tipoServicioVigente = asignacionVigente != null ? asignacionVigente.getTipoServicio() : null;
        PresuntoAgresor agresor = presuntoAgresorRepository.findBySolicitudAtencionId(solicitud.getId())
                .orElse(null);
        VinculoUdeA vinculoSolicitante = solicitante.getVinculoUdea();

        // Correos del solicitante (usando TipoCorreoEnum)
        String correoInstitucional = "";
        String correoPersonal = "";
        if (solicitante != null && solicitante.getCorreos() != null) {
            for (CorreoPersona cp : solicitante.getCorreos()) {
                if (cp.getTipoCorreo() == null || cp.getCorreo() == null)
                    continue;
                Integer idTipo = cp.getTipoCorreo().getId();
                if (TipoCorreoEnum.INSTITUCIONAL.getId().equals(idTipo) && correoInstitucional.isEmpty()) {
                    correoInstitucional = cp.getCorreo();
                } else if (TipoCorreoEnum.PERSONAL.getId().equals(idTipo) && correoPersonal.isEmpty()) {
                    correoPersonal = cp.getCorreo();
                } else if (correoInstitucional.isEmpty()) {
                    correoInstitucional = cp.getCorreo();
                } else if (correoPersonal.isEmpty()) {
                    correoPersonal = cp.getCorreo();
                }
            }
        }

        // Teléfonos del solicitante (usando TipoTelefonoEnum)
        String celular = "";
        String telefonoAlterno = "";
        if (solicitante != null && solicitante.getTelefonos() != null) {
            for (TelefonoPersona tp : solicitante.getTelefonos()) {
                if (tp.getTipoTelefono() == null || tp.getTelefono() == null)
                    continue;
                Integer idTipo = tp.getTipoTelefono().getId();
                if ((TipoTelefonoEnum.CELULAR.getId().equals(idTipo)
                        || TipoTelefonoEnum.WHATSAPP.getId().equals(idTipo))
                        && celular.isEmpty()) {
                    celular = tp.getTelefono();
                } else if ((TipoTelefonoEnum.FIJO.getId().equals(idTipo)
                        || TipoTelefonoEnum.OFICINA.getId().equals(idTipo))
                        && telefonoAlterno.isEmpty()) {
                    telefonoAlterno = tp.getTelefono();
                } else if (celular.isEmpty()) {
                    celular = tp.getTelefono();
                } else if (telefonoAlterno.isEmpty()) {
                    telefonoAlterno = tp.getTelefono();
                }
            }
        }

        List<CorreoBusquedaResponse> correos = solicitante != null && solicitante.getCorreos() != null
                ? solicitante.getCorreos().stream()
                        .filter(correo -> tieneTexto(correo.getCorreo()))
                        .map(correo -> CorreoBusquedaResponse.builder()
                                .tipoId(correo.getTipoCorreo() != null ? correo.getTipoCorreo().getId()
                                        : correo.getIdtipo())
                                .tipo(correo.getTipoCorreo() != null ? correo.getTipoCorreo().getNombre() : null)
                                .correo(correo.getCorreo())
                                .build())
                        .collect(Collectors.toList())
                : List.of();

        List<TelefonoBusquedaResponse> telefonos = solicitante != null && solicitante.getTelefonos() != null
                ? solicitante.getTelefonos().stream()
                        .filter(telefono -> tieneTexto(telefono.getTelefono()))
                        .map(telefono -> TelefonoBusquedaResponse.builder()
                                .tipoId(telefono.getTipoTelefono() != null ? telefono.getTipoTelefono().getId()
                                        : telefono.getIdtipo())
                                .tipo(telefono.getTipoTelefono() != null ? telefono.getTipoTelefono().getNombre()
                                        : null)
                                .telefono(telefono.getTelefono())
                                .build())
                        .collect(Collectors.toList())
                : List.of();

        // Campos del remitente
        String remitentePrimerNombre = "";
        String remitenteSegundoNombre = "";
        String remitentePrimerApellido = "";
        String remitenteSegundoApellido = "";
        Integer remitenteCargoId = null;
        String remitenteCargo = "";
        Integer remitenteCampusId = null;
        String remitenteCampus = "";
        Integer remitenteUnidadAdministrativaId = null;
        String remitenteUnidadAdministrativa = "";
        Integer remitenteUnidadAcademicaId = null;
        String remitenteUnidadAcademica = "";
        String remitenteFechaSolicitud = remision != null && remision.getFechaCreacion() != null
                ? remision.getFechaCreacion().format(DATE_FORMATTER)
                : "";
        Integer remitenteTipoDocumentoId = null;
        String remitenteTipoDocumento = "";
        String remitenteNumeroDocumento = "";
        String nombreRemitente = null;

        if (remision != null && remitentePersona != null) {
            nombreRemitente = remitentePersona.getNombreCompleto();
            remitentePrimerNombre = remitentePersona.getPrimerNombre() != null ? remitentePersona.getPrimerNombre()
                    : "";
            remitenteSegundoNombre = remitentePersona.getSegundoNombre() != null ? remitentePersona.getSegundoNombre()
                    : "";
            remitentePrimerApellido = remitentePersona.getPrimerApellido() != null
                    ? remitentePersona.getPrimerApellido()
                    : "";
            remitenteSegundoApellido = remitentePersona.getSegundoApellido() != null
                    ? remitentePersona.getSegundoApellido()
                    : "";
            remitenteTipoDocumentoId = remitentePersona.getTipoIdentificacion() != null
                    ? remitentePersona.getTipoIdentificacion().getId()
                    : null;
            remitenteTipoDocumento = remitentePersona.getTipoIdentificacion() != null
                    ? remitentePersona.getTipoIdentificacion().getNombre()
                    : "";
            remitenteNumeroDocumento = remitentePersona.getNumeroDocumento() != null
                    ? remitentePersona.getNumeroDocumento()
                    : "";
            remitenteCargoId = remision.getCargo() != null ? remision.getCargo().getId() : null;
            remitenteCargo = remision.getCargo() != null ? remision.getCargo().getNombre() : "";
            remitenteCampusId = remision.getCampus() != null ? remision.getCampus().getId() : null;
            remitenteCampus = remision.getCampus() != null ? remision.getCampus().getNombre() : "";
            remitenteUnidadAdministrativaId = remision.getUnidadAdministrativa() != null ? remision.getUnidadAdministrativa().getId() : null;
            remitenteUnidadAdministrativa = remision.getUnidadAdministrativa() != null ? remision.getUnidadAdministrativa().getNombre() : "";
            remitenteUnidadAcademicaId = remision.getUnidadAcademica() != null ? remision.getUnidadAcademica().getId() : null;
            remitenteUnidadAcademica = remision.getUnidadAcademica() != null ? remision.getUnidadAcademica().getNombre() : "";
        }

        Integer medioSolicitudId = solicitud.getMedioSolicitud() != null ? solicitud.getMedioSolicitud().getId() : null;
        String medioSolicitud = solicitud.getMedioSolicitud() != null ? solicitud.getMedioSolicitud().getNombre() : "";

        return SolicitudAcompanamientoResponse.builder()
                .id(solicitud.getId())
                .codigo("SOL-" + solicitud.getId())
                .tipoSolicitud(solicitud.getTipoSolicitud().getNombre())
                .idEstadoSolicitud(
                        solicitud.getEstadoSolicitud() != null ? solicitud.getEstadoSolicitud().getId() : null)
                .estado(solicitud.getEstadoSolicitud().getNombre())
                .fechaCreacion(solicitud.getFechaCreacion())
                .medioSolicitudId(medioSolicitudId)
                .medioSolicitud(medioSolicitud)
                .observacionesTelefono(solicitud.getObservacionesTelefono())
                .observacionesCorreo(solicitud.getObservacionesCorreo())
                .profesional(grupoVigente != null ? grupoVigente.getNombre() : null)
                .grupoProfesionalId(grupoVigente != null ? grupoVigente.getId() : null)
                .tipoAsignacion(tipoAsignacionVigente != null ? tipoAsignacionVigente.getNombre() : null)
                .tipoAsignacionId(tipoAsignacionVigente != null ? tipoAsignacionVigente.getId() : null)
                .tipoServicio(tipoServicioVigente != null ? tipoServicioVigente.getNombre() : null)
                .tipoServicioId(tipoServicioVigente != null ? tipoServicioVigente.getId() : null)
                .fechaReparto(asignacionVigente != null ? asignacionVigente.getFechaReparto() : null)
                .observacionesReparto(asignacionVigente != null ? asignacionVigente.getObservaciones() : null)
                .descripcionHechos(solicitud.getDescripcionHechos())
                .lugarOcurrencia(solicitud.getLugarOcurrencia())
                .vinculoUniversidadId(vinculoSolicitante != null ? vinculoSolicitante.getId() : null)
                .vinculoUniversidad(vinculoSolicitante != null ? vinculoSolicitante.getNombre() : null)
                .agresorIdentificacion(agresor != null ? agresor.getIdentificacion() : null)
                .agresorNombre(agresor != null ? agresor.getNombre() : null)
                .agresorVinculoUdeaId(agresor != null && agresor.getVinculoUdeA() != null
                        ? agresor.getVinculoUdeA().getId() : null)
                .agresorVinculoUdea(agresor != null && agresor.getVinculoUdeA() != null
                        ? agresor.getVinculoUdeA().getNombre() : null)
                .agresorOtroVinculoUdea(agresor != null ? agresor.getOtroVinculoUdeA() : null)
                .agresorVinculoVictimaId(agresor != null && agresor.getVinculoAgresorVictima() != null
                        ? agresor.getVinculoAgresorVictima().getId() : null)
                .agresorVinculoVictima(agresor != null && agresor.getVinculoAgresorVictima() != null
                        ? agresor.getVinculoAgresorVictima().getNombre() : null)
                .agresorOtroVinculoVictima(agresor != null ? agresor.getOtroVinculoAgresorVictima() : null)
                .agresorCorreo(agresor != null ? agresor.getCorreo() : null)
                .agresorRemitirUad(agresor != null ? agresor.getRemitirUad() : null)
                // Solicitante resumen
                .nombreSolicitante(solicitante != null ? solicitante.getNombreCompleto() : "")
                .documentoSolicitante(solicitante != null ? solicitante.getNumeroDocumento() : "")
                // Solicitante completo
                .tipoDocumentoId(solicitante != null && solicitante.getTipoIdentificacion() != null
                        ? solicitante.getTipoIdentificacion().getId()
                        : null)
                .tipoDocumento(solicitante != null && solicitante.getTipoIdentificacion() != null
                        ? solicitante.getTipoIdentificacion().getNombre()
                        : "")
                .numeroDocumento(solicitante != null ? solicitante.getNumeroDocumento() : "")
                .fechaNacimiento(solicitante != null && solicitante.getFechaNacimiento() != null
                        ? solicitante.getFechaNacimiento().format(DATE_FORMATTER)
                        : null)
                .primerNombre(
                        solicitante != null && solicitante.getPrimerNombre() != null ? solicitante.getPrimerNombre()
                                : "")
                .segundoNombre(
                        solicitante != null && solicitante.getSegundoNombre() != null ? solicitante.getSegundoNombre()
                                : "")
                .primerApellido(
                        solicitante != null && solicitante.getPrimerApellido() != null ? solicitante.getPrimerApellido()
                                : "")
                .segundoApellido(solicitante != null && solicitante.getSegundoApellido() != null
                        ? solicitante.getSegundoApellido()
                        : "")
                .identidadGeneroId(
                        solicitud.getIdentidadGenero() != null ? solicitud.getIdentidadGenero().getId() : null)
                .identidadGenero(
                        solicitud.getIdentidadGenero() != null ? solicitud.getIdentidadGenero().getNombre() : "")
                .idDepartamentoResidencia(null) // No disponible en Persona
                .idCiudadResidencia(null) // No disponible en Persona
                .direccionResidencia(null) // No disponible en Persona
                .celular(celular)
                .telefonoAlterno(telefonoAlterno)
                .correoInstitucional(correoInstitucional)
                .correoPersonal(correoPersonal)
                .correos(correos)
                .telefonos(telefonos)
                // Remitente
                .nombreRemitente(nombreRemitente)
                .remitentePrimerNombre(remitentePrimerNombre)
                .remitenteSegundoNombre(remitenteSegundoNombre)
                .remitentePrimerApellido(remitentePrimerApellido)
                .remitenteSegundoApellido(remitenteSegundoApellido)
                .remitenteCargoId(remitenteCargoId)
                .remitenteCargo(remitenteCargo)
                .remitenteCampusId(remitenteCampusId)
                .remitenteCampus(remitenteCampus)
                .remitenteUnidadAdministrativaId(remitenteUnidadAdministrativaId)
                .remitenteUnidadAdministrativa(remitenteUnidadAdministrativa)
                .remitenteUnidadAcademicaId(remitenteUnidadAcademicaId)
                .remitenteUnidadAcademica(remitenteUnidadAcademica)
                .remitenteFechaSolicitud(remitenteFechaSolicitud)
                .remitenteTipoDocumentoId(remitenteTipoDocumentoId)
                .remitenteTipoDocumento(remitenteTipoDocumento)
                .remitenteNumeroDocumento(remitenteNumeroDocumento)
                .build();
    }

    /**
     * Elimina una solicitud de atención por su ID
     */
    @Transactional
    public void eliminarSolicitud(Long id) {
        log.info("Eliminando solicitud con ID: {}", id);
        SolicitudAtencion solicitud = solicitudAtencionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con ID: " + id));
        presuntoAgresorRepository.deleteBySolicitudAtencionId(id);
        solicitudAtencionRepository.delete(solicitud);
    }

    /**
     * Actualiza los datos editables de una solicitud (Persona del caso)
     */
    @Transactional
    public SolicitudAcompanamientoResponse actualizarSolicitud(Long id, UpdateSolicitudRequest req) {
        log.info("Actualizando solicitud con ID: {}", id);
        SolicitudAtencion solicitud = solicitudAtencionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con ID: " + id));

        Persona solicitante = solicitud.getSolicitante();
        if (solicitante == null) {
            throw new ResourceNotFoundException("La solicitud no tiene solicitante asociado con ID: " + id);
        }

        actualizarSolicitante(solicitante, req);
        actualizarIdentidadGenero(solicitud, req.getIdentidadGeneroId());
        reemplazarCorreosSolicitante(solicitante, req.getCorreos());
        reemplazarTelefonosSolicitante(solicitante, req.getTelefonos());

        solicitud.setObservacionesTelefono(req.getObservacionesTelefono());
        solicitud.setObservacionesCorreo(req.getObservacionesCorreo());
        solicitud.setDescripcionHechos(req.getDescripcionHechos().trim());
        solicitud.setLugarOcurrencia(normalizarTextoOpcional(req.getLugarOcurrencia()));
        guardarPresuntoAgresor(solicitud, req.getAgresorIdentificacion(), req.getAgresorNombre(),
                req.getAgresorVinculoUdeaId(), req.getAgresorOtroVinculoUdea(),
                req.getAgresorVinculoVictimaId(), req.getAgresorOtroVinculoVictima(),
                req.getAgresorCorreo(), req.getAgresorRemitirUad());

        if (req.getMedioSolicitudId() != null) {
            solicitud.setMedioSolicitud(medioSolicitudRepository.findById(req.getMedioSolicitudId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Medio de solicitud no encontrado con ID: " + req.getMedioSolicitudId())));
        } else {
            solicitud.setMedioSolicitud(null);
        }

        Remision remision = solicitud.getRemision();
        if (remision != null) {
            actualizarRemitente(remision, req);
        }

        Usuario usuarioAutenticado = obtenerUsuarioAutenticado();
        if (usuarioAutenticado != null) {
            solicitud.setUsuarioActualizacion(usuarioAutenticado);
            if (remision != null) {
                remision.setUsuarioActualizacion(usuarioAutenticado);
            }
            casoRepository.findByCitaSolicitudAtencionIdOrderByFechaCreacionDesc(solicitud.getId()).stream().findFirst()
                    .ifPresent(caso -> {
                        caso.setUsuarioActualizacion(usuarioAutenticado);
                        casoRepository.save(caso);
                    });
        }

        personaRepository.save(solicitante);
        if (remision != null && remision.getRemitente() != null) {
            personaRepository.save(remision.getRemitente());
            remisionRepository.save(remision);
        }
        solicitudAtencionRepository.save(solicitud);

        return buildResponse(solicitud, solicitud.getRemision());
    }

    private void actualizarSolicitante(Persona solicitante, UpdateSolicitudRequest req) {
        if (tieneTexto(req.getPrimerNombre())) {
            solicitante.setPrimerNombre(req.getPrimerNombre().trim());
        }
        if (req.getSegundoNombre() != null) {
            solicitante.setSegundoNombre(normalizarTextoOpcional(req.getSegundoNombre()));
        }
        if (tieneTexto(req.getPrimerApellido())) {
            solicitante.setPrimerApellido(req.getPrimerApellido().trim());
        }
        if (req.getSegundoApellido() != null) {
            solicitante.setSegundoApellido(normalizarTextoOpcional(req.getSegundoApellido()));
        }
        if (tieneTexto(req.getNumeroDocumento())) {
            solicitante.setNumeroDocumento(req.getNumeroDocumento().trim());
        }
        if (req.getFechaNacimiento() != null) {
            solicitante.setFechaNacimiento(req.getFechaNacimiento().atStartOfDay());
        }
        if (req.getVinculoUniversidadId() != null) {
            solicitante.setVinculoUdea(buscarVinculoUdea(req.getVinculoUniversidadId()));
        }
        if (req.getTipoDocumentoId() != null) {
            solicitante.setTipoIdentificacion(tipoIdentificacionRepository.findById(req.getTipoDocumentoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Tipo de documento no encontrado con ID: " + req.getTipoDocumentoId())));
        }
    }

    private void actualizarIdentidadGenero(SolicitudAtencion solicitud, Integer identidadGeneroId) {
        if (identidadGeneroId == null) {
            return;
        }

        solicitud.setIdentidadGenero(identidadGeneroRepository.findById(identidadGeneroId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Identidad de género no encontrada con ID: " + identidadGeneroId)));
    }

    private void reemplazarCorreosSolicitante(Persona solicitante, List<UpdateCorreoSolicitudRequest> correos) {
        if (correos == null) {
            return;
        }

        solicitante.getCorreos().clear();
        entityManager.flush();

        for (UpdateCorreoSolicitudRequest req : correos) {
            if (!tieneTexto(req.getCorreo())) {
                continue;
            }

            TipoCorreo tipoCorreo = resolverTipoCorreo(req);
            CorreoPersona correoPersona = new CorreoPersona();
            correoPersona.setIdpersona(solicitante.getId());
            correoPersona.setIdtipo(tipoCorreo.getId());
            correoPersona.setPersona(solicitante);
            correoPersona.setTipoCorreo(tipoCorreo);
            correoPersona.setCorreo(req.getCorreo().trim());
            // observaciones are stored at SolicitudAtencion level; no per-email description
            solicitante.getCorreos().add(correoPersona);
        }
    }

    private void reemplazarTelefonosSolicitante(Persona solicitante, List<UpdateTelefonoSolicitudRequest> telefonos) {
        if (telefonos == null) {
            return;
        }

        solicitante.getTelefonos().clear();
        entityManager.flush();

        for (UpdateTelefonoSolicitudRequest req : telefonos) {
            if (!tieneTexto(req.getTelefono())) {
                continue;
            }

            TipoTelefono tipoTelefono = resolverTipoTelefono(req);
            TelefonoPersona telefonoPersona = new TelefonoPersona();
            telefonoPersona.setIdpersona(solicitante.getId());
            telefonoPersona.setIdtipo(tipoTelefono.getId());
            telefonoPersona.setPersona(solicitante);
            telefonoPersona.setTipoTelefono(tipoTelefono);
            telefonoPersona.setTelefono(req.getTelefono().trim());
            // observaciones are stored at SolicitudAtencion level; no per-phone
            // description.trim());
            solicitante.getTelefonos().add(telefonoPersona);
        }
    }

    private void actualizarRemitente(Remision remision, UpdateSolicitudRequest req) {
        Persona remitente = remision.getRemitente();
        if (remitente == null) {
            return;
        }

        if (tieneTexto(req.getRemitentePrimerNombre())) {
            remitente.setPrimerNombre(req.getRemitentePrimerNombre().trim());
        }
        if (req.getRemitenteSegundoNombre() != null) {
            remitente.setSegundoNombre(normalizarTextoOpcional(req.getRemitenteSegundoNombre()));
        }
        if (tieneTexto(req.getRemitentePrimerApellido())) {
            remitente.setPrimerApellido(req.getRemitentePrimerApellido().trim());
        }
        if (req.getRemitenteSegundoApellido() != null) {
            remitente.setSegundoApellido(normalizarTextoOpcional(req.getRemitenteSegundoApellido()));
        }
        if (tieneTexto(req.getRemitenteNumeroDocumento())) {
            remitente.setNumeroDocumento(req.getRemitenteNumeroDocumento().trim());
        }
        if (req.getRemitenteTipoDocumentoId() != null) {
            remitente.setTipoIdentificacion(tipoIdentificacionRepository.findById(req.getRemitenteTipoDocumentoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Tipo de documento no encontrado con ID: " + req.getRemitenteTipoDocumentoId())));
        }
        if (req.getRemitenteCargoId() != null) {
            remision.setCargo(cargoRepository.findById(req.getRemitenteCargoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Cargo no encontrado con ID: " + req.getRemitenteCargoId())));
        }
        if (req.getRemitenteCampusId() != null) {
            remision.setCampus(campusRepository.findById(req.getRemitenteCampusId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Campus no encontrado con ID: " + req.getRemitenteCampusId())));
        }
        if (req.getRemitenteUnidadAdministrativaId() != null) {
            remision.setUnidadAdministrativa(unidadAdministrativaRepository.findById(req.getRemitenteUnidadAdministrativaId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Unidad administrativa no encontrada con ID: " + req.getRemitenteUnidadAdministrativaId())));
        }
        if (req.getRemitenteUnidadAcademicaId() != null) {
            remision.setUnidadAcademica(unidadAcademicaRepository.findById(req.getRemitenteUnidadAcademicaId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Unidad académica no encontrada con ID: " + req.getRemitenteUnidadAcademicaId())));
        }
    }

    private TipoCorreo resolverTipoCorreo(UpdateCorreoSolicitudRequest req) {
        if (req.getTipoId() != null) {
            return tipoCorreoRepository.findById(req.getTipoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Tipo de correo no encontrado con ID: " + req.getTipoId()));
        }

        if (tieneTexto(req.getTipo())) {
            return tipoCorreoRepository.findAll().stream()
                    .filter(tipo -> tipo.getNombre().equalsIgnoreCase(req.getTipo().trim()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Tipo de correo no encontrado: " + req.getTipo()));
        }

        throw new ResourceNotFoundException("El tipo de correo es obligatorio");
    }

    private TipoTelefono resolverTipoTelefono(UpdateTelefonoSolicitudRequest req) {
        if (req.getTipoId() != null) {
            return tipoTelefonoRepository.findById(req.getTipoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Tipo de teléfono no encontrado con ID: " + req.getTipoId()));
        }

        if (tieneTexto(req.getTipo())) {
            String tipoNormalizado = req.getTipo().trim();
            if ("Alterno".equalsIgnoreCase(tipoNormalizado)) {
                tipoNormalizado = TipoTelefonoEnum.FIJO.getNombre();
            }

            final String nombreTipo = tipoNormalizado;
            return tipoTelefonoRepository.findAll().stream()
                    .filter(tipo -> tipo.getNombre().equalsIgnoreCase(nombreTipo))
                    .findFirst()
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Tipo de teléfono no encontrado: " + req.getTipo()));
        }

        throw new ResourceNotFoundException("El tipo de teléfono es obligatorio");
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String normalizarTextoOpcional(String valor) {
        if (!tieneTexto(valor)) {
            return null;
        }
        return valor.trim();
    }

    /**
     * Asigna profesionales a una solicitud de atención y actualiza su estado
     */
    @Transactional
    public SolicitudAcompanamientoResponse asignarSolicitud(Long id, AsignarSolicitudRequest req) {
        log.info("Asignando profesionales a solicitud con ID: {}", id);
        SolicitudAtencion solicitud = solicitudAtencionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con ID: " + id));

        Usuario usuarioAutenticado = obtenerUsuarioAutenticado();

        // Crear asignación (el historial se conserva; la vigente es la más reciente)
        Asignacion asignacion = new Asignacion();
        asignacion.setSolicitudAtencion(solicitud);
        asignacion.setUsuarioCreacion(usuarioAutenticado);
        asignacion.setUsuarioActualizacion(usuarioAutenticado);
        asignacion.setObservaciones(req.getObservaciones().trim());
        asignacion.setFechaReparto(req.getFechaReparto());

        GrupoProfesional grupo = grupoProfesionalRepository.findById(req.getGrupoProfesionalId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Grupo profesional no encontrado con ID: " + req.getGrupoProfesionalId()));
        validarActivo(grupo.getActivo(), "El grupo profesional", grupo.getId());
        asignacion.setGrupoProfesional(grupo);

        TipoAsignacion tipoAsignacion = tipoAsignacionRepository.findById(req.getIdTipoAsignacion())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tipo de asignación no encontrado con ID: " + req.getIdTipoAsignacion()));
        validarActivo(tipoAsignacion.getActivo(), "El tipo de asignación", tipoAsignacion.getId());
        asignacion.setTipoAsignacion(tipoAsignacion);

        TipoServicio tipoServicio = tipoServicioRepository.findById(req.getIdTipoServicio())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tipo de servicio no encontrado con ID: " + req.getIdTipoServicio()));
        validarActivo(tipoServicio.getActivo(), "El tipo de servicio", tipoServicio.getId());
        asignacion.setTipoServicio(tipoServicio);

        asignacionRepository.save(asignacion);
        solicitud.getAsignaciones().add(asignacion);

        // Actualizar estado de la solicitud a ASIGNADA
        solicitud.setEstadoSolicitud(estadoSolicitudRepository.findById(EstadoSolicitud.ASIGNADA.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Estado ASIGNADA no encontrado")));
        if (usuarioAutenticado != null) {
            solicitud.setUsuarioActualizacion(usuarioAutenticado);
        }
        solicitudAtencionRepository.save(solicitud);

        return buildResponse(solicitud, solicitud.getRemision());
    }

    /**
     * Registra un intento de contacto telefonico para una solicitud.
     * Crea una cita automáticamente cuando:
     * - La llamada resulta en "Contesta y se concerta cita" (id=1), o
     * - El número de llamadas alcanza el máximo configurable (parámetro
     * MAX_LLAMADAS_CONTACTO, defecto 2)
     */
    @Transactional
    public ContactoTelefonicoResponse registrarContacto(Long solicitudId, ContactoTelefonicoRequest req) {
        SolicitudAtencion solicitud = solicitudAtencionRepository.findById(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con ID: " + solicitudId));
        Usuario usuarioAutenticado = obtenerUsuarioAutenticado();

        ResultadoContactoTelefonico resultado = resultadoContactoRepository.findAll().stream()
                .filter(r -> r.getNombre().equalsIgnoreCase(req.getResultado()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resultado de contacto no encontrado: " + req.getResultado()));

        // Contar llamadas previas para determinar el número de esta llamada
        long llamadasPrevias = contactoTelefonicoRepository.countBySolicitudAtencionId(solicitudId);
        long numeroDeLlamada = llamadasPrevias + 1;

        ContactoTelefonico contacto = new ContactoTelefonico();
        contacto.setSolicitudAtencion(solicitud);
        contacto.setUsuarioCreacion(usuarioAutenticado);
        contacto.setUsuarioActualizacion(usuarioAutenticado);
        contacto.setFecha(resolverFechaContacto(req.getFecha()));
        contacto.setResultado(resultado);
        contacto.setObservacion(req.getObservacion());
        contactoTelefonicoRepository.save(contacto);

        // Obtener el parámetro configurable de máximo de llamadas (defecto 2)
        int maxLlamadas = 2;
        Optional<ParametroSistema> param = parametroSistemaRepository.findByClave("MAX_LLAMADAS_CONTACTO");
        if (param.isPresent()) {
            try {
                maxLlamadas = Integer.parseInt(param.get().getValor());
            } catch (NumberFormatException ignored) {
            }
        }

        // Determinar si se debe crear una cita:
        // - Primera llamada exitosa ("Contesta y se concerta cita", id=1), o
        // - Número de llamada alcanza el máximo (asignación unilateral)
        boolean esConcertada = resultado.getId() == 1;
        boolean esUnilateral = numeroDeLlamada >= maxLlamadas;
        boolean crearCita = esConcertada || esUnilateral;

        Long citaId = null;
        String fechaCitaStr = null;
        if (crearCita && req.getFechaCita() != null && !req.getFechaCita().isBlank()
                && req.getHoraCita() != null && !req.getHoraCita().isBlank()) {
            log.info("Creando cita para solicitud {} (llamada {}, unilateral={})", solicitudId, numeroDeLlamada,
                    esUnilateral);
            LocalDateTime fechaHoraCita = LocalDateTime.parse(req.getFechaCita() + "T" + req.getHoraCita());
            EstadoCita estadoCreada = estadoCitaRepository.findById(EstadoCitaEnum.CREADA.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Estado de cita 'Creada' no encontrado"));
            Cita cita = new Cita();
            cita.setSolicitudAtencion(solicitud);
            cita.setFecha(fechaHoraCita);
            cita.setEstadoCita(estadoCreada);
            cita.setUsuarioCreacion(usuarioAutenticado);
            cita.setUsuarioActualizacion(usuarioAutenticado);
            cita = citaRepository.save(cita);
            citaId = cita.getId();
            fechaCitaStr = req.getFechaCita() + " " + req.getHoraCita();
        }

        return ContactoTelefonicoResponse.builder()
                .fecha(contacto.getFecha() != null ? contacto.getFecha().toString() : req.getFecha())
                .jornada(calcularJornada(contacto.getFecha()))
                .resultado(resultado.getNombre())
                .observacion(req.getObservacion())
                .citaCreada(citaId != null)
                .citaId(citaId)
                .fechaCita(fechaCitaStr)
                .build();
    }

    /**
     * Lista los contactos telefonicos de una solicitud
     */
    @Transactional(readOnly = true)
    public List<ContactoTelefonicoResponse> listarContactos(Long solicitudId) {
        if (!solicitudAtencionRepository.existsById(solicitudId)) {
            throw new ResourceNotFoundException("Solicitud no encontrada con ID: " + solicitudId);
        }
        return contactoTelefonicoRepository.findBySolicitudAtencionIdOrderByFechaCreacionDesc(solicitudId).stream()
                .map(c -> ContactoTelefonicoResponse.builder()
                        .fecha(c.getFecha() != null ? c.getFecha().toString() : "")
                        .jornada(calcularJornada(c.getFecha()))
                        .resultado(c.getResultado().getNombre())
                        .observacion(c.getObservacion() != null ? c.getObservacion() : "")
                        .build())
                .collect(Collectors.toList());
    }

    private void validarActivo(Boolean activo, String entidad, Integer id) {
        if (!Boolean.TRUE.equals(activo)) {
            throw new IllegalArgumentException(entidad + " con ID " + id + " no está activo");
        }
    }

    /**
     * Lista los grupos profesionales activos del sistema
     */
    @Transactional(readOnly = true)
    public List<ProfesionalResponse> listarGruposProfesionales() {
        log.info("Listando grupos profesionales activos");
        return grupoProfesionalRepository.findByActivoTrueOrderByNombreAsc().stream()
                .map(g -> ProfesionalResponse.builder()
                        .id(g.getId().longValue())
                        .nombre(g.getNombre())
                        .activo(g.getActivo())
                        .idTipoServicio(g.getTipoServicio().getId())
                        .build())
                .collect(Collectors.toList());
    }

    private String calcularJornada(LocalDateTime fechaHora) {
        if (fechaHora == null) {
            return "";
        }

        return fechaHora.getHour() < 12 ? "Mañana" : "Tarde";
    }

    private LocalDateTime resolverFechaContacto(String fechaTexto) {
        if (fechaTexto == null || fechaTexto.isBlank()) {
            return LocalDateTime.now();
        }

        String valor = fechaTexto.trim();
        try {
            return LocalDateTime.parse(valor);
        } catch (Exception ex) {
            try {
                return LocalDateTime.parse(valor + "T00:00:00");
            } catch (Exception ex2) {
                log.warn("No fue posible parsear la fecha de contacto '{}', se usará fecha actual", fechaTexto);
                return LocalDateTime.now();
            }
        }
    }

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
}
