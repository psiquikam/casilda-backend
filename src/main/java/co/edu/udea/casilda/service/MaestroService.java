package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.request.MaestroRequest;
import co.edu.udea.casilda.dto.response.MaestroDTO;
import co.edu.udea.casilda.exception.ResourceNotFoundException;
import co.edu.udea.casilda.model.entity.*;
import co.edu.udea.casilda.repository.*;
import co.edu.udea.casilda.model.enums.TipoServicioEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar maestros del sistema.
 * Proporciona métodos para obtener datos de maestros desde la base de datos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MaestroService {

    // Repositories de maestros
    private final PaisRepository paisRepository;
    private final SexoRepository sexoRepository;
    private final TipoIdentificacionRepository tipoIdentificacionRepository;
    private final EtniaRepository etniaRepository;
    private final IdentidadGeneroRepository identidadGeneroRepository;
    private final OrientacionSexualRepository orientacionSexualRepository;
    private final SubTipoDiscapacidadRepository subTipoDiscapacidadRepository;
    private final TipoDiscapacidadRepository tipoDiscapacidadRepository;
    private final DepartamentoRepository departamentoRepository;
    private final MunicipioRepository municipioRepository;
    private final CampusRepository campusRepository;
    private final UnidadAdministrativaRepository unidadAdministrativaRepository;
    private final UnidadAcademicaRepository unidadAcademicaRepository;
    private final VinculoAgresorVictimaRepository vinculoAgresorRepository;
    private final VinculoUdeARepository vinculoUdeARepository;
    private final FormaOcurrenciaRepository formaOcurrenciaRepository;
    private final LugarOcurrenciaRepository lugarOcurrenciaRepository;
    private final ActividadMisionalRepository actividadMisionalRepository;
    private final TipoViolenciaRepository tipoViolenciaRepository;
    private final GrupoAtencionRepository grupoAtencionRepository;
    private final ModalidadViolenciaRepository modalidadViolenciaRepository;
    private final ModalidadViolenciaSexualRepository modalidadViolenciaSexualRepository;
    private final CargoRepository cargoRepository;
    private final TipoSolicitudRepository tipoSolicitudRepository;
    private final ProgramaRepository programaRepository;
    private final RoleRepository roleRepository;
    private final ResultadoContactoTelefonicoRepository resultadoContactoRepository;
    private final EstadoAtencionRepository estadoAtencionRepository;
    private final EstadoCasoRepository estadoCasoRepository;
    private final RegimenRepository regimenRepository;
    private final EPSRepository epsRepository;
    private final TipoCorreoRepository tipoCorreoRepository;
    private final TipoTelefonoRepository tipoTelefonoRepository;
    private final TipoReporteAlmaRepository tipoReporteAlmaRepository;
    private final CanalContactoRepository canalContactoRepository;
    private final LugarEntrevistaRepository lugarEntrevistaRepository;
    private final ProtocoloAphRepository protocoloAphRepository;
    private final ResultadoTriageRepository resultadoTriageRepository;
    private final TipoAsignacionRepository tipoAsignacionRepository;
    private final TipoServicioRepository tipoServicioRepository;
    private final MotivoEstadoCitaRepository motivoEstadoCitaRepository;
    private final ApreciacionRepository apreciacionRepository;
    private final TipoApreciacionRepository tipoApreciacionRepository;
    private final TipoRutaActivacionRepository tipoRutaActivacionRepository;
    private final RutaActivacionRepository rutaActivacionRepository;
    private final TipoRemisionRepository tipoRemisionRepository;
    private final InstanciaRemisionRepository instanciaRemisionRepository;
    private final TipoCompromisoRepository tipoCompromisoRepository;
    private final MotivoEstadoSeguimientoRepository motivoEstadoSeguimientoRepository;
    private final TipoSeguimientoRepository tipoSeguimientoRepository;
    private final AccionRepository accionRepository;
    private final ActividadRepository actividadRepository;
    private final EstadoSeguimientoRepository estadoSeguimientoRepository;
    private final SeguimientoAtencionRepository seguimientoAtencionRepository;
    private final MedioSolicitudRepository medioSolicitudRepository;
    private final TiempoOcurridoUnidadRepository tiempoOcurridoUnidadRepository;
    private final TipoMedidaRepository tipoMedidaRepository;
    private final SubTipoMedidaRepository subTipoMedidaRepository;
    private final ResponsableMedidaProteccionRepository responsableMedidaProteccionRepository;
    private final ActorRemitenteRepository actorRemitenteRepository;


    /**
     * Obtiene lista de países
     */
    public List<MaestroDTO> obtenerPaises() {
        log.info("Obteniendo países desde la base de datos");
        return paisRepository.findAll().stream()
            .map(p -> new MaestroDTO(p.getId().longValue(), p.getCodigo(), p.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de sexos
     */
    public List<MaestroDTO> obtenerSexos() {
        log.info("Obteniendo sexos desde la base de datos");
        return sexoRepository.findAll().stream()
            .map(s -> new MaestroDTO(s.getId().longValue(), s.getCodigo(), s.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de tipos de identificación
     */
    public List<MaestroDTO> obtenerTiposIdentificacion() {
        log.info("Obteniendo tipos de identificación desde la base de datos");
        return tipoIdentificacionRepository.findAll().stream()
            .map(t -> new MaestroDTO(t.getId().longValue(), t.getCodigo(), t.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de etnias
     */
    public List<MaestroDTO> obtenerEtnias() {
        log.info("Obteniendo etnias desde la base de datos");
        return etniaRepository.findAll().stream()
            .map(e -> new MaestroDTO(e.getId().longValue(), null, e.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de identidades de género
     */
    public List<MaestroDTO> obtenerIdentidadesGenero() {
        log.info("Obteniendo identidades de género desde la base de datos");
        return identidadGeneroRepository.findAll().stream()
            .map(i -> new MaestroDTO(i.getId().longValue(), null, i.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de orientaciones sexuales
     */
    public List<MaestroDTO> obtenerOrientacionesSexuales() {
        log.info("Obteniendo orientaciones sexuales desde la base de datos");
        return orientacionSexualRepository.findAll().stream()
            .map(o -> new MaestroDTO(o.getId().longValue(), null, o.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de tipos de discapacidad
     */
    @Transactional(readOnly = true)
    public List<MaestroDTO> obtenerTipoDiscapacidad() {
        log.info("Obteniendo tipos de discapacidad desde la base de datos");
        return tipoDiscapacidadRepository.findAll().stream()
            .map(d -> new MaestroDTO(d.getId().longValue(), null, d.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de subtipos de discapacidad por tipo ID
     */
    @Transactional(readOnly = true)
    public List<MaestroDTO> obtenerSubTipoDiscapacidadPorTipoId(Integer tipoId) {
        log.info("Obteniendo subtipos de discapacidad para tipo ID: {}", tipoId);
        return subTipoDiscapacidadRepository.findByTipoId(tipoId).stream()
            .map(s -> new MaestroDTO(s.getId().longValue(), null, s.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de departamentos
     */
    public List<MaestroDTO> obtenerDepartamentos() {
        log.info("Obteniendo departamentos desde la base de datos");
        return departamentoRepository.findAll().stream()
            .map(d -> new MaestroDTO(d.getId().longValue(), d.getCodigo(), d.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de municipios
     */
    public List<MaestroDTO> obtenerMunicipios() {
        log.info("Obteniendo municipios desde la base de datos");
        return municipioRepository.findAll().stream()
            .map(m -> new MaestroDTO(m.getId().longValue(), m.getCodigo(), m.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de ciudades (municipios) por id de departamento.
     */
    public List<MaestroDTO> obtenerMunicipiosPorDepartamento(Integer departamentoId) {
        log.info("Obteniendo municipios para departamento ID {}", departamentoId);

        if (!departamentoRepository.existsById(departamentoId)) {
            throw new ResourceNotFoundException("Departamento no encontrado con ID: " + departamentoId);
        }

        return municipioRepository.findByDepartamentoIdOrderByNombreAsc(departamentoId).stream()
            .map(m -> new MaestroDTO(m.getId().longValue(), m.getCodigo(), m.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de ciudades (municipios) por codigo de departamento.
     */
    public List<MaestroDTO> obtenerMunicipiosPorCodigoDepartamento(String codigoDepartamento) {
        log.info("Obteniendo municipios para codigo de departamento {}", codigoDepartamento);

        departamentoRepository.findByCodigo(codigoDepartamento)
            .orElseThrow(() -> new ResourceNotFoundException("Departamento no encontrado con codigo: " + codigoDepartamento));

        return municipioRepository.findByDepartamentoCodigoOrderByNombreAsc(codigoDepartamento).stream()
            .map(m -> new MaestroDTO(m.getId().longValue(), m.getCodigo(), m.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de campus
     */
    public List<MaestroDTO> obtenerCampus() {
        log.info("Obteniendo campus desde la base de datos");
        return campusRepository.findAll().stream()
            .map(c -> new MaestroDTO(c.getId().longValue(), null, c.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de unidades administrativas
     */
    public List<MaestroDTO> obtenerUnidadesAdministrativas() {
        log.info("Obteniendo unidades administrativas desde la base de datos");
        return unidadAdministrativaRepository.findAll().stream()
            .map(d -> new MaestroDTO(d.getId().longValue(), null, d.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de unidades académicas/escuelas/institutos
     */
    public List<MaestroDTO> obtenerUnidadesAcademicas() {
        log.info("Obteniendo unidades académicas desde la base de datos");
        return unidadAcademicaRepository.findAll().stream()
            .map(f -> new MaestroDTO(f.getId().longValue(), null, f.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de vínculos agresor-víctima
     */
    public List<MaestroDTO> obtenerVinculosAgresorVictima() {
        log.info("Obteniendo vínculos agresor-víctima desde la base de datos");
        return vinculoAgresorRepository.findAll().stream()
            .map(v -> new MaestroDTO(v.getId().longValue(), null, v.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de vínculos Universidad
     */
    public List<MaestroDTO> obtenerVinculosUdeA() {
        log.info("Obteniendo vínculos Universidad desde la base de datos");
        return vinculoUdeARepository.findAll().stream()
            .map(v -> new MaestroDTO(v.getId().longValue(), null, v.getNombre()))
            .collect(Collectors.toList());
    }



    /**
     * Obtiene lista de formas de ocurrencia
     */
    public List<MaestroDTO> obtenerFormasOcurrencia() {
        log.info("Obteniendo formas de ocurrencia desde la base de datos");
        return formaOcurrenciaRepository.findAll().stream()
            .map(f -> new MaestroDTO(f.getId().longValue(), null, f.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de lugares de ocurrencia
     */
    public List<MaestroDTO> obtenerLugaresOcurrencia() {
        log.info("Obteniendo lugares de ocurrencia desde la base de datos");
        return lugarOcurrenciaRepository.findAll().stream()
            .map(l -> new MaestroDTO(l.getId().longValue(), null, l.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de actividades misionales
     */
    public List<MaestroDTO> obtenerActividadesMisionales() {
        log.info("Obteniendo actividades misionales desde la base de datos");
        return actividadMisionalRepository.findAll().stream()
            .map(a -> new MaestroDTO(a.getId().longValue(), null, a.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de tipos de violencia
     */
    public List<MaestroDTO> obtenerTiposViolencia() {
        log.info("Obteniendo tipos de violencia desde la base de datos");
        return tipoViolenciaRepository.findAll().stream()
            .map(t -> new MaestroDTO(t.getId().longValue(), null, t.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de modalidades de violencia
     */
    public List<MaestroDTO> obtenerModalidadesViolencia() {
        log.info("Obteniendo modalidades de violencia desde la base de datos");
        return modalidadViolenciaRepository.findAll().stream()
            .map(m -> new MaestroDTO(m.getId().longValue(), null, m.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de modalidades de violencia por tipo
     */
    public List<MaestroDTO> obtenerModalidadesViolenciaPorTipo(Integer tipoViolenciaId) {
        log.info("Obteniendo modalidades de violencia para tipo {}", tipoViolenciaId);
        return modalidadViolenciaRepository.findByTipoViolenciaIdOrderByNombreAsc(tipoViolenciaId).stream()
            .map(m -> new MaestroDTO(m.getId().longValue(), null, m.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de modalidades de violencia sexual
     */
    public List<MaestroDTO> obtenerModalidadesViolenciaSexual() {
        log.info("Obteniendo modalidades de violencia sexual desde la base de datos");
        return modalidadViolenciaSexualRepository.findAll().stream()
            .map(m -> new MaestroDTO(m.getId().longValue(), null, m.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de cargos
     */
    public List<MaestroDTO> obtenerCargos() {
        log.info("Obteniendo cargos desde la base de datos");
        return cargoRepository.findAll().stream()
            .map(c -> new MaestroDTO(c.getId().longValue(), null, c.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de tipos de solicitud
     */
    public List<MaestroDTO> obtenerTiposSolicitud() {
        log.info("Obteniendo tipos de solicitud desde la base de datos");
        return tipoSolicitudRepository.findAll().stream()
            .map(t -> new MaestroDTO(t.getId().longValue(), null, t.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de opciones para medio de solicitud
     */
    public List<MaestroDTO> obtenerMedioSolicitud() {
        log.info("Obteniendo opciones de medio de solicitud desde la base de datos");
        return medioSolicitudRepository.findAll().stream()
            .map(d -> new MaestroDTO(d.getId().longValue(), null, d.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de opciones para la unidad del tiempo ocurrido
     */
    public List<MaestroDTO> obtenerTiemposOcurridoUnidad() {
        log.info("Obteniendo opciones de unidades de tiempo ocurrido desde la base de datos");
        return tiempoOcurridoUnidadRepository.findAll().stream()
            .map(t -> new MaestroDTO(t.getId().longValue(), null, t.getNombre()))
            .collect(Collectors.toList());
    }

        /**
         * Obtiene un catálogo paginado para gestión de sistema.
         */
        public Page<MaestroDTO> obtenerCatalogoPaginado(String catalogo, int page, int size) {
        log.info("Obteniendo catálogo paginado. catalogo={}, page={}, size={}", catalogo, page, size);
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "nombre"));

        return switch (catalogo) {
            case "tipos-solicitud" -> tipoSolicitudRepository.findAll(pageable)
                .map(t -> new MaestroDTO(t.getId().longValue(), null, t.getNombre()));
            case "medio-solicitud" -> medioSolicitudRepository.findAll(pageable)
                .map(d -> new MaestroDTO(d.getId().longValue(), null, d.getNombre()));
            case "campus" -> campusRepository.findAll(pageable)
                .map(c -> new MaestroDTO(c.getId().longValue(), null, c.getNombre()));
            case "unidades-administrativas" -> unidadAdministrativaRepository.findAll(pageable)
                .map(d -> new MaestroDTO(d.getId().longValue(), null, d.getNombre()));
            case "unidades-academicas" -> unidadAcademicaRepository.findAll(pageable)
                .map(f -> new MaestroDTO(f.getId().longValue(), null, f.getNombre()));
            case "tipos-identificacion" -> tipoIdentificacionRepository.findAll(pageable)
                .map(t -> new MaestroDTO(t.getId().longValue(), t.getCodigo(), t.getNombre()));
            default -> throw new IllegalArgumentException("Catálogo no soportado para paginación: " + catalogo);
        };
        }

    /**
     * Obtiene lista de programas académicos
     */
    public List<MaestroDTO> obtenerProgramas() {
        log.info("Obteniendo programas desde la base de datos");
        return programaRepository.findAll().stream()
            .map(p -> new MaestroDTO(p.getId().longValue(), null, p.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de programas académicos filtrados por Unidad Académica y Pregrado/Posgrado
     */
    public List<MaestroDTO> obtenerProgramasPorUnidad(Integer unidadAcademicaId, boolean pregrado) {
        log.info("Obteniendo programas para unidad académica: {}, pregrado: {}", unidadAcademicaId, pregrado);
        List<Programa> programas = pregrado 
            ? programaRepository.findByIdunidadacademicaAndAplicapregradoTrue(unidadAcademicaId)
            : programaRepository.findByIdunidadacademicaAndAplicaposgradoTrue(unidadAcademicaId);
            
        return programas.stream()
            .map(p -> new MaestroDTO(p.getId().longValue(), null, p.getNombre()))
            .collect(Collectors.toList());
    }


    /**
     * Obtiene lista de roles
     */
    public List<MaestroDTO> obtenerRoles() {
        log.info("Obteniendo roles desde la base de datos");
        return roleRepository.findAll().stream()
            .map(r -> new MaestroDTO(r.getId().longValue(), r.getCodigo(), r.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de resultados de contacto telefónico
     */
    public List<MaestroDTO> obtenerResultadosContactoTelefonico() {
        log.info("Obteniendo resultados de contacto telefónico desde la base de datos");
        return resultadoContactoRepository.findAll().stream()
            .map(r -> new MaestroDTO(r.getId().longValue(), null, r.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de regímenes de salud
     */
    public List<MaestroDTO> obtenerRegimenes() {
        log.info("Obteniendo regímenes desde la base de datos");
        return regimenRepository.findAll().stream()
            .map(r -> new MaestroDTO(r.getId().longValue(), null, r.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de EPS
     */
    public List<MaestroDTO> obtenerEPS() {
        log.info("Obteniendo EPS desde la base de datos");
        return epsRepository.findAll().stream()
            .map(e -> new MaestroDTO(e.getId().longValue(), null, e.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de grupos de atención
     */
    public List<MaestroDTO> obtenerGruposAtencion() {
        log.info("Obteniendo grupos de atención desde la base de datos");
        return grupoAtencionRepository.findAll().stream()
            .map(g -> new MaestroDTO(g.getId().longValue(), null, g.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de estados de atención (tabla estadoatencion)
     */
    public List<MaestroDTO> obtenerEstadosAtencion() {
        log.info("Obteniendo estados de atención desde la base de datos");
        return estadoAtencionRepository.findAll().stream()
            .map(e -> new MaestroDTO(e.getId().longValue(), null, e.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de estados de caso (tabla estadocaso)
     */
    public List<MaestroDTO> obtenerEstadosCaso() {
        log.info("Obteniendo estados de caso desde la base de datos");
        return estadoCasoRepository.findAll().stream()
            .map(e -> new MaestroDTO(e.getId().longValue(), null, e.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de tipos de correo
     */
    public List<MaestroDTO> obtenerTiposCorreo() {
        log.info("Obteniendo tipos de correo desde la base de datos");
        return tipoCorreoRepository.findAll().stream()
            .map(tc -> new MaestroDTO(tc.getId().longValue(), null, tc.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de tipos de teléfono
     */
    public List<MaestroDTO> obtenerTiposTelefono() {
        log.info("Obteniendo tipos de teléfono desde la base de datos");
        return tipoTelefonoRepository.findAll().stream()
            .map(tt -> new MaestroDTO(tt.getId().longValue(), null, tt.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de tipos de reporte ALMA.
     */
    public List<MaestroDTO> obtenerTiposReporteAlma() {
        log.info("Obteniendo tipos de reporte ALMA desde la base de datos");
        return tipoReporteAlmaRepository.findAll().stream()
                .map(t -> new MaestroDTO(t.getId().longValue(), null, t.getNombre()))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de canales de contacto para Línea ALMA.
     */
    public List<MaestroDTO> obtenerCanalesContacto() {
        log.info("Obteniendo canales de contacto desde la base de datos");
        return canalContactoRepository.findAll().stream()
                .map(c -> new MaestroDTO(c.getId().longValue(), null, c.getNombre()))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de lugares de entrevista.
     */
    public List<MaestroDTO> obtenerLugaresEntrevista() {
        log.info("Obteniendo lugares de entrevista desde la base de datos");
        return lugarEntrevistaRepository.findAll().stream()
                .map(f -> new MaestroDTO(f.getId().longValue(), null, f.getNombre()))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de actores remitentes.
     */
    public List<MaestroDTO> obtenerActoresRemitentes() {
        log.info("Obteniendo actores remitentes desde la base de datos");
        return actorRemitenteRepository.findAll().stream()
                .map(a -> new MaestroDTO(a.getId().longValue(), null, a.getNombre()))
                .collect(Collectors.toList());
    }
    /**
     * Obtiene lista de protocolos APH.
     */
    public List<MaestroDTO> obtenerProtocolosAph() {
        log.info("Obteniendo protocolos APH desde la base de datos");
        return protocoloAphRepository.findAll().stream()
                .map(p -> new MaestroDTO(p.getId().longValue(), null, p.getNombre()))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de resultados de triage.
     */
    public List<MaestroDTO> obtenerResultadosTriage() {
        log.info("Obteniendo resultados de triage desde la base de datos");
        return resultadoTriageRepository.findAll().stream()
                .map(r -> new MaestroDTO(r.getId().longValue(), null, r.getNombre()))
                .collect(Collectors.toList());
    }

    // ==================== MÉTODOS CRUD PARA MAESTROS ====================

    /**
     * Crear un nuevo tipo de solicitud
     */
    @Transactional
    public MaestroDTO crearTipoSolicitud(MaestroRequest request) {
        log.info("Creando tipo de solicitud: {}", request.getNombre());
        TipoSolicitud tipoSolicitud = new TipoSolicitud();
        tipoSolicitud.setId(request.getId());
        tipoSolicitud.setNombre(request.getNombre());
        TipoSolicitud saved = tipoSolicitudRepository.save(tipoSolicitud);
        return new MaestroDTO(saved.getId().longValue(), null, saved.getNombre());
    }

    /**
     * Actualizar un tipo de solicitud existente
     */
    @Transactional
    public MaestroDTO actualizarTipoSolicitud(Integer id, MaestroRequest request) {
        log.info("Actualizando tipo de solicitud con id: {}", id);
        TipoSolicitud tipoSolicitud = tipoSolicitudRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tipo de solicitud no encontrado con id: " + id));
        tipoSolicitud.setNombre(request.getNombre());
        TipoSolicitud updated = tipoSolicitudRepository.save(tipoSolicitud);
        return new MaestroDTO(updated.getId().longValue(), null, updated.getNombre());
    }

    /**
     * Eliminar un tipo de solicitud
     */
    @Transactional
    public void eliminarTipoSolicitud(Integer id) {
        log.info("Eliminando tipo de solicitud con id: {}", id);
        if (!tipoSolicitudRepository.existsById(id)) {
            throw new RuntimeException("Tipo de solicitud no encontrado con id: " + id);
        }
        tipoSolicitudRepository.deleteById(id);
    }

    /**
     * Crear un nuevo campus
     */
    @Transactional
    public MaestroDTO crearCampus(MaestroRequest request) {
        log.info("Creando campus: {}", request.getNombre());
        Campus campus = new Campus();
        campus.setId(request.getId());
        campus.setNombre(request.getNombre());
        Campus saved = campusRepository.save(campus);
        return new MaestroDTO(saved.getId().longValue(), null, saved.getNombre());
    }

    /**
     * Actualizar un campus existente
     */
    @Transactional
    public MaestroDTO actualizarCampus(Integer id, MaestroRequest request) {
        log.info("Actualizando campus con id: {}", id);
        Campus campus = campusRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Campus no encontrado con id: " + id));
        campus.setNombre(request.getNombre());
        Campus updated = campusRepository.save(campus);
        return new MaestroDTO(updated.getId().longValue(), null, updated.getNombre());
    }

    /**
     * Eliminar un campus
     */
    @Transactional
    public void eliminarCampus(Integer id) {
        log.info("Eliminando campus con id: {}", id);
        if (!campusRepository.existsById(id)) {
            throw new RuntimeException("Campus no encontrado con id: " + id);
        }
        campusRepository.deleteById(id);
    }

    /**
     * Crear una nueva unidad administrativa
     */
    @Transactional
    public MaestroDTO crearUnidadAdministrativa(MaestroRequest request) {
        log.info("Creando unidad administrativa: {}", request.getNombre());
        UnidadAdministrativa unidadAdministrativa = new UnidadAdministrativa();
        unidadAdministrativa.setId(request.getId());
        unidadAdministrativa.setNombre(request.getNombre());
        UnidadAdministrativa saved = unidadAdministrativaRepository.save(unidadAdministrativa);
        return new MaestroDTO(saved.getId().longValue(), null, saved.getNombre());
    }

    /**
     * Actualizar una unidad administrativa existente
     */
    @Transactional
    public MaestroDTO actualizarUnidadAdministrativa(Integer id, MaestroRequest request) {
        log.info("Actualizando unidad administrativa con id: {}", id);
        UnidadAdministrativa unidadAdministrativa = unidadAdministrativaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Unidad administrativa no encontrada con id: " + id));
        unidadAdministrativa.setNombre(request.getNombre());
        UnidadAdministrativa updated = unidadAdministrativaRepository.save(unidadAdministrativa);
        return new MaestroDTO(updated.getId().longValue(), null, updated.getNombre());
    }

    /**
     * Eliminar una unidad administrativa
     */
    @Transactional
    public void eliminarUnidadAdministrativa(Integer id) {
        log.info("Eliminando unidad administrativa con id: {}", id);
        if (!unidadAdministrativaRepository.existsById(id)) {
            throw new RuntimeException("Unidad administrativa no encontrada con id: " + id);
        }
        unidadAdministrativaRepository.deleteById(id);
    }

    /**
     * Crear una nueva unidad académica
     */
    @Transactional
    public MaestroDTO crearUnidadAcademica(MaestroRequest request) {
        log.info("Creando unidad académica: {}", request.getNombre());
        UnidadAcademica unidadAcademica = new UnidadAcademica();
        unidadAcademica.setId(request.getId());
        unidadAcademica.setNombre(request.getNombre());
        UnidadAcademica saved = unidadAcademicaRepository.save(unidadAcademica);
        return new MaestroDTO(saved.getId().longValue(), null, saved.getNombre());
    }

    /**
     * Actualizar una unidad académica existente
     */
    @Transactional
    public MaestroDTO actualizarUnidadAcademica(Integer id, MaestroRequest request) {
        log.info("Actualizando unidad académica con id: {}", id);
        UnidadAcademica unidadAcademica = unidadAcademicaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Unidad académica no encontrada con id: " + id));
        unidadAcademica.setNombre(request.getNombre());
        UnidadAcademica updated = unidadAcademicaRepository.save(unidadAcademica);
        return new MaestroDTO(updated.getId().longValue(), null, updated.getNombre());
    }

    /**
     * Eliminar una unidad académica
     */
    @Transactional
    public void eliminarUnidadAcademica(Integer id) {
        log.info("Eliminando unidad académica con id: {}", id);
        if (!unidadAcademicaRepository.existsById(id)) {
            throw new RuntimeException("Unidad académica no encontrada con id: " + id);
        }
        unidadAcademicaRepository.deleteById(id);
    }

    /**
     * Crear un nuevo tipo de identificación
     */
    @Transactional
    public MaestroDTO crearTipoIdentificacion(MaestroRequest request) {
        log.info("Creando tipo de identificación: {}", request.getNombre());
        TipoIdentificacion tipoIdentificacion = new TipoIdentificacion();
        tipoIdentificacion.setId(request.getId());
        tipoIdentificacion.setCodigo(request.getCodigo());
        tipoIdentificacion.setNombre(request.getNombre());
        TipoIdentificacion saved = tipoIdentificacionRepository.save(tipoIdentificacion);
        return new MaestroDTO(saved.getId().longValue(), saved.getCodigo(), saved.getNombre());
    }

    /**
     * Actualizar un tipo de identificación existente
     */
    @Transactional
    public MaestroDTO actualizarTipoIdentificacion(Integer id, MaestroRequest request) {
        log.info("Actualizando tipo de identificación con id: {}", id);
        TipoIdentificacion tipoIdentificacion = tipoIdentificacionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tipo de identificación no encontrado con id: " + id));
        tipoIdentificacion.setCodigo(request.getCodigo());
        tipoIdentificacion.setNombre(request.getNombre());
        TipoIdentificacion updated = tipoIdentificacionRepository.save(tipoIdentificacion);
        return new MaestroDTO(updated.getId().longValue(), updated.getCodigo(), updated.getNombre());
    }

    /**
     * Eliminar un tipo de identificación
     */
    @Transactional
    public void eliminarTipoIdentificacion(Integer id) {
        log.info("Eliminando tipo de identificación con id: {}", id);
        if (!tipoIdentificacionRepository.existsById(id)) {
            throw new RuntimeException("Tipo de identificación no encontrado con id: " + id);
        }
        tipoIdentificacionRepository.deleteById(id);
    }

    /**
     * Obtiene lista de tipos de asignación
     */
    @Transactional(readOnly = true)
    public List<MaestroDTO> obtenerTiposAsignacion() {
        log.info("Obteniendo tipos de asignación desde la base de datos");
        return tipoAsignacionRepository.findAll().stream()
            .map(t -> new MaestroDTO(t.getId().longValue(), null, t.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de tipos de servicio
     */
    @Transactional(readOnly = true)
    public List<MaestroDTO> obtenerTiposServicio() {
        log.info("Obteniendo tipos de servicio desde la base de datos");
        return tipoServicioRepository.findAll().stream()
            .filter(t -> !t.getId().equals(TipoServicioEnum.ATENCION_APH.getId()))
            .map(t -> new MaestroDTO(t.getId().longValue(), null, t.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de motivos del estado de la cita
     */
    @Transactional(readOnly = true)
    public List<MaestroDTO> obtenerMotivosEstadoCita() {
        log.info("Obteniendo motivos de estado de cita desde la base de datos");
        return motivoEstadoCitaRepository.findAll().stream()
            .map(m -> new MaestroDTO(m.getId().longValue(), null, m.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de apreciaciones
     */
    @Transactional(readOnly = true)
    public List<MaestroDTO> obtenerApreciaciones() {
        log.info("Obteniendo apreciaciones desde la base de datos");
        return apreciacionRepository.findAll().stream()
            .map(a -> new MaestroDTO(a.getId().longValue(), null, a.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de tipos de apreciación por apreciación ID
     */
    @Transactional(readOnly = true)
    public List<MaestroDTO> obtenerTiposApreciacionPorApreciacionId(Integer apreciacionId) {
        log.info("Obteniendo tipos de apreciación para apreciación ID: {}", apreciacionId);
        return tipoApreciacionRepository.findByApreciacionId(apreciacionId).stream()
            .map(t -> new MaestroDTO(t.getId().longValue(), null, t.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de tipos de ruta de activación
     */
    @Transactional(readOnly = true)
    public List<MaestroDTO> obtenerTiposRutaActivacion() {
        log.info("Obteniendo tipos de ruta de activación desde la base de datos");
        return tipoRutaActivacionRepository.findAll().stream()
            .map(t -> new MaestroDTO(t.getId().longValue(), null, t.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de rutas de activación
     */
    @Transactional(readOnly = true)
    public List<MaestroDTO> obtenerRutasActivacion() {
        log.info("Obteniendo rutas de activación desde la base de datos");
        return rutaActivacionRepository.findAll().stream()
            .map(r -> new MaestroDTO(r.getId().longValue(), null, r.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de tipos de remisión
     */
    @Transactional(readOnly = true)
    public List<MaestroDTO> obtenerTiposRemision() {
        log.info("Obteniendo tipos de remisión desde la base de datos");
        return tipoRemisionRepository.findAll().stream()
            .map(t -> new MaestroDTO(t.getId().longValue(), null, t.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de instancias de remisión
     */
    @Transactional(readOnly = true)
    public List<MaestroDTO> obtenerInstanciasRemision(Integer tipoRemisionId) {
        log.info("Obteniendo instancias de remisión desde la base de datos para tipo: {}", tipoRemisionId);
        List<InstanciaRemision> instancias;
        if (tipoRemisionId != null) {
            instancias = instanciaRemisionRepository.findByTipoRemisionId(tipoRemisionId);
        } else {
            instancias = instanciaRemisionRepository.findAll();
        }
        return instancias.stream()
            .map(i -> new MaestroDTO(i.getId().longValue(), null, i.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de tipos de compromiso
     */
    @Transactional(readOnly = true)
    public List<MaestroDTO> obtenerTiposCompromiso() {
        log.info("Obteniendo tipos de compromiso desde la base de datos");
        return tipoCompromisoRepository.findAll().stream()
            .map(t -> new MaestroDTO(t.getId().longValue(), null, t.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de motivos de estado de seguimiento.
     */
    @Transactional(readOnly = true)
    public List<MaestroDTO> obtenerMotivosEstadoSeguimiento() {
        log.info("Obteniendo motivos de estado de seguimiento desde la base de datos");
        return motivoEstadoSeguimientoRepository.findAll().stream()
            .map(m -> new MaestroDTO(m.getId().longValue(), null, m.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de tipos de seguimiento.
     */
    @Transactional(readOnly = true)
    public List<MaestroDTO> obtenerTiposSeguimiento() {
        log.info("Obteniendo tipos de seguimiento desde la base de datos");
        return tipoSeguimientoRepository.findAll().stream()
            .map(t -> new MaestroDTO(t.getId().longValue(), null, t.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de acciones.
     */
    @Transactional(readOnly = true)
    public List<MaestroDTO> obtenerAcciones() {
        log.info("Obteniendo acciones desde la base de datos");
        return accionRepository.findAll().stream()
            .map(a -> new MaestroDTO(a.getId().longValue(), null, a.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de actividades por acción.
     */
    @Transactional(readOnly = true)
    public List<MaestroDTO> obtenerActividadesPorAccion(Integer accionId) {
        log.info("Obteniendo actividades para acción ID: {}", accionId);
        return actividadRepository.findByAccionIdOrderByNombreAsc(accionId).stream()
            .map(a -> new MaestroDTO(a.getId().longValue(), null, a.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de estados de seguimiento.
     */
    @Transactional(readOnly = true)
    public List<MaestroDTO> obtenerEstadosSeguimiento() {
        log.info("Obteniendo estados de seguimiento desde la base de datos");
        return estadoSeguimientoRepository.findAll().stream()
            .map(e -> new MaestroDTO(e.getId().longValue(), null, e.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de seguimientos de atencion como maestro simplificado.
     */
    @Transactional(readOnly = true)
    public List<MaestroDTO> obtenerSeguimientosAtencion() {
        log.info("Obteniendo seguimientos de atencion desde la base de datos");
        return seguimientoAtencionRepository.findAll().stream()
            .map(s -> new MaestroDTO(s.getId(), s.getAtencion().getId().toString(), s.getDescripcion()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de tipos de medida de protección.
     */
    @Transactional(readOnly = true)
    public List<MaestroDTO> obtenerTiposMedida() {
        log.info("Obteniendo tipos de medida de protección desde la base de datos");
        return tipoMedidaRepository.findAll().stream()
            .map(t -> new MaestroDTO(t.getId().longValue(), null, t.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de subtipos de medida de protección por tipo ID.
     */
    @Transactional(readOnly = true)
    public List<MaestroDTO> obtenerSubTiposMedidaPorTipoId(Integer tipoId) {
        log.info("Obteniendo subtipos de medida de protección para tipo ID: {}", tipoId);
        return subTipoMedidaRepository.findByTipoMedidaId(tipoId).stream()
            .map(s -> new MaestroDTO(s.getId().longValue(), null, s.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de todos los subtipos de medida de protección.
     */
    @Transactional(readOnly = true)
    public List<MaestroDTO> obtenerSubTiposMedida() {
        log.info("Obteniendo todos los subtipos de medida de protección desde la base de datos");
        return subTipoMedidaRepository.findAll().stream()
            .map(s -> new MaestroDTO(s.getId().longValue(), null, s.getNombre()))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene lista de responsables de medida de protección.
     */
    @Transactional(readOnly = true)
    public List<MaestroDTO> obtenerResponsablesMedida() {
        log.info("Obteniendo responsables de medida de protección desde la base de datos");
        return responsableMedidaProteccionRepository.findAll().stream()
            .map(r -> new MaestroDTO(r.getId().longValue(), null, r.getNombre()))
            .collect(Collectors.toList());
    }
}
