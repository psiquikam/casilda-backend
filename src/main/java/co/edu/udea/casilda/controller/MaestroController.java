package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.dto.request.MaestroRequest;
import co.edu.udea.casilda.dto.response.MaestroDTO;
import co.edu.udea.casilda.service.MaestroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Controlador REST para maestros
 * Proporciona endpoints para obtener datos de maestros del sistema
 */
@RestController
@RequestMapping("/maestros")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Maestros", description = "Endpoints para obtener datos de maestros")
public class MaestroController {

    private final MaestroService catalogoService;

    @GetMapping("/paises")
    @Operation(summary = "Obtener lista de países")
    public ResponseEntity<List<MaestroDTO>> obtenerPaises() {
        return ResponseEntity.ok(catalogoService.obtenerPaises());
    }

    @GetMapping("/sexos")
    @Operation(summary = "Obtener lista de sexos")
    public ResponseEntity<List<MaestroDTO>> obtenerSexos() {
        return ResponseEntity.ok(catalogoService.obtenerSexos());
    }

    @GetMapping("/tipos-identificacion")
    @Operation(summary = "Obtener lista de tipos de identificación")
    public ResponseEntity<List<MaestroDTO>> obtenerTiposIdentificacion() {
        return ResponseEntity.ok(catalogoService.obtenerTiposIdentificacion());
    }

    @GetMapping("/etnias")
    @Operation(summary = "Obtener lista de etnias")
    public ResponseEntity<List<MaestroDTO>> obtenerEtnias() {
        return ResponseEntity.ok(catalogoService.obtenerEtnias());
    }

    @GetMapping("/identidades-genero")
    @Operation(summary = "Obtener lista de identidades de género")
    public ResponseEntity<List<MaestroDTO>> obtenerIdentidadesGenero() {
        return ResponseEntity.ok(catalogoService.obtenerIdentidadesGenero());
    }

    @GetMapping("/orientaciones-sexuales")
    @Operation(summary = "Obtener lista de orientaciones sexuales")
    public ResponseEntity<List<MaestroDTO>> obtenerOrientacionesSexuales() {
        return ResponseEntity.ok(catalogoService.obtenerOrientacionesSexuales());
    }

    @GetMapping("/tipo-discapacidad")
    @Operation(summary = "Obtener lista de tipos de discapacidad")
    public ResponseEntity<List<MaestroDTO>> obtenerTipoDiscapacidad() {
        return ResponseEntity.ok(catalogoService.obtenerTipoDiscapacidad());
    }

    @GetMapping("/subtipo-discapacidad/{tipoId}")
    @Operation(summary = "Obtener lista de subtipos de discapacidad por tipo ID")
    public ResponseEntity<List<MaestroDTO>> obtenerSubTipoDiscapacidadPorTipoId(@PathVariable Integer tipoId) {
        return ResponseEntity.ok(catalogoService.obtenerSubTipoDiscapacidadPorTipoId(tipoId));
    }

    @GetMapping("/departamentos")
    @Operation(summary = "Obtener lista de departamentos")
    public ResponseEntity<List<MaestroDTO>> obtenerDepartamentos() {
        return ResponseEntity.ok(catalogoService.obtenerDepartamentos());
    }

    @GetMapping("/municipios")
    @Operation(summary = "Obtener lista de municipios")
    public ResponseEntity<List<MaestroDTO>> obtenerMunicipios() {
        return ResponseEntity.ok(catalogoService.obtenerMunicipios());
    }

    @GetMapping("/ciudades")
    @Operation(summary = "Obtener lista de ciudades")
    public ResponseEntity<List<MaestroDTO>> obtenerCiudades() {
        return ResponseEntity.ok(catalogoService.obtenerMunicipios());
    }

    @GetMapping("/departamentos/{departamentoId}/ciudades")
    @Operation(summary = "Obtener lista de ciudades por departamento")
    public ResponseEntity<List<MaestroDTO>> obtenerCiudadesPorDepartamento(@PathVariable Integer departamentoId) {
        return ResponseEntity.ok(catalogoService.obtenerMunicipiosPorDepartamento(departamentoId));
    }

    @GetMapping("/departamentos/codigo/{codigoDepartamento}/ciudades")
    @Operation(summary = "Obtener lista de ciudades por codigo de departamento")
    public ResponseEntity<List<MaestroDTO>> obtenerCiudadesPorCodigoDepartamento(
            @PathVariable String codigoDepartamento) {
        return ResponseEntity.ok(catalogoService.obtenerMunicipiosPorCodigoDepartamento(codigoDepartamento));
    }

    @GetMapping("/campus")
    @Operation(summary = "Obtener lista de campus")
    public ResponseEntity<List<MaestroDTO>> obtenerCampus() {
        return ResponseEntity.ok(catalogoService.obtenerCampus());
    }

    @GetMapping("/unidades-administrativas")
    @Operation(summary = "Obtener lista de unidades administrativas")
    public ResponseEntity<List<MaestroDTO>> obtenerUnidadesAdministrativas() {
        return ResponseEntity.ok(catalogoService.obtenerUnidadesAdministrativas());
    }

    @GetMapping("/unidades-academicas")
    @Operation(summary = "Obtener lista de unidades académicas/escuelas/institutos")
    public ResponseEntity<List<MaestroDTO>> obtenerUnidadesAcademicas() {
        return ResponseEntity.ok(catalogoService.obtenerUnidadesAcademicas());
    }

    @GetMapping("/roles")
    @Operation(summary = "Obtener lista de roles")
    public ResponseEntity<List<MaestroDTO>> obtenerRoles() {
        return ResponseEntity.ok(catalogoService.obtenerRoles());
    }

    @GetMapping("/vinculos-agresor-victima")
    @Operation(summary = "Obtener lista de vínculos agresor-víctima")
    public ResponseEntity<List<MaestroDTO>> obtenerVinculosAgresorVictima() {
        return ResponseEntity.ok(catalogoService.obtenerVinculosAgresorVictima());
    }

    @GetMapping("/vinculos-udea")
    @Operation(summary = "Obtener lista de vínculos Universidad")
    public ResponseEntity<List<MaestroDTO>> obtenerVinculosUdeA() {
        return ResponseEntity.ok(catalogoService.obtenerVinculosUdeA());
    }



    @GetMapping("/formas-ocurrencia")
    @Operation(summary = "Obtener lista de formas de ocurrencia")
    public ResponseEntity<List<MaestroDTO>> obtenerFormasOcurrencia() {
        return ResponseEntity.ok(catalogoService.obtenerFormasOcurrencia());
    }

    @GetMapping("/lugares-ocurrencia")
    @Operation(summary = "Obtener lista de lugares de ocurrencia")
    public ResponseEntity<List<MaestroDTO>> obtenerLugaresOcurrencia() {
        return ResponseEntity.ok(catalogoService.obtenerLugaresOcurrencia());
    }

    @GetMapping("/actividades-misionales")
    @Operation(summary = "Obtener lista de actividades misionales")
    public ResponseEntity<List<MaestroDTO>> obtenerActividadesMisionales() {
        return ResponseEntity.ok(catalogoService.obtenerActividadesMisionales());
    }

    @GetMapping("/tipos-violencia")
    @Operation(summary = "Obtener lista de tipos de violencia")
    public ResponseEntity<List<MaestroDTO>> obtenerTiposViolencia() {
        return ResponseEntity.ok(catalogoService.obtenerTiposViolencia());
    }

    @GetMapping("/modalidades-violencia")
    @Operation(summary = "Obtener lista de modalidades de violencia")
    public ResponseEntity<List<MaestroDTO>> obtenerModalidadesViolencia() {
        return ResponseEntity.ok(catalogoService.obtenerModalidadesViolencia());
    }

    @GetMapping("/modalidades-violencia/tipo/{tipoViolenciaId}")
    @Operation(summary = "Obtener lista de modalidades de violencia por tipo")
    public ResponseEntity<List<MaestroDTO>> obtenerModalidadesViolenciaPorTipo(@PathVariable Integer tipoViolenciaId) {
        return ResponseEntity.ok(catalogoService.obtenerModalidadesViolenciaPorTipo(tipoViolenciaId));
    }

    @GetMapping("/modalidades-violencia-sexual")
    @Operation(summary = "Obtener lista de modalidades de violencia sexual")
    public ResponseEntity<List<MaestroDTO>> obtenerModalidadesViolenciaSexual() {
        return ResponseEntity.ok(catalogoService.obtenerModalidadesViolenciaSexual());
    }

    @GetMapping("/cargos")
    @Operation(summary = "Obtener lista de cargos")
    public ResponseEntity<List<MaestroDTO>> obtenerCargos() {
        return ResponseEntity.ok(catalogoService.obtenerCargos());
    }

    @GetMapping("/tipos-solicitud")
    @Operation(summary = "Obtener lista de tipos de solicitud")
    public ResponseEntity<List<MaestroDTO>> obtenerTiposSolicitud() {
        return ResponseEntity.ok(catalogoService.obtenerTiposSolicitud());
    }

    @GetMapping("/medio-solicitud")
    @Operation(summary = "Obtener lista de opciones para medio de solicitud")
    public ResponseEntity<List<MaestroDTO>> obtenerMedioSolicitud() {
        return ResponseEntity.ok(catalogoService.obtenerMedioSolicitud());
    }

    @GetMapping("/tiempos-ocurrido-unidad")
    @Operation(summary = "Obtener lista de opciones para la unidad del tiempo ocurrido")
    public ResponseEntity<List<MaestroDTO>> obtenerTiemposOcurridoUnidad() {
        return ResponseEntity.ok(catalogoService.obtenerTiemposOcurridoUnidad());
    }

    @GetMapping("/catalogos/{catalogo}/paginado")
    @Operation(summary = "Obtener catálogo paginado")
    public ResponseEntity<Page<MaestroDTO>> obtenerCatalogoPaginado(
            @PathVariable String catalogo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(catalogoService.obtenerCatalogoPaginado(catalogo, page, size));
    }

    @GetMapping("/programas")
    @Operation(summary = "Obtener lista de programas académicos, opcionalmente filtrados por unidad académica y si es pregrado/posgrado")
    public ResponseEntity<List<MaestroDTO>> obtenerProgramas(
            @RequestParam(required = false) Integer unidadAcademicaId,
            @RequestParam(required = false, defaultValue = "true") Boolean pregrado) {
        if (unidadAcademicaId != null) {
            return ResponseEntity.ok(catalogoService.obtenerProgramasPorUnidad(unidadAcademicaId, pregrado));
        }
        return ResponseEntity.ok(catalogoService.obtenerProgramas());
    }


    @GetMapping("/resultados-contacto-telefonico")
    @Operation(summary = "Obtener lista de resultados de contacto telefónico")
    public ResponseEntity<List<MaestroDTO>> obtenerResultadosContactoTelefonico() {
        return ResponseEntity.ok(catalogoService.obtenerResultadosContactoTelefonico());
    }

    @GetMapping("/regimenes")
    @Operation(summary = "Obtener lista de regímenes de salud")
    public ResponseEntity<List<MaestroDTO>> obtenerRegimenes() {
        return ResponseEntity.ok(catalogoService.obtenerRegimenes());
    }

    @GetMapping("/eps")
    @Operation(summary = "Obtener lista de EPS")
    public ResponseEntity<List<MaestroDTO>> obtenerEPS() {
        return ResponseEntity.ok(catalogoService.obtenerEPS());
    }

    @GetMapping("/grupos-atencion")
    @Operation(summary = "Obtener lista de grupos de atención")
    public ResponseEntity<List<MaestroDTO>> obtenerGruposAtencion() {
        return ResponseEntity.ok(catalogoService.obtenerGruposAtencion());
    }

    @GetMapping("/estados-atencion")
    @Operation(summary = "Obtener lista de estados de atención")
    public ResponseEntity<List<MaestroDTO>> obtenerEstadosAtencion() {
        return ResponseEntity.ok(catalogoService.obtenerEstadosAtencion());
    }

    @GetMapping("/estados-caso")
    @Operation(summary = "Obtener lista de estados de caso")
    public ResponseEntity<List<MaestroDTO>> obtenerEstadosCaso() {
        return ResponseEntity.ok(catalogoService.obtenerEstadosCaso());
    }

    @GetMapping("/tipos-correo")
    @Operation(summary = "Obtener lista de tipos de correo")
    public ResponseEntity<List<MaestroDTO>> obtenerTiposCorreo() {
        return ResponseEntity.ok(catalogoService.obtenerTiposCorreo());
    }

    @GetMapping("/tipos-telefono")
    @Operation(summary = "Obtener lista de tipos de teléfono")
    public ResponseEntity<List<MaestroDTO>> obtenerTiposTelefono() {
        return ResponseEntity.ok(catalogoService.obtenerTiposTelefono());
    }

    @GetMapping("/tipos-reporte-alma")
    @Operation(summary = "Obtener lista de tipos de reporte ALMA")
    public ResponseEntity<List<MaestroDTO>> obtenerTiposReporteAlma() {
        return ResponseEntity.ok(catalogoService.obtenerTiposReporteAlma());
    }

    @GetMapping("/canales-contacto")
    @Operation(summary = "Obtener lista de canales de contacto")
    public ResponseEntity<List<MaestroDTO>> obtenerCanalesContacto() {
        return ResponseEntity.ok(catalogoService.obtenerCanalesContacto());
    }

    @GetMapping("/lugares-entrevista")
    @Operation(summary = "Obtener lista de lugares de entrevista")
    public ResponseEntity<List<MaestroDTO>> obtenerLugaresEntrevista() {
        return ResponseEntity.ok(catalogoService.obtenerLugaresEntrevista());
    }


    @GetMapping("/protocolos-aph")
    @Operation(summary = "Obtener lista de protocolos APH")
    public ResponseEntity<List<MaestroDTO>> obtenerProtocolosAph() {
        return ResponseEntity.ok(catalogoService.obtenerProtocolosAph());
    }

    @GetMapping("/resultados-triage")
    @Operation(summary = "Obtener lista de resultados de triage")
    public ResponseEntity<List<MaestroDTO>> obtenerResultadosTriage() {
        return ResponseEntity.ok(catalogoService.obtenerResultadosTriage());
    }

    @GetMapping("/tipos-asignacion")
    @Operation(summary = "Obtener lista de tipos de asignación")
    public ResponseEntity<List<MaestroDTO>> obtenerTiposAsignacion() {
        return ResponseEntity.ok(catalogoService.obtenerTiposAsignacion());
    }

    @GetMapping("/tipos-servicio")
    @Operation(summary = "Obtener lista de tipos de servicio")
    public ResponseEntity<List<MaestroDTO>> obtenerTiposServicio() {
        return ResponseEntity.ok(catalogoService.obtenerTiposServicio());
    }

    @GetMapping("/motivos-estado-cita")
    @Operation(summary = "Obtener lista de motivos del estado de la cita")
    public ResponseEntity<List<MaestroDTO>> obtenerMotivosEstadoCita() {
        return ResponseEntity.ok(catalogoService.obtenerMotivosEstadoCita());
    }

    @GetMapping("/apreciaciones")
    @Operation(summary = "Obtener lista de apreciaciones")
    public ResponseEntity<List<MaestroDTO>> obtenerApreciaciones() {
        return ResponseEntity.ok(catalogoService.obtenerApreciaciones());
    }

    @GetMapping("/tipos-apreciacion/{apreciacionId}")
    @Operation(summary = "Obtener lista de tipos de apreciación por apreciación ID")
    public ResponseEntity<List<MaestroDTO>> obtenerTiposApreciacionPorApreciacionId(
            @PathVariable Integer apreciacionId) {
        return ResponseEntity.ok(catalogoService.obtenerTiposApreciacionPorApreciacionId(apreciacionId));
    }

    @GetMapping("/tipos-ruta-activacion")
    @Operation(summary = "Obtener lista de tipos de ruta de activación")
    public ResponseEntity<List<MaestroDTO>> obtenerTiposRutaActivacion() {
        return ResponseEntity.ok(catalogoService.obtenerTiposRutaActivacion());
    }

    @GetMapping("/rutas-activacion")
    @Operation(summary = "Obtener lista de rutas de activación")
    public ResponseEntity<List<MaestroDTO>> obtenerRutasActivacion() {
        return ResponseEntity.ok(catalogoService.obtenerRutasActivacion());
    }

    @GetMapping("/rutas-internas")
    @Operation(summary = "Obtener rutas internas para Equipo de Atención")
    public ResponseEntity<List<MaestroDTO>> obtenerRutasInternas() {
        return ResponseEntity.ok(catalogoService.obtenerRutasEquipoAtencion("INTERNA"));
    }

    @GetMapping("/rutas-externas")
    @Operation(summary = "Obtener rutas externas para Equipo de Atención")
    public ResponseEntity<List<MaestroDTO>> obtenerRutasExternas() {
        return ResponseEntity.ok(catalogoService.obtenerRutasEquipoAtencion("EXTERNA"));
    }

    @GetMapping("/tipos-remision")
    @Operation(summary = "Obtener lista de tipos de remisión")
    public ResponseEntity<List<MaestroDTO>> obtenerTiposRemision() {
        return ResponseEntity.ok(catalogoService.obtenerTiposRemision());
    }

    @GetMapping("/instancias-remision")
    @Operation(summary = "Obtener lista de instancias de remisión por tipo")
    public ResponseEntity<List<MaestroDTO>> obtenerInstanciasRemision(@RequestParam(required = false) Integer tipoRemisionId) {
        return ResponseEntity.ok(catalogoService.obtenerInstanciasRemision(tipoRemisionId));
    }

    @GetMapping("/tipos-compromiso")
    @Operation(summary = "Obtener lista de tipos de compromiso")
    public ResponseEntity<List<MaestroDTO>> obtenerTiposCompromiso() {
        return ResponseEntity.ok(catalogoService.obtenerTiposCompromiso());
    }

    @GetMapping("/motivos-estado-seguimiento")
    @Operation(summary = "Obtener lista de motivos de estado de seguimiento")
    public ResponseEntity<List<MaestroDTO>> obtenerMotivosEstadoSeguimiento() {
        return ResponseEntity.ok(catalogoService.obtenerMotivosEstadoSeguimiento());
    }

    @GetMapping("/tipos-seguimiento")
    @Operation(summary = "Obtener lista de tipos de seguimiento")
    public ResponseEntity<List<MaestroDTO>> obtenerTiposSeguimiento() {
        return ResponseEntity.ok(catalogoService.obtenerTiposSeguimiento());
    }

    @GetMapping("/acciones")
    @Operation(summary = "Obtener lista de acciones")
    public ResponseEntity<List<MaestroDTO>> obtenerAcciones() {
        return ResponseEntity.ok(catalogoService.obtenerAcciones());
    }

    @GetMapping("/actividades/por-accion/{accionId}")
    @Operation(summary = "Obtener lista de actividades por acción")
    public ResponseEntity<List<MaestroDTO>> obtenerActividadesPorAccion(@PathVariable Integer accionId) {
        return ResponseEntity.ok(catalogoService.obtenerActividadesPorAccion(accionId));
    }

    @GetMapping("/estados-seguimiento")
    @Operation(summary = "Obtener lista de estados de seguimiento")
    public ResponseEntity<List<MaestroDTO>> obtenerEstadosSeguimiento() {
        return ResponseEntity.ok(catalogoService.obtenerEstadosSeguimiento());
    }

    @GetMapping("/seguimientos-atencion")
    @Operation(summary = "Obtener lista de seguimientos de atencion")
    public ResponseEntity<List<MaestroDTO>> obtenerSeguimientosAtencion() {
        return ResponseEntity.ok(catalogoService.obtenerSeguimientosAtencion());
    }
    // ==================== ENDPOINTS CRUD PARA MAESTROS ====================

    // TIPOS DE SOLICITUD
    @PostMapping("/tipos-solicitud")
    @Operation(summary = "Crear un nuevo tipo de solicitud")
    public ResponseEntity<MaestroDTO> crearTipoSolicitud(@Valid @RequestBody MaestroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.crearTipoSolicitud(request));
    }

    @PutMapping("/tipos-solicitud/{id}")
    @Operation(summary = "Actualizar un tipo de solicitud existente")
    public ResponseEntity<MaestroDTO> actualizarTipoSolicitud(
            @PathVariable Integer id,
            @Valid @RequestBody MaestroRequest request) {
        return ResponseEntity.ok(catalogoService.actualizarTipoSolicitud(id, request));
    }

    @DeleteMapping("/tipos-solicitud/{id}")
    @Operation(summary = "Eliminar un tipo de solicitud")
    public ResponseEntity<Void> eliminarTipoSolicitud(@PathVariable Integer id) {
        catalogoService.eliminarTipoSolicitud(id);
        return ResponseEntity.noContent().build();
    }

    // CAMPUS
    @PostMapping("/campus")
    @Operation(summary = "Crear un nuevo campus")
    public ResponseEntity<MaestroDTO> crearCampus(@Valid @RequestBody MaestroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.crearCampus(request));
    }

    @PutMapping("/campus/{id}")
    @Operation(summary = "Actualizar un campus existente")
    public ResponseEntity<MaestroDTO> actualizarCampus(
            @PathVariable Integer id,
            @Valid @RequestBody MaestroRequest request) {
        return ResponseEntity.ok(catalogoService.actualizarCampus(id, request));
    }

    @DeleteMapping("/campus/{id}")
    @Operation(summary = "Eliminar un campus")
    public ResponseEntity<Void> eliminarCampus(@PathVariable Integer id) {
        catalogoService.eliminarCampus(id);
        return ResponseEntity.noContent().build();
    }

    // UNIDADES ADMINISTRATIVAS
    @PostMapping("/unidades-administrativas")
    @Operation(summary = "Crear una nueva unidad administrativa")
    public ResponseEntity<MaestroDTO> crearUnidadAdministrativa(@Valid @RequestBody MaestroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.crearUnidadAdministrativa(request));
    }

    @PutMapping("/unidades-administrativas/{id}")
    @Operation(summary = "Actualizar una unidad administrativa existente")
    public ResponseEntity<MaestroDTO> actualizarUnidadAdministrativa(
            @PathVariable Integer id,
            @Valid @RequestBody MaestroRequest request) {
        return ResponseEntity.ok(catalogoService.actualizarUnidadAdministrativa(id, request));
    }

    @DeleteMapping("/unidades-administrativas/{id}")
    @Operation(summary = "Eliminar una unidad administrativa")
    public ResponseEntity<Void> eliminarUnidadAdministrativa(@PathVariable Integer id) {
        catalogoService.eliminarUnidadAdministrativa(id);
        return ResponseEntity.noContent().build();
    }

    // UNIDADES ACADÉMICAS
    @PostMapping("/unidades-academicas")
    @Operation(summary = "Crear una nueva unidad académica/escuela/instituto")
    public ResponseEntity<MaestroDTO> crearUnidadAcademica(@Valid @RequestBody MaestroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.crearUnidadAcademica(request));
    }

    @PutMapping("/unidades-academicas/{id}")
    @Operation(summary = "Actualizar una unidad académica existente")
    public ResponseEntity<MaestroDTO> actualizarUnidadAcademica(
            @PathVariable Integer id,
            @Valid @RequestBody MaestroRequest request) {
        return ResponseEntity.ok(catalogoService.actualizarUnidadAcademica(id, request));
    }

    @DeleteMapping("/unidades-academicas/{id}")
    @Operation(summary = "Eliminar una unidad académica")
    public ResponseEntity<Void> eliminarUnidadAcademica(@PathVariable Integer id) {
        catalogoService.eliminarUnidadAcademica(id);
        return ResponseEntity.noContent().build();
    }

    // TIPOS DE IDENTIFICACIÓN
    @PostMapping("/tipos-identificacion")
    @Operation(summary = "Crear un nuevo tipo de identificación")
    public ResponseEntity<MaestroDTO> crearTipoIdentificacion(@Valid @RequestBody MaestroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.crearTipoIdentificacion(request));
    }

    @PutMapping("/tipos-identificacion/{id}")
    @Operation(summary = "Actualizar un tipo de identificación existente")
    public ResponseEntity<MaestroDTO> actualizarTipoIdentificacion(
            @PathVariable Integer id,
            @Valid @RequestBody MaestroRequest request) {
        return ResponseEntity.ok(catalogoService.actualizarTipoIdentificacion(id, request));
    }

    @DeleteMapping("/tipos-identificacion/{id}")
    @Operation(summary = "Eliminar un tipo de identificación")
    public ResponseEntity<Void> eliminarTipoIdentificacion(@PathVariable Integer id) {
        catalogoService.eliminarTipoIdentificacion(id);
        return ResponseEntity.noContent().build();
    }

    // MEDIDAS DE PROTECCIÓN MAESTROS
    @GetMapping("/tipos-medida")
    @Operation(summary = "Obtener lista de tipos de medida de protección")
    public ResponseEntity<List<MaestroDTO>> obtenerTiposMedida() {
        return ResponseEntity.ok(catalogoService.obtenerTiposMedida());
    }

    @GetMapping("/subtipos-medida")
    @Operation(summary = "Obtener todos los subtipos de medida de protección")
    public ResponseEntity<List<MaestroDTO>> obtenerSubTiposMedida() {
        return ResponseEntity.ok(catalogoService.obtenerSubTiposMedida());
    }

    @GetMapping("/subtipos-medida/{tipoId}")
    @Operation(summary = "Obtener lista de subtipos de medida de protección por tipo ID")
    public ResponseEntity<List<MaestroDTO>> obtenerSubTiposMedidaPorTipoId(@PathVariable Integer tipoId) {
        return ResponseEntity.ok(catalogoService.obtenerSubTiposMedidaPorTipoId(tipoId));
    }

    @GetMapping("/responsables-medida")
    @Operation(summary = "Obtener lista de responsables de medida de protección")
    public ResponseEntity<List<MaestroDTO>> obtenerResponsablesMedida() {
        return ResponseEntity.ok(catalogoService.obtenerResponsablesMedida());
    }

    @GetMapping("/actores-remitentes")
    @Operation(summary = "Obtener lista de actores remitentes")
    public ResponseEntity<List<MaestroDTO>> obtenerActoresRemitentes() {
        return ResponseEntity.ok(catalogoService.obtenerActoresRemitentes());
    }

}
