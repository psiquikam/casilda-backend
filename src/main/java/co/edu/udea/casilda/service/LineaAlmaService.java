package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.request.AtencionAphRequest;
import co.edu.udea.casilda.dto.request.ContactoLineaAlmaRequest;
import co.edu.udea.casilda.dto.request.CorreoSolicitanteRequest;
import co.edu.udea.casilda.dto.request.RemisionRegistroAlmaRequest;
import co.edu.udea.casilda.dto.request.TelefonoSolicitanteRequest;
import co.edu.udea.casilda.dto.request.DiscapacidadPersonaRequest;
import co.edu.udea.casilda.dto.request.Pestana0LineaAlmaRequest;
import co.edu.udea.casilda.dto.request.Pestana1LineaAlmaRequest;
import co.edu.udea.casilda.dto.request.Pestana2LineaAlmaRequest;
import co.edu.udea.casilda.dto.request.Pestana3LineaAlmaRequest;
import co.edu.udea.casilda.dto.request.Pestana4LineaAlmaRequest;
import co.edu.udea.casilda.dto.request.Pestana5LineaAlmaRequest;
import co.edu.udea.casilda.dto.response.AtencionAphResponse;
import co.edu.udea.casilda.dto.response.ContactoLineaAlmaResponse;
import co.edu.udea.casilda.dto.response.RegistroLineaAlmaResponse;
import co.edu.udea.casilda.dto.response.RemisionRegistroAlmaResponse;
import co.edu.udea.casilda.exception.ResourceNotFoundException;
import co.edu.udea.casilda.model.entity.*;
import co.edu.udea.casilda.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LineaAlmaService {

    private final RegistroLineaAlmaRepository registroLineaAlmaRepository;
    private final AtencionAphRepository atencionAphRepository;
    private final RemisionRegistroAlmaRepository remisionRegistroAlmaRepository;
    private final ContactoLineaAlmaRepository contactoLineaAlmaRepository;

    private final PersonaRepository personaRepository;
    private final SexoRepository sexoRepository;
    private final CorreoPersonaRepository correoPersonaRepository;
    private final TelefonoPersonaRepository telefonoPersonaRepository;
    private final TipoCorreoRepository tipoCorreoRepository;
    private final TipoTelefonoRepository tipoTelefonoRepository;
    private final SubTipoDiscapacidadRepository subTipoDiscapacidadRepository;
    private final TipoReporteAlmaRepository tipoReporteAlmaRepository;
    private final CanalContactoRepository canalContactoRepository;
    private final UsuarioRepository usuarioRepository;
    private final GrupoProfesionalRepository grupoProfesionalRepository;
    private final TipoServicioRepository tipoServicioRepository;
    private final IdentidadGeneroRepository identidadGeneroRepository;
    private final TipoIdentificacionRepository tipoIdentificacionRepository;
    private final OrientacionSexualRepository orientacionSexualRepository;
    private final EtniaRepository etniaRepository;
    private final MunicipioRepository municipioRepository;
    private final VinculoUdeARepository vinculoUdeARepository;
    private final UnidadAcademicaRepository unidadAcademicaRepository;
    private final ProgramaRepository programaRepository;
    private final UnidadAdministrativaRepository unidadAdministrativaRepository;
    private final CampusRepository campusRepository;
    private final ProtocoloAphRepository protocoloAphRepository;
    private final ResultadoTriageRepository resultadoTriageRepository;
    private final TipoRemisionRepository tipoRemisionRepository;
    private final ResultadoContactoTelefonicoRepository resultadoContactoTelefonicoRepository;
    private final ActorRemitenteRepository actorRemitenteRepository;

    @Transactional
    public RegistroLineaAlmaResponse registrarPestana0(Pestana0LineaAlmaRequest request) {
        log.info("Registrando pestaña 0 para registro ID: {}", request.getId());
        Usuario usuarioAutenticado = obtenerUsuarioAutenticado();

        RegistroLineaAlma registro;
        if (request.getId() != null) {
            registro = registroLineaAlmaRepository.findById(request.getId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Registro no encontrado con ID: " + request.getId()));
            registro.setUsuarioActualizacion(usuarioAutenticado);
        } else {
            registro = new RegistroLineaAlma();
            registro.setFechaHoraAtencion(
                    request.getFechaHoraAtencion() != null ? request.getFechaHoraAtencion() : LocalDateTime.now());
            registro.setUsuarioCreacion(usuarioAutenticado);
        }

        registro.setTipoReporte(tipoReporteAlmaRepository.findById(request.getIdTipoReporte())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tipo reporte ALMA no encontrado con ID: " + request.getIdTipoReporte())));
        registro.setCanalContacto(canalContactoRepository.findById(request.getIdCanalContacto())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Canal de contacto no encontrado con ID: " + request.getIdCanalContacto())));
        if (request.getIdQuienRemite() != null) {
            registro.setQuienRemite(actorRemitenteRepository.findById(request.getIdQuienRemite())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Actor remitente no encontrado con ID: " + request.getIdQuienRemite())));
        } else {
            registro.setQuienRemite(null);
        }
        registro.setPersonaAtiende(grupoProfesionalRepository.findById(request.getIdPersonaAtiende().intValue())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Grupo profesional no encontrado con ID: " + request.getIdPersonaAtiende())));
        registro.setTipoServicio(tipoServicioRepository.findByIdAndActivoTrue(request.getIdTipoServicio())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tipo de servicio activo no encontrado con ID: " + request.getIdTipoServicio())));
        registro.setPersonaRegistra(usuarioRepository.findById(request.getIdPersonaRegistra())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario registra no encontrado con ID: " + request.getIdPersonaRegistra())));

        registro = registroLineaAlmaRepository.save(registro);
        return obtenerPorId(registro.getId());
    }

    @Transactional
    public RegistroLineaAlmaResponse registrarPestana1(Pestana1LineaAlmaRequest request) {
        log.info("Registrando pestaña 1 para registro ID: {}", request.getId());
        Usuario usuarioAutenticado = obtenerUsuarioAutenticado();

        Persona persona = null;
        if (request.getIdPersona() != null && request.getIdPersona() > 0) {
            persona = personaRepository.findById(request.getIdPersona()).orElse(null);
        }
        if (persona == null && request.getNumeroDocumento() != null && !request.getNumeroDocumento().isBlank()) {
            persona = personaRepository.findByNumeroDocumento(request.getNumeroDocumento().trim()).orElse(null);
        }

        boolean tieneDatosPersonales = (request.getPrimerNombre() != null && !request.getPrimerNombre().isBlank()) ||
                (request.getNumeroDocumento() != null && !request.getNumeroDocumento().isBlank());
        if (persona == null && tieneDatosPersonales) {
            persona = new Persona();
            persona.setPrimerNombre(request.getPrimerNombre().trim());
            persona.setPrimerApellido(
                    request.getPrimerApellido() != null ? request.getPrimerApellido().trim() : "Temp");
            persona.setNumeroDocumento(
                    request.getNumeroDocumento() != null ? request.getNumeroDocumento().trim() : "0");
            persona.setTipoIdentificacion(request.getIdTipoIdentificacion() != null
                    ? tipoIdentificacionRepository.findById(request.getIdTipoIdentificacion()).orElse(null)
                    : tipoIdentificacionRepository.findAll().stream().findFirst().orElse(null));
            persona.setCiudadNacimiento(request.getIdCiudadNacimiento() != null
                    ? municipioRepository.findById(request.getIdCiudadNacimiento()).orElse(null)
                    : municipioRepository.findAll().stream().findFirst().orElse(null));
            if (request.getIdSexo() != null) {
                persona.setSexo(sexoRepository.findById(request.getIdSexo()).orElse(null));
            }
            persona = personaRepository.save(persona);
        }

        if (persona != null) {
            if (request.getPrimerNombre() != null && !request.getPrimerNombre().isBlank()) {
                persona.setPrimerNombre(request.getPrimerNombre().trim());
            }
            if (request.getSegundoNombre() != null) {
                persona.setSegundoNombre(request.getSegundoNombre().trim());
            }
            if (request.getPrimerApellido() != null && !request.getPrimerApellido().isBlank()) {
                persona.setPrimerApellido(request.getPrimerApellido().trim());
            }
            if (request.getSegundoApellido() != null) {
                persona.setSegundoApellido(request.getSegundoApellido().trim());
            }
            if (request.getNumeroDocumento() != null && !request.getNumeroDocumento().isBlank()) {
                persona.setNumeroDocumento(request.getNumeroDocumento().trim());
            }
            if (request.getIdTipoIdentificacion() != null) {
                persona.setTipoIdentificacion(
                        tipoIdentificacionRepository.findById(request.getIdTipoIdentificacion()).orElse(null));
            }
            if (request.getIdCiudadNacimiento() != null) {
                persona.setCiudadNacimiento(municipioRepository.findById(request.getIdCiudadNacimiento()).orElse(null));
            }
            if (request.getIdSexo() != null) {
                persona.setSexo(sexoRepository.findById(request.getIdSexo()).orElse(null));
            }

            // Update disabilities
            persona.getDiscapacidades().clear();
            if (request.getDiscapacidades() != null && !request.getDiscapacidades().isEmpty()) {
                for (DiscapacidadPersonaRequest req : request.getDiscapacidades()) {
                    if (req.getIdSubTipoDiscapacidad() == null) continue;
                    SubTipoDiscapacidad subTipo = subTipoDiscapacidadRepository.findById(req.getIdSubTipoDiscapacidad()).orElse(null);
                    if (subTipo == null) continue;
                    DiscapacidadPersona dp = new DiscapacidadPersona();
                    dp.setIdpersona(persona.getId());
                    dp.setIdsubtipodiscapacidad(subTipo.getId());
                    dp.setPersona(persona);
                    dp.setSubTipoDiscapacidad(subTipo);
                    dp.setDescripcion(req.getDescripcion() != null ? req.getDescripcion().trim() : "");
                    persona.getDiscapacidades().add(dp);
                }
            }

            personaRepository.save(persona);
        }

        RegistroLineaAlma registro = registroLineaAlmaRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Registro no encontrado con ID: " + request.getId()));
        if (persona != null) {
            registro.setPersona(persona);
        }
        registro.setUsuarioActualizacion(usuarioAutenticado);

        if (request.getIdIdentidadGenero() != null) {
            registro.setIdentidadGenero(identidadGeneroRepository.findById(request.getIdIdentidadGenero())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Identidad de género no encontrada con ID: " + request.getIdIdentidadGenero())));
        } else {
            registro.setIdentidadGenero(null);
        }
        if (request.getIdOrientacionSexual() != null) {
            registro.setOrientacionSexual(
                    orientacionSexualRepository.findById(request.getIdOrientacionSexual()).orElse(null));
        } else {
            registro.setOrientacionSexual(null);
        }
        if (request.getIdEtnia() != null) {
            registro.setEtnia(etniaRepository.findById(request.getIdEtnia()).orElse(null));
        } else {
            registro.setEtnia(null);
        }
        if (request.getIdCiudadResidencia() != null) {
            registro.setCiudadResidencia(municipioRepository.findById(request.getIdCiudadResidencia()).orElse(null));
        } else {
            registro.setCiudadResidencia(null);
        }
        registro.setDireccionResidencia(request.getDireccionResidencia());

        if (request.getFechaNacimiento() != null) {
            Persona pers = registro.getPersona();
            if (pers != null) {
                pers.setFechaNacimiento(request.getFechaNacimiento());
                personaRepository.save(pers);
            }
        }

        registro = registroLineaAlmaRepository.save(registro);
        return obtenerPorId(registro.getId());
    }

    @Transactional
    public RegistroLineaAlmaResponse registrarPestana2(Pestana2LineaAlmaRequest request) {
        log.info("Registrando pestaña 2 para registro ID: {}", request.getId());
        Usuario usuarioAutenticado = obtenerUsuarioAutenticado();

        RegistroLineaAlma registro = registroLineaAlmaRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Registro no encontrado con ID: " + request.getId()));
        registro.setUsuarioActualizacion(usuarioAutenticado);

        if (request.getIdVinculoUdeA() != null) {
            registro.setVinculoUdeA(vinculoUdeARepository.findById(request.getIdVinculoUdeA()).orElse(null));
        } else {
            registro.setVinculoUdeA(null);
        }
        if (request.getIdUnidadAcademica() != null) {
            registro.setUnidadAcademica(
                    unidadAcademicaRepository.findById(request.getIdUnidadAcademica()).orElse(null));
        } else {
            registro.setUnidadAcademica(null);
        }
        if (request.getIdPrograma() != null) {
            registro.setPrograma(programaRepository.findById(request.getIdPrograma()).orElse(null));
        } else {
            registro.setPrograma(null);
        }
        if (request.getIdUnidadAdministrativa() != null) {
            registro.setUnidadAdministrativa(
                    unidadAdministrativaRepository.findById(request.getIdUnidadAdministrativa()).orElse(null));
        } else {
            registro.setUnidadAdministrativa(null);
        }
        if (request.getIdCampus() != null) {
            registro.setCampus(campusRepository.findById(request.getIdCampus()).orElse(null));
        } else {
            registro.setCampus(null);
        }

        Persona persona = registro.getPersona();
        if (persona != null) {
            // Update correos
            List<CorreoPersona> existingCorreos = correoPersonaRepository.findByIdpersona(persona.getId());
            correoPersonaRepository.deleteAll(existingCorreos);

            if (request.getCorreos() != null && !request.getCorreos().isEmpty()) {
                List<TipoCorreo> tiposCorreo = tipoCorreoRepository.findAll();
                for (CorreoSolicitanteRequest req : request.getCorreos()) {
                    if (req.getCorreo() == null || req.getCorreo().isBlank())
                        continue;
                    TipoCorreo tipo = tiposCorreo.stream()
                            .filter(t -> t.getId().equals(req.getTipoId()))
                            .findFirst()
                            .orElse(tiposCorreo.stream().findFirst().orElse(null));
                    if (tipo == null)
                        continue;
                    CorreoPersona correo = new CorreoPersona();
                    correo.setIdpersona(persona.getId());
                    correo.setIdtipo(tipo.getId());
                    correo.setPersona(persona);
                    correo.setTipoCorreo(tipo);
                    correo.setCorreo(req.getCorreo());
                    correoPersonaRepository.save(correo);
                }
            }

            // Update telefonos
            List<TelefonoPersona> existingTelefonos = telefonoPersonaRepository.findByIdpersona(persona.getId());
            telefonoPersonaRepository.deleteAll(existingTelefonos);

            if (request.getTelefonos() != null && !request.getTelefonos().isEmpty()) {
                List<TipoTelefono> tiposTelefono = tipoTelefonoRepository.findAll();
                for (TelefonoSolicitanteRequest req : request.getTelefonos()) {
                    if (req.getTelefono() == null || req.getTelefono().isBlank())
                        continue;
                    TipoTelefono tipo = tiposTelefono.stream()
                            .filter(t -> t.getId().equals(req.getTipoId()))
                            .findFirst()
                            .orElse(tiposTelefono.stream().findFirst().orElse(null));
                    if (tipo == null)
                        continue;
                    TelefonoPersona telefono = new TelefonoPersona();
                    telefono.setIdpersona(persona.getId());
                    telefono.setIdtipo(tipo.getId());
                    telefono.setPersona(persona);
                    telefono.setTipoTelefono(tipo);
                    telefono.setTelefono(req.getTelefono());
                    telefonoPersonaRepository.save(telefono);
                }
            }
        }

        registro.setObservacionesCorreo(request.getObservacionesCorreo());
        registro.setObservacionesTelefono(request.getObservacionesTelefono());

        registro = registroLineaAlmaRepository.save(registro);
        return obtenerPorId(registro.getId());
    }

    @Transactional
    public RegistroLineaAlmaResponse registrarPestana3(Pestana3LineaAlmaRequest request) {
        log.info("Registrando pestaña 3 para registro ID: {}", request.getId());
        Usuario usuarioAutenticado = obtenerUsuarioAutenticado();

        RegistroLineaAlma registro = registroLineaAlmaRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Registro no encontrado con ID: " + request.getId()));
        registro.setUsuarioActualizacion(usuarioAutenticado);

        if (request.getAtencionAph() != null) {
            atencionAphRepository.save(construirAtencionAph(registro, request.getAtencionAph(), usuarioAutenticado));
        }

        registro = registroLineaAlmaRepository.save(registro);
        return obtenerPorId(registro.getId());
    }

    @Transactional
    public RegistroLineaAlmaResponse registrarPestana4(Pestana4LineaAlmaRequest request) {
        log.info("Registrando pestaña 4 para registro ID: {}", request.getId());
        Usuario usuarioAutenticado = obtenerUsuarioAutenticado();

        RegistroLineaAlma registro = registroLineaAlmaRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Registro no encontrado con ID: " + request.getId()));
        registro.setUsuarioActualizacion(usuarioAutenticado);

        List<ContactoLineaAlma> existingContactos = contactoLineaAlmaRepository
                .findByRegistroLineaAlmaIdOrderByFechaCreacionDesc(registro.getId());
        contactoLineaAlmaRepository.deleteAll(existingContactos);

        if (request.getContactos() != null && !request.getContactos().isEmpty()) {
            for (ContactoLineaAlmaRequest cReq : request.getContactos()) {
                ContactoLineaAlma contacto = new ContactoLineaAlma();
                contacto.setRegistroLineaAlma(registro);
                contacto.setFecha(cReq.getFecha() != null ? cReq.getFecha() : LocalDateTime.now());
                contacto.setResultado(resultadoContactoTelefonicoRepository.findById(cReq.getIdResultado())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Resultado contacto no encontrado con ID: " + cReq.getIdResultado())));
                contacto.setUsuarioCreacion(usuarioAutenticado);
                contacto.setUsuarioActualizacion(usuarioAutenticado);
                contactoLineaAlmaRepository.save(contacto);
            }
        }

        registro = registroLineaAlmaRepository.save(registro);
        return obtenerPorId(registro.getId());
    }

    @Transactional
    public RegistroLineaAlmaResponse registrarPestana5(Pestana5LineaAlmaRequest request) {
        log.info("Registrando pestaña 5 para registro ID: {}", request.getId());
        Usuario usuarioAutenticado = obtenerUsuarioAutenticado();

        RegistroLineaAlma registro = registroLineaAlmaRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Registro no encontrado con ID: " + request.getId()));
        registro.setUsuarioActualizacion(usuarioAutenticado);

        List<RemisionRegistroAlma> existingRemisiones = remisionRegistroAlmaRepository
                .findByIdregistrolinealmaOrderByFechaDesc(registro.getId());
        remisionRegistroAlmaRepository.deleteAll(existingRemisiones);

        if (request.getRemisiones() != null && !request.getRemisiones().isEmpty()) {
            for (RemisionRegistroAlmaRequest remisionRequest : request.getRemisiones()) {
                RemisionRegistroAlma remision = new RemisionRegistroAlma();
                remision.setIdregistrolinealma(registro.getId());
                remision.setIdtiporemision(remisionRequest.getIdTipoRemision());
                remision.setRegistroLineaAlma(registro);
                remision.setTipoRemision(tipoRemisionRepository.findById(remisionRequest.getIdTipoRemision())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Tipo remisión no encontrado con ID: " + remisionRequest.getIdTipoRemision())));
                remision.setCual(remisionRequest.getCual());
                remision.setFecha(remisionRequest.getFecha());
                remisionRegistroAlmaRepository.save(remision);
            }
        }

        registro = registroLineaAlmaRepository.save(registro);
        return obtenerPorId(registro.getId());
    }

    @Transactional(readOnly = true)
    public RegistroLineaAlmaResponse obtenerPorId(Long id) {
        RegistroLineaAlma registro = registroLineaAlmaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro Línea ALMA no encontrado con ID: " + id));
        AtencionAph atencionAph = atencionAphRepository.findByRegistroLineaAlmaId(id).orElse(null);
        List<RemisionRegistroAlma> remisiones = remisionRegistroAlmaRepository
                .findByIdregistrolinealmaOrderByFechaDesc(id);
        return mapToResponse(registro, atencionAph, remisiones);
    }

    @Transactional(readOnly = true)
    public List<RegistroLineaAlmaResponse> listarRegistros() {
        List<RegistroLineaAlmaResponse> response = new ArrayList<>();
        for (RegistroLineaAlma registro : registroLineaAlmaRepository.findAllByOrderByFechaCreacionDesc()) {
            AtencionAph atencionAph = atencionAphRepository.findByRegistroLineaAlmaId(registro.getId()).orElse(null);
            List<RemisionRegistroAlma> remisiones = remisionRegistroAlmaRepository
                    .findByIdregistrolinealmaOrderByFechaDesc(registro.getId());
            response.add(mapToResponse(registro, atencionAph, remisiones));
        }
        return response;
    }

    @Transactional
    public ContactoLineaAlmaResponse registrarContacto(Long idRegistro, ContactoLineaAlmaRequest request) {
        RegistroLineaAlma registro = registroLineaAlmaRepository.findById(idRegistro)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Registro Línea ALMA no encontrado con ID: " + idRegistro));

        Usuario usuarioAutenticado = obtenerUsuarioAutenticado();

        ContactoLineaAlma contacto = new ContactoLineaAlma();
        contacto.setRegistroLineaAlma(registro);
        contacto.setFecha(request.getFecha() != null ? request.getFecha() : LocalDateTime.now());
        contacto.setResultado(resultadoContactoTelefonicoRepository.findById(request.getIdResultado())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resultado contacto no encontrado con ID: " + request.getIdResultado())));
        contacto.setUsuarioCreacion(usuarioAutenticado);
        contacto.setUsuarioActualizacion(usuarioAutenticado);

        contacto = contactoLineaAlmaRepository.save(contacto);
        return mapContacto(contacto);
    }

    @Transactional(readOnly = true)
    public List<ContactoLineaAlmaResponse> listarContactos(Long idRegistro) {
        registroLineaAlmaRepository.findById(idRegistro)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Registro Línea ALMA no encontrado con ID: " + idRegistro));
        return contactoLineaAlmaRepository.findByRegistroLineaAlmaIdOrderByFechaCreacionDesc(idRegistro)
                .stream()
                .map(this::mapContacto)
                .toList();
    }

    private AtencionAph construirAtencionAph(RegistroLineaAlma registro, AtencionAphRequest request,
            Usuario usuarioAutenticado) {
        AtencionAph atencionAph = atencionAphRepository.findByRegistroLineaAlmaId(registro.getId())
                .orElse(new AtencionAph());
        atencionAph.setRegistroLineaAlma(registro);
        atencionAph.setFechaHora(request.getFechaHora());
        atencionAph.setProtocoloAph(protocoloAphRepository.findById(request.getIdProtocoloAph())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Protocolo APH no encontrado con ID: " + request.getIdProtocoloAph())));
        atencionAph.setPracticoTriage(Boolean.TRUE.equals(request.getPracticoTriage()));
        if (request.getIdResultadoTriage() != null) {
            atencionAph.setResultadoTriage(resultadoTriageRepository.findById(request.getIdResultadoTriage())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Resultado triage no encontrado con ID: " + request.getIdResultadoTriage())));
        } else {
            atencionAph.setResultadoTriage(null);
        }
        atencionAph.setNotaAph(request.getNotaAph());
        atencionAph.setMotivoNoTriage(request.getMotivoNoTriage());
        atencionAph.setAceptaPsicologia(Boolean.TRUE.equals(request.getAceptaPsicologia()));
        atencionAph.setRequiereRemision(Boolean.TRUE.equals(request.getRequiereRemision()));
        if (atencionAph.getId() == null) {
            atencionAph.setUsuarioCreacion(usuarioAutenticado);
        }
        atencionAph.setUsuarioActualizacion(usuarioAutenticado);
        return atencionAph;
    }

    private RegistroLineaAlmaResponse mapToResponse(RegistroLineaAlma registro,
            AtencionAph atencionAph,
            List<RemisionRegistroAlma> remisiones) {
        return RegistroLineaAlmaResponse.builder()
                .id(registro.getId())
                .idPersona(registro.getPersona() != null ? registro.getPersona().getId() : null)
                .idTipoReporte(registro.getTipoReporte() != null ? registro.getTipoReporte().getId() : null)
                .tipoReporte(registro.getTipoReporte() != null ? registro.getTipoReporte().getNombre() : null)
                .idCanalContacto(registro.getCanalContacto() != null ? registro.getCanalContacto().getId() : null)
                .canalContacto(registro.getCanalContacto() != null ? registro.getCanalContacto().getNombre() : null)
                .quienRemite(registro.getQuienRemite() != null ? registro.getQuienRemite().getNombre() : null)
                .idQuienRemite(registro.getQuienRemite() != null ? registro.getQuienRemite().getId() : null)
                .fechaHoraAtencion(registro.getFechaHoraAtencion())
                .idPersonaAtiende(
                        registro.getPersonaAtiende() != null ? registro.getPersonaAtiende().getId().longValue() : null)
                .idTipoServicio(registro.getTipoServicio() != null ? registro.getTipoServicio().getId() : null)
                .tipoServicio(registro.getTipoServicio() != null ? registro.getTipoServicio().getNombre() : null)
                .idPersonaRegistra(registro.getPersonaRegistra() != null ? registro.getPersonaRegistra().getId() : null)
                .idLugarEntrevista(registro.getLugarEntrevista() != null ? registro.getLugarEntrevista().getId() : null)
                .lugarEntrevista(
                        registro.getLugarEntrevista() != null ? registro.getLugarEntrevista().getNombre() : null)
                .idIdentidadGenero(registro.getIdentidadGenero() != null ? registro.getIdentidadGenero().getId() : null)
                .idOrientacionSexual(
                        registro.getOrientacionSexual() != null ? registro.getOrientacionSexual().getId() : null)
                .idEtnia(registro.getEtnia() != null ? registro.getEtnia().getId() : null)
                .idCiudadResidencia(
                        registro.getCiudadResidencia() != null ? registro.getCiudadResidencia().getId() : null)
                .direccionResidencia(registro.getDireccionResidencia())
                .idVinculoUdeA(registro.getVinculoUdeA() != null ? registro.getVinculoUdeA().getId() : null)
                .idUnidadAcademica(registro.getUnidadAcademica() != null ? registro.getUnidadAcademica().getId() : null)
                .idPrograma(registro.getPrograma() != null ? registro.getPrograma().getId() : null)
                .idUnidadAdministrativa(
                        registro.getUnidadAdministrativa() != null ? registro.getUnidadAdministrativa().getId() : null)
                .idCampus(registro.getCampus() != null ? registro.getCampus().getId() : null)
                .atencionAph(mapAtencionAph(atencionAph))
                .remisiones(mapRemisiones(remisiones))
                .fechaCreacion(registro.getFechaCreacion())
                .idUsuarioCreacion(registro.getUsuarioCreacion() != null ? registro.getUsuarioCreacion().getId() : null)
                .fechaActualizacion(registro.getFechaActualizacion())
                .idUsuarioActualizacion(
                        registro.getUsuarioActualizacion() != null ? registro.getUsuarioActualizacion().getId() : null)
                .observacionesCorreo(registro.getObservacionesCorreo())
                .observacionesTelefono(registro.getObservacionesTelefono())
                .build();
    }

    private AtencionAphResponse mapAtencionAph(AtencionAph atencionAph) {
        if (atencionAph == null) {
            return null;
        }
        return AtencionAphResponse.builder()
                .id(atencionAph.getId())
                .fechaHora(atencionAph.getFechaHora())
                .idProtocoloAph(atencionAph.getProtocoloAph() != null ? atencionAph.getProtocoloAph().getId() : null)
                .protocoloAph(atencionAph.getProtocoloAph() != null ? atencionAph.getProtocoloAph().getNombre() : null)
                .practicoTriage(atencionAph.isPracticoTriage())
                .idResultadoTriage(
                        atencionAph.getResultadoTriage() != null ? atencionAph.getResultadoTriage().getId() : null)
                .resultadoTriage(
                        atencionAph.getResultadoTriage() != null ? atencionAph.getResultadoTriage().getNombre() : null)
                .notaAph(atencionAph.getNotaAph())
                .motivoNoTriage(atencionAph.getMotivoNoTriage())
                .aceptaPsicologia(atencionAph.isAceptaPsicologia())
                .requiereRemision(atencionAph.isRequiereRemision())
                .build();
    }

    private List<RemisionRegistroAlmaResponse> mapRemisiones(List<RemisionRegistroAlma> remisiones) {
        return remisiones.stream()
                .map(remision -> RemisionRegistroAlmaResponse.builder()
                        .idTipoRemision(remision.getIdtiporemision())
                        .tipoRemision(
                                remision.getTipoRemision() != null ? remision.getTipoRemision().getNombre() : null)
                        .cual(remision.getCual())
                        .fecha(remision.getFecha())
                        .build())
                .toList();
    }

    private ContactoLineaAlmaResponse mapContacto(ContactoLineaAlma contacto) {
        return ContactoLineaAlmaResponse.builder()
                .id(contacto.getId())
                .idRegistroLineaAlma(
                        contacto.getRegistroLineaAlma() != null ? contacto.getRegistroLineaAlma().getId() : null)
                .fecha(contacto.getFecha())
                .idResultado(contacto.getResultado() != null ? contacto.getResultado().getId() : null)
                .resultado(contacto.getResultado() != null ? contacto.getResultado().getNombre() : null)
                .fechaCreacion(contacto.getFechaCreacion())
                .build();
    }

    private Usuario obtenerUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("Usuario no autenticado");
        }
        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + authentication.getName()));
    }
}
