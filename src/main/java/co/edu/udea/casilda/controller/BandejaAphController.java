package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.exception.AtencionAlmaException;
import co.edu.udea.casilda.service.BandejaAphService;
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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

/** Gestión de remisiones y bandeja del equipo APH. */
@RestController
@RequestMapping("/alma")
@RequiredArgsConstructor
@Tag(name = "Bandeja APH")
@SecurityRequirement(name = "bearerAuth")
public class BandejaAphController {
    private final BandejaAphService service;

    @PostMapping("/remisiones-aph")
    @Operation(summary = "Remitir una persona a Línea Alma")
    public ResponseEntity<JsonNode> remitir(@Valid @RequestBody JsonNode request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.remitir(request, auth));
    }

    @GetMapping("/bandeja/aph")
    @Operation(summary = "Consultar remisiones o mis atenciones")
    public ResponseEntity<Map<String, Object>> listar(@RequestParam(defaultValue = "remisiones") String vista,
            @RequestParam(required = false) String origen, @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        return ResponseEntity.ok(service.listar(vista, origen, estado, page, size, auth));
    }

    @PostMapping("/bandeja/aph/{idAtencion}/tomar")
    @Operation(summary = "Tomar una remisión pendiente")
    public ResponseEntity<JsonNode> tomar(@PathVariable String idAtencion, @Valid @RequestBody JsonNode request,
                                          Authentication auth) {
        return ResponseEntity.ok(service.tomar(idAtencion, request, auth));
    }

    @ExceptionHandler(AtencionAlmaException.class)
    public ResponseEntity<Map<String, Object>> error(AtencionAlmaException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
                "codigo", ex.getCodigo(), "mensaje", ex.getMessage(), "detalles", ex.getDetalles()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> formatoInvalido(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("codigo", "VALIDACION",
                "mensaje", "Revise el formato de los datos enviados.", "detalles", Map.of()));
    }
}
