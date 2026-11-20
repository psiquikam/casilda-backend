package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.dto.response.BandejaPsicologiaResponse;
import co.edu.udea.casilda.exception.AtencionAlmaException;
import co.edu.udea.casilda.service.BandejaPsicologiaService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

/** Consulta de novedades derivadas a Psicología. */
@RestController
@RequestMapping("/alma/bandeja/psicologia")
@RequiredArgsConstructor
@Tag(name = "Bandeja Psicología")
@SecurityRequirement(name = "bearerAuth")
public class BandejaPsicologiaController {
    private final BandejaPsicologiaService service;

    @GetMapping
    @Operation(summary = "Consultar novedades de Psicología")
    public ResponseEntity<BandejaPsicologiaResponse> listar(
            @RequestParam(required = false) String estado, @RequestParam(required = false) String triaje,
            @RequestParam(required = false) String semaforo, @RequestParam(required = false) String documento,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        return ResponseEntity.ok(service.listar(estado, triaje, semaforo, documento, page, size, auth));
    }

    @PostMapping("/{idAtencion}/tomar")
    @Operation(summary = "Tomar una novedad de Psicología")
    public ResponseEntity<JsonNode> tomar(@PathVariable String idAtencion, @RequestBody JsonNode request,
                                          Authentication auth) {
        return ResponseEntity.ok(service.tomar(idAtencion, request, auth));
    }

    @PostMapping("/{idAtencion}/liberar")
    @Operation(summary = "Liberar una novedad de Psicología")
    public ResponseEntity<JsonNode> liberar(@PathVariable String idAtencion, @RequestBody JsonNode request,
                                            Authentication auth) {
        return ResponseEntity.ok(service.liberar(idAtencion, request, auth));
    }

    @ExceptionHandler(AtencionAlmaException.class)
    public ResponseEntity<Map<String, Object>> error(AtencionAlmaException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
                "codigo", ex.getCodigo(), "mensaje", ex.getMessage(), "detalles", ex.getDetalles()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> errorTipo(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(Map.of("codigo", "VALIDACION", "mensaje", "Revise los filtros.",
                "detalles", Map.of(ex.getName(), "El valor no tiene el formato esperado.")));
    }
}
