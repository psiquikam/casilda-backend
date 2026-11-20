package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.dto.response.DerivacionInternaAlmaResponse;
import co.edu.udea.casilda.exception.AtencionAlmaException;
import co.edu.udea.casilda.service.DerivacionInternaAlmaService;
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

import java.util.List;
import java.util.Map;

/** Registro de derivaciones internas asociadas a atenciones APH. */
@RestController
@RequestMapping("/alma/derivaciones/internas")
@RequiredArgsConstructor
@Tag(name = "Derivaciones internas APH")
@SecurityRequirement(name = "bearerAuth")
public class DerivacionInternaAlmaController {
    private final DerivacionInternaAlmaService service;

    @PostMapping
    @Operation(summary = "Registrar una derivación interna sin entrega automática")
    public ResponseEntity<DerivacionInternaAlmaResponse> crear(@Valid @RequestBody JsonNode request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request, auth));
    }

    @GetMapping
    @Operation(summary = "Consultar derivaciones internas de una atención APH")
    public ResponseEntity<List<DerivacionInternaAlmaResponse>> listar(
            @RequestParam(required = false) String tipoContexto, @RequestParam(required = false) String idContexto,
            Authentication auth) {
        return ResponseEntity.ok(service.listar(tipoContexto, idContexto, auth));
    }

    @ExceptionHandler(AtencionAlmaException.class)
    public ResponseEntity<Map<String, Object>> error(AtencionAlmaException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
                "codigo", ex.getCodigo(), "mensaje", ex.getMessage(), "detalles", ex.getDetalles()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> formatoInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("codigo", "VALIDACION",
                "mensaje", "Revise el formato de los datos enviados.", "detalles", Map.of()));
    }
}
