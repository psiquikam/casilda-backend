package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.request.CancelarCitaRequest;
import co.edu.udea.casilda.dto.request.ProgramarCitaSeguimientoRequest;
import co.edu.udea.casilda.dto.request.ReprogramarCitaRequest;
import co.edu.udea.casilda.dto.request.RegistrarResultadoCitaRequest;
import co.edu.udea.casilda.dto.response.CitaResponse;
import co.edu.udea.casilda.exception.ResourceNotFoundException;
import co.edu.udea.casilda.model.entity.*;
import co.edu.udea.casilda.model.enums.TipoCorreoEnum;
import co.edu.udea.casilda.model.enums.TipoTelefonoEnum;
import co.edu.udea.casilda.model.enums.EstadoCitaEnum;
import co.edu.udea.casilda.repository.CasoRepository;
import co.edu.udea.casilda.repository.CitaRepository;
import co.edu.udea.casilda.repository.EstadoCitaRepository;
import co.edu.udea.casilda.repository.MotivoEstadoCitaRepository;
import co.edu.udea.casilda.repository.UsuarioRepository;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de citas de atención
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CitaService {

    private final CitaRepository citaRepository;
    private final CasoRepository casoRepository;
    private final EstadoCitaRepository estadoCitaRepository;
    private final MotivoEstadoCitaRepository motivoEstadoCitaRepository;
    private final UsuarioRepository usuarioRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Transactional(readOnly = true)
    public List<CitaResponse> listarTodasLasCitas() {
        log.info("Listando todas las citas");
        return citaRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<CitaResponse> listarCitasPaginadas(int page, int size, Integer idEstadoCita, Integer excluirEstadoCitaId) {
        log.info("Listando citas paginadas. page={}, size={}, idEstadoCita={}, excluirEstadoCitaId={}",
                page, size, idEstadoCita, excluirEstadoCitaId);

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Integer estadoExcluido = idEstadoCita == null ? excluirEstadoCitaId : null;
        Page<Cita> citas = citaRepository.findParaTabla(
                idEstadoCita,
                estadoExcluido,
                List.of(EstadoCitaEnum.PROGRAMADA.getId(), EstadoCitaEnum.REPROGRAMADA.getId()),
                EstadoCitaEnum.ATENDIDA.getId(),
                pageable);

        return citas.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<CitaResponse> listarCitasPendientesDeCaso(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        List<Integer> estadosElegibles = List.of(
                EstadoCitaEnum.PROGRAMADA.getId(),
                EstadoCitaEnum.REPROGRAMADA.getId(),
                EstadoCitaEnum.ATENDIDA.getId());
        return citaRepository.findPendientesDeCaso(estadosElegibles, PageRequest.of(safePage, safeSize))
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<CitaResponse> listarHistorialAtenciones(Long casoId) {
        Caso caso = casoRepository.findById(casoId)
                .orElseThrow(() -> new ResourceNotFoundException("Caso no encontrado con ID: " + casoId));
        Long citaInicialId = caso.getCita().getId();
        return citaRepository
                .findBySolicitudAtencionIdOrderByFechaDesc(caso.getCita().getSolicitudAtencion().getId())
                .stream()
                .filter(cita -> cita.getId() >= citaInicialId)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CitaResponse registrarResultado(Long id, RegistrarResultadoCitaRequest req) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + id));
        validarCitaPendiente(cita);
        if (cita.getFecha() == null || cita.getFecha().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("El resultado solo puede registrarse a partir de la fecha y hora de la cita.");
        }

        EstadoCitaEnum resultado = switch (req.getResultado()) {
            case "ATENDIDA" -> EstadoCitaEnum.ATENDIDA;
            case "INASISTENCIA" -> EstadoCitaEnum.INASISTENCIA;
            default -> throw new IllegalArgumentException("Resultado de cita no válido.");
        };
        EstadoCita estado = estadoCitaRepository.findById(resultado.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estado de cita '" + resultado.getNombre() + "' no encontrado"));
        cita.setEstadoCita(estado);
        if (req.getObservaciones() != null) {
            cita.setObservaciones(req.getObservaciones().trim());
        }
        Usuario usuarioAutenticado = obtenerUsuarioAutenticado();
        if (usuarioAutenticado != null) {
            cita.setUsuarioActualizacion(usuarioAutenticado);
        }
        return mapToResponse(citaRepository.save(cita));
    }

    @Transactional
    public CitaResponse programarSeguimiento(Long casoId, ProgramarCitaSeguimientoRequest req) {
        Caso caso = casoRepository.findById(casoId)
                .orElseThrow(() -> new ResourceNotFoundException("Caso no encontrado con ID: " + casoId));

        LocalDateTime fechaHora;
        try {
            fechaHora = LocalDateTime.of(LocalDate.parse(req.getFechaCita()), LocalTime.parse(req.getHoraCita()));
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("La fecha u hora de la cita no tienen un formato válido.", error);
        }
        if (!fechaHora.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("La cita de seguimiento debe programarse en una fecha futura.");
        }

        EstadoCita estadoProgramada = estadoCitaRepository.findById(EstadoCitaEnum.PROGRAMADA.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Estado de cita 'Programada' no encontrado"));
        Usuario usuarioAutenticado = obtenerUsuarioAutenticado();

        Cita cita = new Cita();
        cita.setSolicitudAtencion(caso.getCita().getSolicitudAtencion());
        cita.setFecha(fechaHora);
        cita.setEstadoCita(estadoProgramada);
        cita.setObservaciones("Cita de seguimiento del caso " + caso.getCodigo());
        cita.setUsuarioCreacion(usuarioAutenticado);
        cita.setUsuarioActualizacion(usuarioAutenticado);
        return mapToResponse(citaRepository.save(cita));
    }

    @Transactional
    public CitaResponse reprogramar(Long id, ReprogramarCitaRequest req) {
        log.info("Reprogramando cita id={}", id);
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + id));
        validarCitaPendiente(cita);
        LocalDateTime nuevaFecha;
        try {
            nuevaFecha = LocalDateTime.parse(req.getFechaCita() + "T" + req.getHoraCita());
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("La fecha u hora de la cita no tienen un formato válido.", error);
        }
        if (!nuevaFecha.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("La nueva fecha de la cita debe ser futura.");
        }
        cita.setFecha(nuevaFecha);
        EstadoCita estadoReprogramada = estadoCitaRepository.findById(EstadoCitaEnum.REPROGRAMADA.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Estado de cita 'Reprogramada' no encontrado"));
        cita.setEstadoCita(estadoReprogramada);
        if (req.getIdMotivoEstadoCita() != null) {
            MotivoEstadoCita motivo = motivoEstadoCitaRepository.findById(req.getIdMotivoEstadoCita())
                    .orElseThrow(() -> new ResourceNotFoundException("Motivo de estado de cita no encontrado con ID: " + req.getIdMotivoEstadoCita()));
            cita.setMotivoEstadoCita(motivo);
        }
        cita.setObservaciones(req.getObservaciones());
        Usuario usuarioAutenticado = obtenerUsuarioAutenticado();
        if (usuarioAutenticado != null) {
            cita.setUsuarioActualizacion(usuarioAutenticado);
        }
        return mapToResponse(citaRepository.save(cita));
    }

    @Transactional
    public CitaResponse cancelar(Long id, CancelarCitaRequest req) {
        log.info("Cancelando cita id={}", id);
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + id));
        validarCitaPendiente(cita);
        EstadoCita estadoCancelada = estadoCitaRepository.findById(EstadoCitaEnum.CANCELADA.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Estado de cita 'Cancelada' no encontrado"));
        cita.setEstadoCita(estadoCancelada);
        if (req.getIdMotivoEstadoCita() != null) {
            MotivoEstadoCita motivo = motivoEstadoCitaRepository.findById(req.getIdMotivoEstadoCita())
                    .orElseThrow(() -> new ResourceNotFoundException("Motivo de estado de cita no encontrado con ID: " + req.getIdMotivoEstadoCita()));
            cita.setMotivoEstadoCita(motivo);
        }
        cita.setObservaciones(req.getObservaciones());
        Usuario usuarioAutenticado = obtenerUsuarioAutenticado();
        if (usuarioAutenticado != null) {
            cita.setUsuarioActualizacion(usuarioAutenticado);
        }
        return mapToResponse(citaRepository.save(cita));
    }

    private void validarCitaPendiente(Cita cita) {
        Integer estadoId = cita.getEstadoCita() != null ? cita.getEstadoCita().getId() : null;
        if (!EstadoCitaEnum.PROGRAMADA.getId().equals(estadoId)
                && !EstadoCitaEnum.REPROGRAMADA.getId().equals(estadoId)) {
            throw new IllegalArgumentException("Solo se pueden gestionar citas programadas o reprogramadas.");
        }
    }

    // ─── Mapeo ──────────────────────────────────────────────────────────────────

    private CitaResponse mapToResponse(Cita cita) {
        SolicitudAtencion sa = cita.getSolicitudAtencion();
        Caso caso = casoRepository.findFirstByCitaSolicitudAtencionIdOrderByFechaCreacionAsc(sa.getId())
                .orElse(null);
        boolean perteneceAlCaso = caso != null && cita.getId() >= caso.getCita().getId();
        Remision remision = sa.getRemision();
        // La persona es el solicitante de la solicitud; para solicitudes indirectas también existe el remitente
        Persona persona = sa.getSolicitante();
        if (persona == null) {
            // Fallback: intentar el remitente de la remisión
            persona = remision != null ? remision.getRemitente() : null;
        }
        if (persona == null) {
            throw new IllegalArgumentException("No se encontró persona para la solicitud de atención ID: " + sa.getId());
        }

        // Correos
        String correoInstitucional = "";
        String correoPersonal = "";
        if (persona.getCorreos() != null) {
            for (CorreoPersona cp : persona.getCorreos()) {
                if (cp.getTipoCorreo() == null || cp.getCorreo() == null) continue;
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

        // Teléfonos
        String celular = "";
        String telefonoAlterno = "";
        if (persona.getTelefonos() != null) {
            for (TelefonoPersona tp : persona.getTelefonos()) {
                if (tp.getTipoTelefono() == null || tp.getTelefono() == null) continue;
                Integer idTipo = tp.getTipoTelefono().getId();
                if ((TipoTelefonoEnum.CELULAR.getId().equals(idTipo) || TipoTelefonoEnum.WHATSAPP.getId().equals(idTipo))
                        && celular.isEmpty()) {
                    celular = tp.getTelefono();
                } else if ((TipoTelefonoEnum.FIJO.getId().equals(idTipo) || TipoTelefonoEnum.OFICINA.getId().equals(idTipo))
                        && telefonoAlterno.isEmpty()) {
                    telefonoAlterno = tp.getTelefono();
                } else if (celular.isEmpty()) {
                    celular = tp.getTelefono();
                } else if (telefonoAlterno.isEmpty()) {
                    telefonoAlterno = tp.getTelefono();
                }
            }
        }

        // Para solicitudes indirectas, los datos de ubicación vienen de la remisión
        boolean esIndirecta = remision != null;

        String unidadAdministrativaNombre = esIndirecta
                ? (remision.getUnidadAdministrativa() != null ? remision.getUnidadAdministrativa().getNombre() : null)
                : null;
        String unidadAcademicaNombre = esIndirecta
                ? (remision.getUnidadAcademica() != null ? remision.getUnidadAcademica().getNombre() : null)
                : null;
        String campusNombre = esIndirecta
                ? (remision.getCampus() != null ? remision.getCampus().getNombre() : null)
                : null;

        String profesionalNombre = "Sin asignar";
        String tipoAsignacionNombre = "Sin asignar";
        if (sa.getAsignaciones() != null && !sa.getAsignaciones().isEmpty()) {
            Asignacion ultimaAsignacion = sa.getAsignaciones().get(sa.getAsignaciones().size() - 1);
            if (ultimaAsignacion.getGrupoProfesional() != null) {
                profesionalNombre = ultimaAsignacion.getGrupoProfesional().getNombre();
            }
            if (ultimaAsignacion.getTipoAsignacion() != null) {
                tipoAsignacionNombre = ultimaAsignacion.getTipoAsignacion().getNombre();
            }
        }

        return CitaResponse.builder()
                .id(cita.getId())
                .solicitudId(sa.getId())
                .codigoSolicitud(sa.getId().toString())
                .casoId(perteneceAlCaso ? caso.getId() : null)
                .codigoCaso(perteneceAlCaso ? caso.getCodigo() : null)
                .solicitudTieneCaso(caso != null)
                .tipoAtencion(perteneceAlCaso && !cita.getId().equals(caso.getCita().getId())
                        ? "SEGUIMIENTO" : "INICIAL")
                .nombreSolicitante(persona.getNombreCompleto().trim())
                .tipoDocumento(persona.getTipoIdentificacion() != null ? persona.getTipoIdentificacion().getNombre() : null)
                .documento(persona.getNumeroDocumento())
                .fechaCita(cita.getFecha() != null ? cita.getFecha().format(FORMATTER) : null)
                .idEstadoCita(cita.getEstadoCita() != null ? cita.getEstadoCita().getId() : null)
                .estadoCita(cita.getEstadoCita() != null ? cita.getEstadoCita().getNombre() : null)
                .motivoEstadoCita(cita.getMotivoEstadoCita() != null ? cita.getMotivoEstadoCita().getNombre() : null)
                .observaciones(cita.getObservaciones())
                .tipoSolicitud(sa.getTipoSolicitud() != null ? sa.getTipoSolicitud().getNombre() : null)
                .unidadAdministrativa(unidadAdministrativaNombre)
                .profesional(profesionalNombre)
                .tipoAsignacion(tipoAsignacionNombre)
                .unidadAcademica(unidadAcademicaNombre)
                .campus(campusNombre)
                .identidadGenero(sa.getIdentidadGenero() != null ? sa.getIdentidadGenero().getNombre() : null)
                .celular(celular)
                .telefonoAlterno(telefonoAlterno)
                .correoInstitucional(correoInstitucional)
                .correoPersonal(correoPersonal)
                .build();
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
