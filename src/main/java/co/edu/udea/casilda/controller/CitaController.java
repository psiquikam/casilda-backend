package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.dto.request.CancelarCitaRequest;
import co.edu.udea.casilda.dto.request.ProgramarCitaSeguimientoRequest;
import co.edu.udea.casilda.dto.request.ReprogramarCitaRequest;
import co.edu.udea.casilda.dto.request.RegistrarResultadoCitaRequest;
import co.edu.udea.casilda.dto.response.CitaResponse;
import co.edu.udea.casilda.service.CitaService;
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
 * Controlador REST para gestión de citas de atención
 */
@RestController
@RequestMapping("/citas")
@RequiredArgsConstructor
@Tag(name = "Citas", description = "API para gestión de citas de atención")
public class CitaController {

    private final CitaService citaService;

    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Listar todas las citas", description = "Retorna todas las citas registradas en el sistema. **Requiere autenticación.**")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de citas obtenida exitosamente")
    })
    public ResponseEntity<List<CitaResponse>> listarCitas() {
        return ResponseEntity.ok(citaService.listarTodasLasCitas());
    }

    @GetMapping("/paginado")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Listar citas paginadas", description = "Retorna citas de forma paginada con filtros opcionales de estado. **Requiere autenticación.**")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de citas obtenida exitosamente")
    })
    public ResponseEntity<Page<CitaResponse>> listarCitasPaginadas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer idEstadoCita,
            @RequestParam(required = false) Integer excluirEstadoCitaId) {
        return ResponseEntity.ok(citaService.listarCitasPaginadas(page, size, idEstadoCita, excluirEstadoCitaId));
    }

    @GetMapping("/pendientes-caso/paginado")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Listar citas iniciales pendientes de caso")
    public ResponseEntity<Page<CitaResponse>> listarCitasPendientesDeCaso(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(citaService.listarCitasPendientesDeCaso(page, size));
    }

    @GetMapping("/caso/{casoId}/historial")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Listar citas asociadas al historial de un caso")
    public ResponseEntity<List<CitaResponse>> listarHistorialAtenciones(@PathVariable Long casoId) {
        return ResponseEntity.ok(citaService.listarHistorialAtenciones(casoId));
    }

    @PostMapping("/caso/{casoId}/seguimientos")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Programar una cita de seguimiento para un caso")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cita de seguimiento programada"),
            @ApiResponse(responseCode = "400", description = "Fecha u hora inválida"),
            @ApiResponse(responseCode = "404", description = "Caso no encontrado")
    })
    public ResponseEntity<CitaResponse> programarSeguimiento(
            @PathVariable Long casoId,
            @Valid @RequestBody ProgramarCitaSeguimientoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(citaService.programarSeguimiento(casoId, request));
    }

    @PutMapping("/{id}/resultado")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Registrar el resultado de una cita")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resultado de cita actualizado"),
            @ApiResponse(responseCode = "400", description = "Resultado inválido"),
            @ApiResponse(responseCode = "404", description = "Cita o estado no encontrado")
    })
    public ResponseEntity<CitaResponse> registrarResultado(
            @PathVariable Long id,
            @Valid @RequestBody RegistrarResultadoCitaRequest request) {
        return ResponseEntity.ok(citaService.registrarResultado(id, request));
    }

    @PutMapping("/{id}/reprogramar")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Reprogramar cita", description = "Actualiza la fecha y hora de una cita. **Requiere autenticación.**")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cita reprogramada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Cita no encontrada")
    })
    public ResponseEntity<CitaResponse> reprogramar(
            @PathVariable Long id,
            @Valid @RequestBody ReprogramarCitaRequest request) {
        return ResponseEntity.ok(citaService.reprogramar(id, request));
    }

    @PutMapping("/{id}/cancelar")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cancelar cita", description = "Marca una cita como Cancelada con su motivo. **Requiere autenticación.**")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cita cancelada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Cita no encontrada")
    })
    public ResponseEntity<CitaResponse> cancelar(
            @PathVariable Long id,
            @Valid @RequestBody CancelarCitaRequest request) {
        return ResponseEntity.ok(citaService.cancelar(id, request));
    }
}
