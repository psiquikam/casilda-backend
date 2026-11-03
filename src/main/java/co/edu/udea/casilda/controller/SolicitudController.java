package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.dto.request.AsignarSolicitudRequest;
import co.edu.udea.casilda.dto.request.ContactoTelefonicoRequest;
import co.edu.udea.casilda.dto.request.SolicitudAcompanamientoRequest;
import co.edu.udea.casilda.dto.request.UpdateSolicitudRequest;
import co.edu.udea.casilda.dto.response.ContactoTelefonicoResponse;
import co.edu.udea.casilda.dto.response.ProfesionalResponse;
import co.edu.udea.casilda.dto.response.SolicitudAcompanamientoResponse;
import co.edu.udea.casilda.service.SolicitudAcompanamientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
 * Controlador REST para gestión de solicitudes de acompañamiento
 */
@RestController
@RequestMapping("/solicitudes")
@RequiredArgsConstructor
@Tag(name = "Solicitudes", description = "API para gestión de solicitudes de acompañamiento")
public class SolicitudController {

    private final SolicitudAcompanamientoService service;

    /**
     * Crea una nueva solicitud de acompañamiento
     */
    @PostMapping("/acompanamiento")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Crear solicitud de acompañamiento",
            description = "Crea una nueva solicitud de acompañamiento y genera un código único. **Requiere autenticación.**"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Solicitud creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<SolicitudAcompanamientoResponse> crearSolicitud(
            @Valid @RequestBody SolicitudAcompanamientoRequest request) {
        SolicitudAcompanamientoResponse response = service.crearSolicitud(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Obtiene una solicitud por ID
     */
    @GetMapping("/acompanamiento/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Obtener solicitud por ID",
            description = "Obtiene los detalles de una solicitud específica por su ID. **Requiere autenticación.**"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitud encontrada"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
    })
    public ResponseEntity<SolicitudAcompanamientoResponse> obtenerPorId(@PathVariable Long id) {
        SolicitudAcompanamientoResponse response = service.obtenerPorId(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Lista todas las solicitudes
     */
    @GetMapping("/acompanamiento")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Listar todas las solicitudes",
            description = "Obtiene un listado con todas las solicitudes de acompañamiento. **Requiere autenticación.**"
    )
    @ApiResponse(responseCode = "200", description = "Lista de solicitudes obtenida exitosamente")
        public ResponseEntity<List<SolicitudAcompanamientoResponse>> listarTodas(
                        @RequestParam(required = false) Integer idEstadoSolicitud) {
                List<SolicitudAcompanamientoResponse> solicitudes = service.listarTodas(idEstadoSolicitud);
        return ResponseEntity.ok(solicitudes);
    }

        /**
         * Lista solicitudes paginadas
         */
        @GetMapping("/acompanamiento/paginado")
        @SecurityRequirement(name = "bearerAuth")
        @Operation(
                        summary = "Listar solicitudes paginadas",
                        description = "Obtiene solicitudes de acompañamiento en formato paginado. **Requiere autenticación.**"
        )
        @ApiResponse(responseCode = "200", description = "Página de solicitudes obtenida exitosamente")
        public ResponseEntity<Page<SolicitudAcompanamientoResponse>> listarPaginadas(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) Integer idEstadoSolicitud) {
                return ResponseEntity.ok(service.listarPaginadas(page, size, idEstadoSolicitud));
        }

    /**
     * Elimina una solicitud por ID
     */
    @DeleteMapping("/acompanamiento/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Eliminar solicitud",
            description = "Elimina una solicitud de acompañamiento por su ID. **Requiere autenticación.**"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Solicitud eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
    })
    public ResponseEntity<Void> eliminarSolicitud(@PathVariable Long id) {
        service.eliminarSolicitud(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Actualiza los datos de una solicitud
     */
    @PutMapping("/acompanamiento/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Actualizar solicitud",
            description = "Actualiza los datos del solicitante de una solicitud. **Requiere autenticación.**"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitud actualizada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
    })
    public ResponseEntity<SolicitudAcompanamientoResponse> actualizarSolicitud(
            @PathVariable Long id,
                        @Valid @RequestBody UpdateSolicitudRequest request) {
        return ResponseEntity.ok(service.actualizarSolicitud(id, request));
    }

    /**
     * Asigna profesionales a una solicitud
     */
    @PostMapping("/acompanamiento/{id}/asignar")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Asignar solicitud",
            description = "Asigna profesionales a una solicitud y la marca como asignada. **Requiere autenticación.**"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitud asignada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de reparto inválidos"),
            @ApiResponse(responseCode = "404", description = "Solicitud o elemento de catálogo no encontrado")
    })
    public ResponseEntity<SolicitudAcompanamientoResponse> asignarSolicitud(
            @PathVariable Long id,
            @Valid @RequestBody AsignarSolicitudRequest request) {
        return ResponseEntity.ok(service.asignarSolicitud(id, request));
    }

    /**
     * Lista todos los grupos profesionales disponibles
     */
    @GetMapping("/grupos-profesionales")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Listar grupos profesionales",
            description = "Obtiene todos los grupos profesionales disponibles para asignación. **Requiere autenticación.**"
    )
    @ApiResponse(responseCode = "200", description = "Lista de grupos profesionales obtenida exitosamente")
    public ResponseEntity<List<ProfesionalResponse>> listarGruposProfesionales() {
        return ResponseEntity.ok(service.listarGruposProfesionales());
    }

    /**
     * Registra un intento de contacto telefonico para una solicitud
     */
    @PostMapping("/acompanamiento/{id}/contacto")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Registrar contacto telefónico", description = "Registra un intento de contacto telefónico para una solicitud. **Requiere autenticación.**")
    @ApiResponse(responseCode = "201", description = "Contacto registrado exitosamente")
    public ResponseEntity<ContactoTelefonicoResponse> registrarContacto(
            @PathVariable Long id,
            @Valid @RequestBody ContactoTelefonicoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarContacto(id, request));
    }

    /**
     * Lista los intentos de contacto telefonico de una solicitud
     */
    @GetMapping("/acompanamiento/{id}/contactos")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Listar contactos telefónicos", description = "Obtiene el historial de contactos telefónicos de una solicitud. **Requiere autenticación.**")
    @ApiResponse(responseCode = "200", description = "Historial de contactos obtenido exitosamente")
    public ResponseEntity<List<ContactoTelefonicoResponse>> listarContactos(@PathVariable Long id) {
        return ResponseEntity.ok(service.listarContactos(id));
    }

}
