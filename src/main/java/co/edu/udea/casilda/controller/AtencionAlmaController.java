package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.exception.AtencionAlmaException;
import co.edu.udea.casilda.service.AtencionAlmaService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Gestión de atenciones del nuevo módulo de Línea Alma. */
@RestController
@RequestMapping("/alma/atenciones-aph")
@RequiredArgsConstructor
@Tag(name = "Atenciones APH", description = "Atenciones de Línea Alma")
@SecurityRequirement(name = "bearerAuth")
public class AtencionAlmaController {
    private final AtencionAlmaService service;

    @PostMapping
    @Operation(summary = "Crear borrador de atención APH")
    public ResponseEntity<JsonNode> crear(@Valid @RequestBody JsonNode request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request, auth));
    }

    @GetMapping("/{idAtencion}")
    @Operation(summary = "Consultar atención APH")
    public ResponseEntity<JsonNode> obtener(@PathVariable String idAtencion, Authentication auth) {
        return ResponseEntity.ok(service.obtener(idAtencion, auth));
    }

    @PatchMapping("/{idAtencion}")
    @Operation(summary = "Actualizar parcialmente una atención APH")
    public ResponseEntity<JsonNode> actualizar(@PathVariable String idAtencion,
                                                @Valid @RequestBody JsonNode request,
                                                Authentication auth) {
        return ResponseEntity.ok(service.actualizar(idAtencion, request, auth));
    }

    @PutMapping("/{idAtencion}/triaje")
    @Operation(summary = "Registrar triaje u omisión por peligro inminente")
    public ResponseEntity<JsonNode> registrarTriaje(@PathVariable String idAtencion,
                                                    @Valid @RequestBody JsonNode request,
                                                    Authentication auth) {
        return ResponseEntity.ok(service.registrarTriaje(idAtencion, request, auth));
    }

    @PostMapping("/{idAtencion}/derivaciones/psicologia")
    @Operation(summary = "Derivar una atención a Psicología")
    public ResponseEntity<JsonNode> derivarPsicologia(@PathVariable String idAtencion,
                                                       @Valid @RequestBody JsonNode request,
                                                       Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.derivarPsicologia(idAtencion, request, auth));
    }

    @ExceptionHandler(AtencionAlmaException.class)
    public ResponseEntity<Map<String, Object>> handleAtencionException(AtencionAlmaException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
                "codigo", ex.getCodigo(), "mensaje", ex.getMessage(), "detalles", ex.getDetalles()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidJson(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "codigo", "VALIDACION", "mensaje", "Revise el formato de los datos enviados.",
                "detalles", Map.of()));
    }
}
