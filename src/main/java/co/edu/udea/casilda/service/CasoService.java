package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.request.*;
import co.edu.udea.casilda.dto.response.CasoResponse;
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
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import co.edu.udea.casilda.dto.response.CasoListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
@Slf4j
public class CasoService {

    private final CitaRepository citaRepository;
    private final SolicitudAtencionRepository solicitudAtencionRepository;
    private final PersonaRepository personaRepository;
    private final CasoRepository casoRepository;
    private final AtencionRepository atencionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaCasoService auditoriaCasoService;
    private final CorreoPersonaRepository correoPersonaRepository;
    private final TelefonoPersonaRepository telefonoPersonaRepository;

    // Repositories de maestros
    private final MunicipioRepository municipioRepository;
    private final RegimenRepository regimenRepository;
    private final EPSRepository epsRepository;
    private final EtniaRepository etniaRepository;
    private final UnidadAdministrativaRepository unidadAdministrativaRepository;
    private final CampusRepository campusRepository;
    private final UnidadAcademicaRepository unidadAcademicaRepository;
    private final VinculoUdeARepository vinculoUdeARepository;
    private final FormaOcurrenciaRepository formaOcurrenciaRepository;
    private final LugarOcurrenciaRepository lugarOcurrenciaRepository;
    private final ActividadMisionalRepository actividadMisionalRepository;
    private final VinculoAgresorVictimaRepository vinculoAgresorVictimaRepository;
    private final AgresorVictimaRepository agresorVictimaRepository;
    private final HechoRepository hechoRepository;
    private final IdentidadGeneroRepository identidadGeneroRepository;
    private final ProgramaRepository programaRepository;
    private final TiempoOcurridoUnidadRepository tiempoOcurridoUnidadRepository;
    private final SubTipoDiscapacidadRepository subTipoDiscapacidadRepository;
    private final ModalidadViolenciaRepository modalidadViolenciaRepository;
    private final SexoRepository sexoRepository;
    private final PresuntoAgresorRepository presuntoAgresorRepository;

    /**
     * Pestaña 0 - Datos de la persona: actualiza persona (sexo, correos, teléfonos)
     * + régimen/eps/etnia/residencia en el Caso.
     */
    @Transactional
    public CasoResponse registrarPestanaDatosPersona(PestanaDatosPersonaRequest request) {
        log.info("Pestaña 0: Actualizando datos de persona para cita ID: {}", request.getCitaId());

        Usuario usuario = obtenerUsuarioAutenticadoRequerido();

        Caso caso;
        boolean esNuevo = false;
        if (request.getIdCaso() != null) {
            caso = casoRepository.findById(request.getIdCaso())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Caso no encontrado con ID: " + request.getIdCaso()));
        } else {
            // Buscar si ya existe un caso para esta cita
            List<Caso> casosExistentes = casoRepository.findByCitaIdOrderByFechaCreacionDesc(request.getCitaId());
            if (!casosExistentes.isEmpty()) {
                caso = casosExistentes.get(0);
            } else {
                // Crear un nuevo caso
                esNuevo = true;
                Cita cita = citaRepository.findById(request.getCitaId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Cita no encontrada con ID: " + request.getCitaId()));
                if (cita.getEstadoCita() == null
                        || !EstadoCitaEnum.ATENDIDA.getId().equals(cita.getEstadoCita().getId())) {
                    throw new IllegalArgumentException("Solo se puede crear un caso después de registrar la cita como atendida.");
                }
                if (!casoRepository.findByCitaSolicitudAtencionIdOrderByFechaCreacionDesc(
                        cita.getSolicitudAtencion().getId()).isEmpty()) {
                    throw new IllegalArgumentException("La solicitud ya tiene un caso; registre la atención como seguimiento.");
                }
                caso = new Caso();
                caso.setCita(cita);
                caso.setCodigo(generarCodigoCaso(cita.getSolicitudAtencion()));
                caso.setUsuarioCreacion(usuario);
                caso.setUsuarioActualizacion(usuario);

                IdentidadGenero identidadGenero = cita.getSolicitudAtencion().getIdentidadGenero();
                if (identidadGenero == null) {
                    identidadGenero = identidadGeneroRepository.findById(8).orElse(null);
                    if (identidadGenero == null) {
                        identidadGenero = identidadGeneroRepository.findById(1).orElse(null);
                    }
                }
                caso.setIdentidadGenero(identidadGenero);

                Regimen defaultRegimen = regimenRepository.findById(1)
                        .orElseThrow(() -> new ResourceNotFoundException("Regimen por defecto no encontrado (ID 1)"));
                EPS defaultEps = epsRepository.findById(1)
                        .orElseThrow(() -> new ResourceNotFoundException("EPS por defecto no encontrado (ID 1)"));
                caso.setRegimen(defaultRegimen);
                caso.setEps(defaultEps);
            }
        }

        caso.setUsuarioActualizacion(usuario);
        caso.setFechaActualizacion(LocalDateTime.now());

        // Actualizar régimen y EPS
        if (request.getIdRegimen() != null) {
            caso.setRegimen(regimenRepository.findById(request.getIdRegimen()).orElse(null));
        }
        if (request.getIdEps() != null) {
            caso.setEps(epsRepository.findById(request.getIdEps()).orElse(null));
        }

        // Actualizar datos demográficos en Caso
        if (request.getPersona() != null) {
            if (request.getPersona().getIdEtnia() != null) {
                caso.setEtnia(etniaRepository.findById(request.getPersona().getIdEtnia()).orElse(null));
            }
            if (request.getPersona().getIdCiudadResidencia() != null) {
                caso.setCiudadResidencia(
                        municipioRepository.findById(request.getPersona().getIdCiudadResidencia()).orElse(null));
            }
            if (request.getPersona().getDireccionResidencia() != null) {
                caso.setDireccionResidencia(request.getPersona().getDireccionResidencia());
            }

            // Actualizar persona (sexo, correos, teléfonos)
            SolicitudAtencion solicitud = caso.getCita().getSolicitudAtencion();
            Persona persona = resolverPersonaAtencion(solicitud);
            actualizarPersona(persona, request.getPersona());
        }

        // Actualizar discapacidades de la persona
        if (request.getDiscapacidades() != null) {
            SolicitudAtencion solicitud = caso.getCita().getSolicitudAtencion();
            Persona persona = resolverPersonaAtencion(solicitud);
            List<SubTipoDiscapacidad> subTipos = request.getDiscapacidades().stream()
                    .map(req -> {
                        if (req.getIdSubTipoDiscapacidad() == null) {
                            throw new IllegalArgumentException("El subtipo de discapacidad es obligatorio");
                        }
                        return subTipoDiscapacidadRepository.findById(req.getIdSubTipoDiscapacidad())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                        "Subtipo de discapacidad no encontrado: " + req.getIdSubTipoDiscapacidad()));
                    }).toList();
            persona.getDiscapacidades().clear();
            personaRepository.saveAndFlush(persona);
            for (int i = 0; i < request.getDiscapacidades().size(); i++) {
                DiscapacidadPersonaRequest req = request.getDiscapacidades().get(i);
                SubTipoDiscapacidad subTipo = subTipos.get(i);
                DiscapacidadPersona dp = new DiscapacidadPersona();
                dp.setIdpersona(persona.getId());
                dp.setIdsubtipodiscapacidad(subTipo.getId());
                dp.setPersona(persona);
                dp.setSubTipoDiscapacidad(subTipo);
                dp.setDescripcion(req.getDescripcion() != null ? req.getDescripcion().trim() : "");
                persona.getDiscapacidades().add(dp);
            }
            personaRepository.save(persona);
        }

        caso = casoRepository.save(caso);
        auditoriaCasoService.registrar(caso, usuario, esNuevo ? "CREACION" : "ACTUALIZACION",
                esNuevo ? "Se creó un caso." : "Se actualizaron datos del caso.");

        log.info("Pestaña 0: Datos de persona y caso actualizados. Caso ID: {}", caso.getId());

        return CasoResponse.builder()
                .id(caso.getId())
                .codigo(caso.getCodigo())
                .build();
    }

    /**
     * Pestaña 1 - Datos complementarios: actualiza contexto (dependencia, campus,
     * etc.) en el caso + observaciones de correo y teléfono en la solicitud.
     */
    @Transactional
    public void registrarPestanaDatosComplementarios(PestanaDatosComplementariosRequest request) {
        log.info("Pestaña 1: Actualizando datos complementarios para el caso ID: {}", request.getIdCaso());

        Usuario usuario = obtenerUsuarioAutenticadoRequerido();

        Caso caso = casoRepository.findById(request.getIdCaso())
                .orElseThrow(() -> new ResourceNotFoundException("Caso no encontrado con ID: " + request.getIdCaso()));

        caso.setUsuarioActualizacion(usuario);
        caso.setFechaActualizacion(LocalDateTime.now());

        if (request.getIdvinculoudea() != null) {
            caso.setVinculoUdeA(vinculoUdeARepository.findById(request.getIdvinculoudea()).orElse(null));
        } else {
            caso.setVinculoUdeA(null);
        }
        caso.setOtroVinculo(request.getOtrovinculo());

        if (request.getIdprograma() != null) {
            caso.setPrograma(programaRepository.findById(request.getIdprograma()).orElse(null));
        } else {
            caso.setPrograma(null);
        }

        if (request.getIdunidadacademica() != null) {
            caso.setUnidadAcademica(unidadAcademicaRepository.findById(request.getIdunidadacademica()).orElse(null));
        } else {
            caso.setUnidadAcademica(null);
        }

        if (request.getIdunidadadministrativa() != null) {
            caso.setUnidadAdministrativa(
                    unidadAdministrativaRepository.findById(request.getIdunidadadministrativa()).orElse(null));
        } else {
            caso.setUnidadAdministrativa(null);
        }

        if (request.getIdcampus() != null) {
            caso.setCampus(campusRepository.findById(request.getIdcampus()).orElse(null));
        } else {
            caso.setCampus(null);
        }

        // Actualizar observaciones en la solicitud
        SolicitudAtencion solicitud = caso.getCita().getSolicitudAtencion();
        solicitud.setObservacionesTelefono(request.getObservacionesTelefono());
        solicitud.setObservacionesCorreo(request.getObservacionesCorreo());

        // Actualizar correos de la persona
        Persona persona = resolverPersonaAtencion(solicitud);
        if (request.getCorreos() != null) {
            List<CorreoPersona> existentes = correoPersonaRepository.findByIdpersona(persona.getId());
            correoPersonaRepository.deleteAll(existentes);
            persona.getCorreos().clear();
            personaRepository.saveAndFlush(persona);
            for (CorreoSolicitanteRequest correoReq : request.getCorreos()) {
                CorreoPersona correo = new CorreoPersona();
                correo.setIdpersona(persona.getId());
                correo.setIdtipo(correoReq.getTipoId());
                correo.setPersona(persona);
                correo.setCorreo(correoReq.getCorreo());
                persona.getCorreos().add(correo);
            }
        }

        // Actualizar teléfonos de la persona
        if (request.getTelefonos() != null) {
            List<TelefonoPersona> existentes = telefonoPersonaRepository.findByIdpersona(persona.getId());
            telefonoPersonaRepository.deleteAll(existentes);
            persona.getTelefonos().clear();
            personaRepository.saveAndFlush(persona);
            for (TelefonoSolicitanteRequest telefonoReq : request.getTelefonos()) {
                TelefonoPersona telefono = new TelefonoPersona();
                telefono.setIdpersona(persona.getId());
                telefono.setIdtipo(telefonoReq.getTipoId());
                telefono.setPersona(persona);
                telefono.setTelefono(telefonoReq.getTelefono());
                persona.getTelefonos().add(telefono);
            }
        }

        solicitudAtencionRepository.save(solicitud);
        personaRepository.save(persona);
        casoRepository.save(caso);
        auditoriaCasoService.registrar(caso, usuario, "ACTUALIZACION", "Se actualizaron datos del caso.");

        log.info("Pestaña 1: Datos complementarios actualizados para caso ID: {}", caso.getId());
    }

    /* Pestaña 2 - Documentación: actualiza caso principal + hechos. */
    @Transactional
    public void registrarPestanaDocumentacion(PestanaDocumentacionRequest request) {
        log.info("Pestaña 2: Actualizando documentación para caso ID: {}", request.getIdCaso());

        Usuario usuario = obtenerUsuarioAutenticadoRequerido();

        Caso caso = casoRepository.findById(request.getIdCaso())
                .orElseThrow(() -> new ResourceNotFoundException("Caso no encontrado con ID: " + request.getIdCaso()));

        caso.setUsuarioActualizacion(usuario);
        caso.setFechaActualizacion(LocalDateTime.now());

        // Mapear campos planos de documentación
        caso.setHacecuantooccurrio(request.getHacecuantooccurrio());
        actualizarAmbitoOcurrencia(caso, request.getAmbitoOcurrencia(), request.getAmbitoOcurrenciaOtro());

        if (request.getIdtiempoocurridounidad() != null) {
            caso.setTiempoOcurridoUnidad(
                    tiempoOcurridoUnidadRepository.findById(request.getIdtiempoocurridounidad()).orElse(null));
        } else {
            caso.setTiempoOcurridoUnidad(null);
        }

        if (request.getIdformaocurrencia() != null) {
            caso.setFormaOcurrencia(formaOcurrenciaRepository.findById(request.getIdformaocurrencia()).orElse(null));
        } else {
            caso.setFormaOcurrencia(null);
        }

        if (request.getIdciudadhechos() != null) {
            caso.setCiudadHechos(municipioRepository.findById(request.getIdciudadhechos()).orElse(null));
        } else {
            caso.setCiudadHechos(null);
        }

        if (request.getIdlugarocurrencia() != null) {
            caso.setLugarOcurrencia(lugarOcurrenciaRepository.findById(request.getIdlugarocurrencia()).orElse(null));
        } else {
            caso.setLugarOcurrencia(null);
        }

        caso.setViolenciaBasadaGenero(request.getViolenciabasadagenero());
        caso.setHechoViolenciaOcurrioActividadesMisionales(request.getHechoviolenciaocurrioactividadesmisionales());

        if (request.getIdactivadmisional() != null) {
            caso.setActividadMisional(
                    actividadMisionalRepository.findById(request.getIdactivadmisional())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Actividad misional no encontrada con ID: " + request.getIdactivadmisional())));
        } else {
            caso.setActividadMisional(null);
        }

        Caso casoGuardado = casoRepository.save(caso);

        // Guardar hechos
        if (request.getHechos() != null) {
            guardarHechos(casoGuardado, request.getHechos());
        }
        auditoriaCasoService.registrar(casoGuardado, usuario, "ACTUALIZACION", "Se actualizaron datos del caso.");

        log.info("Pestaña 2: Documentación actualizada para caso ID: {}", request.getIdCaso());
    }

    /**
     * Pestaña 3 - Tipo de violencia: actualiza modalidades de violencia del caso
     * principal.
     */
    @Transactional
    public void registrarPestanaVBG(PestanaVBGRequest request) {
        log.info("Pestaña 3: Actualizando tipos de violencia para caso ID: {}", request.getIdCaso());

        Usuario usuario = obtenerUsuarioAutenticadoRequerido();

        Caso caso = casoRepository.findById(request.getIdCaso())
                .orElseThrow(() -> new ResourceNotFoundException("Caso no encontrado con ID: " + request.getIdCaso()));

        caso.setUsuarioActualizacion(usuario);
        caso.setFechaActualizacion(LocalDateTime.now());

        // Recolectar todas las modalidades y asociarlas al caso
        List<Integer> todosIdsModalidades = new ArrayList<>();
        if (request.getModalidadesViolenciaPsicologica() != null)
            todosIdsModalidades.addAll(request.getModalidadesViolenciaPsicologica());
        if (request.getModalidadesViolenciaFisica() != null)
            todosIdsModalidades.addAll(request.getModalidadesViolenciaFisica());
        if (request.getModalidadesViolenciaSexual() != null)
            todosIdsModalidades.addAll(request.getModalidadesViolenciaSexual());
        if (request.getModalidadesViolenciaInstitucional() != null)
            todosIdsModalidades.addAll(request.getModalidadesViolenciaInstitucional());
        if (request.getModalidadesViolenciaEconomica() != null)
            todosIdsModalidades.addAll(request.getModalidadesViolenciaEconomica());
        if (request.getModalidadesViolenciaInformatica() != null)
            todosIdsModalidades.addAll(request.getModalidadesViolenciaInformatica());
        if (request.getModalidadesViolenciaPrejuicio() != null)
            todosIdsModalidades.addAll(request.getModalidadesViolenciaPrejuicio());

        caso.getModalidadesViolencia().clear();
        casoRepository.saveAndFlush(caso);

        for (Integer idModalidad : todosIdsModalidades) {
            ModalidadViolencia mv = modalidadViolenciaRepository.findById(idModalidad)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Modalidad de violencia no encontrada con ID: " + idModalidad));
            ModalidadViolenciaCaso mvc = new ModalidadViolenciaCaso();
            mvc.setIdcaso(caso.getId());
            mvc.setIdmodalidadviolencia(idModalidad);
            mvc.setCaso(caso);
            mvc.setModalidadViolencia(mv);
            caso.getModalidadesViolencia().add(mvc);
        }

        caso.setTipoViolenciaPsicologica(request.getModalidadesViolenciaPsicologica() != null
                && !request.getModalidadesViolenciaPsicologica().isEmpty());
        caso.setTipoViolenciaFisica(request.getModalidadesViolenciaFisica() != null
                && !request.getModalidadesViolenciaFisica().isEmpty());
        caso.setTipoViolenciaSexual(request.getModalidadesViolenciaSexual() != null
                && !request.getModalidadesViolenciaSexual().isEmpty());
        caso.setTipoViolenciaInstitucional(request.getModalidadesViolenciaInstitucional() != null
                && !request.getModalidadesViolenciaInstitucional().isEmpty());
        caso.setTipoViolenciaEconomicaPatrimonial(request.getModalidadesViolenciaEconomica() != null
                && !request.getModalidadesViolenciaEconomica().isEmpty());
        caso.setTipoViolenciaSexualInformatica(request.getModalidadesViolenciaInformatica() != null
                && !request.getModalidadesViolenciaInformatica().isEmpty());
        caso.setTipoViolenciaPorPrejuicio(request.getModalidadesViolenciaPrejuicio() != null
                && !request.getModalidadesViolenciaPrejuicio().isEmpty());

        casoRepository.save(caso);
        auditoriaCasoService.registrar(caso, usuario, "ACTUALIZACION", "Se actualizaron datos del caso.");
        log.info("Pestaña 3: Tipos de violencia actualizados para caso ID: {}", caso.getId());
    }

    /**
     * Pestaña 4 - Presunto agresor: crea/actualiza datos del agresor/víctima.
     */
    @Transactional
    public void registrarPestanaPresuntoAgresor(PestanaPresuntoAgresorRequest request) {
        log.info("Pestaña 4: Actualizando datos de presunto agresor para caso ID: {}", request.getIdCaso());

        Usuario usuario = obtenerUsuarioAutenticadoRequerido();

        Caso caso = casoRepository.findById(request.getIdCaso())
                .orElseThrow(() -> new ResourceNotFoundException("Caso no encontrado con ID: " + request.getIdCaso()));

        caso.setUsuarioActualizacion(usuario);
        caso.setFechaActualizacion(LocalDateTime.now());

        if (request.getAgresores() != null) {
            guardarPresuntosAgresores(caso, request.getAgresores());
        }

        casoRepository.save(caso);
        auditoriaCasoService.registrar(caso, usuario, "ACTUALIZACION", "Se actualizaron datos del caso.");
        log.info("Pestaña 4: Datos de presunto agresor actualizados para caso ID: {}", caso.getId());
    }

    // Private helper methods
    private String generarCodigoCaso(SolicitudAtencion solicitud) {
        LocalDateTime now = LocalDateTime.now();
        int currentYear = now.getYear();
        String prefijoActor = obtenerPrefijoActor(solicitud);
        int secuencial = obtenerSiguienteSecuencial(currentYear);
        String mmdd = now.format(DateTimeFormatter.ofPattern("MMdd"));
        int randomNum = java.util.concurrent.ThreadLocalRandom.current().nextInt(10000);

        // Formato: prefijo+AAAA+SSSSSSSS+MMDD+AAAA
        // Ejemplo: SP2026+99999999+0320+9999
        return String.format("%s%d+%08d+%s+%04d", prefijoActor, currentYear, secuencial, mmdd, randomNum);
    }

    private String obtenerPrefijoActor(SolicitudAtencion solicitud) {
        String defaultPrefix = "EA";

        return defaultPrefix;
    }

    private int obtenerSiguienteSecuencial(int year) {
        List<String> codigos = casoRepository.findCodigosByYear(String.valueOf(year));
        int minSequential = 99999999;
        boolean hasAny = false;
        for (String codigo : codigos) {
            String[] parts = codigo.split("\\+");
            if (parts.length >= 2) {
                try {
                    int seq = Integer.parseInt(parts[1].trim());
                    if (seq < minSequential) {
                        minSequential = seq;
                    }
                    hasAny = true;
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }
        if (!hasAny) {
            return 99999999;
        }
        return minSequential - 1;
    }

    private Persona resolverPersonaAtencion(SolicitudAtencion solicitud) {
        if (solicitud == null) {
            throw new ResourceNotFoundException("Solicitud de atención no encontrada para la cita");
        }

        if (solicitud.getSolicitante() != null) {
            return solicitud.getSolicitante();
        }

        throw new ResourceNotFoundException("No se encontró persona asociada a la solicitud ID: " + solicitud.getId());
    }

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
            personaRepository.saveAndFlush(persona); // Flush deletes immediately to avoid unique constraint violations
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
            personaRepository.saveAndFlush(persona); // Flush deletes immediately to avoid unique constraint violations
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
     * Crea o actualiza los datos del agresor/víctima asociados al caso.
     */
    private void guardarPresuntosAgresores(Caso caso, List<AgresorVictimaRequest> requestList) {
        presuntoAgresorRepository.deleteByCasoId(caso.getId());

        if (requestList == null || requestList.isEmpty()) {
            return;
        }

        for (AgresorVictimaRequest request : requestList) {
            boolean isEmpty = (request.getPrimerNombre() == null || request.getPrimerNombre().isBlank())
                    && (request.getPrimerApellido() == null || request.getPrimerApellido().isBlank())
                    && request.getIdVinculoUniversidad() == null
                    && request.getIdVinculoVictima() == null;
            if (isEmpty) {
                continue;
            }

            PresuntoAgresor presuntoAgresor = new PresuntoAgresor();
            presuntoAgresor.setCaso(caso);
            presuntoAgresor.setPrimerNombre(request.getPrimerNombre());
            presuntoAgresor.setSegundoNombre(request.getSegundoNombre());
            presuntoAgresor.setPrimerApellido(request.getPrimerApellido());
            presuntoAgresor.setSegundoApellido(request.getSegundoApellido());

            if (request.getIdVinculoUniversidad() != null) {
                presuntoAgresor
                        .setVinculoUdeA(vinculoUdeARepository.findById(request.getIdVinculoUniversidad()).orElse(null));
            }
            if (request.getIdVinculoVictima() != null) {
                presuntoAgresor.setVinculoAgresorVictima(
                        vinculoAgresorVictimaRepository.findById(request.getIdVinculoVictima()).orElse(null));
            }

            presuntoAgresorRepository.save(presuntoAgresor);
        }
    }

    /**
     * Crea o actualiza los hechos asociados al caso.
     */
    private void guardarHechos(Caso caso, List<HechoRequest> hechosRequest) {
        hechoRepository.deleteByCasoId(caso.getId());

        if (hechosRequest == null || hechosRequest.isEmpty()) {
            return;
        }

        for (HechoRequest hechoRequest : hechosRequest) {
            Hecho hecho = new Hecho();
            hecho.setCaso(caso);
            hecho.setFecha(resolverFechaHecho(hechoRequest.getFecha()));
            hecho.setLugar(hechoRequest.getLugar().trim());
            hecho.setDescripcion(hechoRequest.getDescripcion().trim());
            hechoRepository.save(hecho);
        }
    }

    /**
     * Resuelve la fecha del hecho desde texto. Si no puede parsearse, usa la fecha
     * actual.
     */
    private LocalDateTime resolverFechaHecho(String fechaTexto) {
        if (fechaTexto == null || fechaTexto.isBlank()) {
            return LocalDateTime.now();
        }

        try {
            return LocalDateTime.parse(fechaTexto.trim());
        } catch (Exception ex) {
            log.warn("No fue posible parsear la fecha del hecho '{}', se usará fecha actual", fechaTexto);
            return LocalDateTime.now();
        }
    }

    private Usuario obtenerUsuarioAutenticadoRequerido() {
        Usuario usuario = obtenerUsuarioAutenticado();
        if (usuario == null) {
            throw new ResourceNotFoundException("Usuario no autenticado");
        }
        return usuario;
    }

    private Usuario obtenerUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String email = auth.getName();
        return usuarioRepository.findByEmail(email).orElse(null);
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Transactional(readOnly = true)
    public Page<CasoListResponse> listarCasosPaginados(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fechaCreacion").descending());
        Page<Caso> casos = casoRepository.findAll(pageable);
        return casos.map(this::mapToListResponse);
    }

    void actualizarAmbitoOcurrencia(Caso caso, String ambito, String otro) {
        if (ambito == null) {
            return;
        }
        if (!ambito.isBlank() && !java.util.Set.of("Pareja/expareja", "Laboral", "Académico", "Sindical",
                "Político", "Público", "Familiar", "Privado", "Otro", "N/A").contains(ambito)) {
            throw new IllegalArgumentException("Ámbito de ocurrencia no válido.");
        }
        if (otro != null && otro.length() > 255) {
            throw new IllegalArgumentException("La descripción del ámbito no puede superar 255 caracteres.");
        }
        caso.setAmbitoOcurrencia(ambito.isBlank() ? null : ambito);
        caso.setAmbitoOcurrenciaOtro("Otro".equals(ambito) && otro != null && !otro.isBlank() ? otro.trim() : null);
    }

    @Transactional(readOnly = true)
    public CasoListResponse obtenerCasoPorId(Long id) {
        Caso caso = casoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caso no encontrado con ID: " + id));
        return mapToListResponse(caso);
    }

    private CasoListResponse mapToListResponse(Caso caso) {
        Cita cita = caso.getCita();
        SolicitudAtencion sa = cita != null ? cita.getSolicitudAtencion() : null;
        Remision remision = sa != null ? sa.getRemision() : null;

        Persona persona = null;
        if (sa != null) {
            persona = sa.getSolicitante();
            if (persona == null && remision != null) {
                persona = remision.getRemitente();
            }
        }

        String correoInstitucional = "";
        String correoPersonal = "";
        String celular = "";
        String telefonoAlterno = "";

        if (persona != null) {
            if (persona.getCorreos() != null) {
                for (CorreoPersona cp : persona.getCorreos()) {
                    if (cp.getTipoCorreo() == null || cp.getCorreo() == null)
                        continue;
                    Integer idTipo = cp.getTipoCorreo().getId();
                    if (idTipo == 1 && correoInstitucional.isEmpty()) {
                        correoInstitucional = cp.getCorreo();
                    } else if (idTipo == 2 && correoPersonal.isEmpty()) {
                        correoPersonal = cp.getCorreo();
                    } else if (correoInstitucional.isEmpty()) {
                        correoInstitucional = cp.getCorreo();
                    } else if (correoPersonal.isEmpty()) {
                        correoPersonal = cp.getCorreo();
                    }
                }
            }
            if (persona.getTelefonos() != null) {
                for (TelefonoPersona tp : persona.getTelefonos()) {
                    if (tp.getTipoTelefono() == null || tp.getTelefono() == null)
                        continue;
                    Integer idTipo = tp.getTipoTelefono().getId();
                    if ((idTipo == 1 || idTipo == 3) && celular.isEmpty()) {
                        celular = tp.getTelefono();
                    } else if ((idTipo == 2 || idTipo == 4) && telefonoAlterno.isEmpty()) {
                        telefonoAlterno = tp.getTelefono();
                    } else if (celular.isEmpty()) {
                        celular = tp.getTelefono();
                    } else if (telefonoAlterno.isEmpty()) {
                        telefonoAlterno = tp.getTelefono();
                    }
                }
            }
        }

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
        if (sa != null && sa.getAsignaciones() != null && !sa.getAsignaciones().isEmpty()) {
            Asignacion ultimaAsignacion = sa.getAsignaciones().get(sa.getAsignaciones().size() - 1);
            if (ultimaAsignacion.getGrupoProfesional() != null) {
                profesionalNombre = ultimaAsignacion.getGrupoProfesional().getNombre();
            }
            if (ultimaAsignacion.getTipoAsignacion() != null) {
                tipoAsignacionNombre = ultimaAsignacion.getTipoAsignacion().getNombre();
            }
        }

        return CasoListResponse.builder()
                .id(caso.getId())
                .codigo(caso.getCodigo())
                .idActividadMisional(caso.getActividadMisional() != null ? caso.getActividadMisional().getId() : null)
                .formaOcurrencia(caso.getFormaOcurrencia() != null ? caso.getFormaOcurrencia().getNombre() : null)
                .ambitoOcurrencia(caso.getAmbitoOcurrencia())
                .ambitoOcurrenciaOtro(caso.getAmbitoOcurrenciaOtro())
                .solicitudId(sa != null ? sa.getId() : null)
                .citaId(cita != null ? cita.getId() : null)
                .cantidadAtenciones(atencionRepository.countByCasoId(caso.getId()))
                .nombreSolicitante(persona != null ? persona.getNombreCompleto().trim() : "")
                .tipoDocumento(persona != null && persona.getTipoIdentificacion() != null
                        ? persona.getTipoIdentificacion().getNombre()
                        : null)
                .documento(persona != null ? persona.getNumeroDocumento() : "")
                .primerNombre(persona != null ? persona.getPrimerNombre() : "")
                .segundoNombre(persona != null ? persona.getSegundoNombre() : "")
                .primerApellido(persona != null ? persona.getPrimerApellido() : "")
                .segundoApellido(persona != null ? persona.getSegundoApellido() : "")
                .fechaNacimiento(persona != null && persona.getFechaNacimiento() != null
                        ? persona.getFechaNacimiento().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        : null)
                .sexo(persona != null && persona.getSexo() != null ? persona.getSexo().getNombre() : null)
                .etnia(caso.getEtnia() != null ? caso.getEtnia().getNombre() : null)
                .orientacionSexual(caso.getOrientacionSexual() != null ? caso.getOrientacionSexual().getNombre() : null)
                .eps(caso.getEps() != null ? caso.getEps().getNombre() : null)
                .regimenSalud(caso.getRegimen() != null ? caso.getRegimen().getNombre() : null)
                .departamentoNacimiento(persona != null && persona.getCiudadNacimiento() != null
                        && persona.getCiudadNacimiento().getDepartamento() != null
                                ? persona.getCiudadNacimiento().getDepartamento().getNombre()
                                : null)
                .ciudadNacimiento(persona != null && persona.getCiudadNacimiento() != null
                        ? persona.getCiudadNacimiento().getNombre()
                        : null)
                .departamentoResidencia(
                        caso.getCiudadResidencia() != null && caso.getCiudadResidencia().getDepartamento() != null
                                ? caso.getCiudadResidencia().getDepartamento().getNombre()
                                : null)
                .ciudadResidencia(caso.getCiudadResidencia() != null ? caso.getCiudadResidencia().getNombre() : null)
                .direccionResidencia(caso.getDireccionResidencia())
                .fechaCaso(caso.getFechaCreacion() != null ? caso.getFechaCreacion().format(FORMATTER) : null)
                .fechaCierre(caso.getFechaCierre())
                .estadoCaso(caso.getEstadoCaso() != null ? caso.getEstadoCaso().getNombre() : null)
                .tipoSolicitud(sa != null && sa.getTipoSolicitud() != null ? sa.getTipoSolicitud().getNombre() : null)
                .unidadAdministrativa(unidadAdministrativaNombre)
                .profesional(profesionalNombre)
                .tipoAsignacion(tipoAsignacionNombre)
                .unidadAcademica(unidadAcademicaNombre)
                .campus(campusNombre)
                .identidadGenero(
                        sa != null && sa.getIdentidadGenero() != null ? sa.getIdentidadGenero().getNombre() : null)
                .celular(celular)
                .telefonoAlterno(telefonoAlterno)
                .correoInstitucional(correoInstitucional)
                .correoPersonal(correoPersonal)
                .build();

    }
}
