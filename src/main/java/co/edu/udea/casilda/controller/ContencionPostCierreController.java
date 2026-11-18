package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.dto.response.ContencionPostCierreResponse;
import co.edu.udea.casilda.exception.AtencionAlmaException;
import co.edu.udea.casilda.service.ContencionPostCierreService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Registro de contenciones posteriores al cierre de un caso. */
@RestController
@RequestMapping("/alma/contenciones-post-cierre")
@RequiredArgsConstructor
@Tag(name = "Contenciones post-cierre")
@SecurityRequirement(name = "bearerAuth")
public class ContencionPostCierreController {
    private final ContencionPostCierreService service;

    @PostMapping
    @Operation(summary = "Registrar contención sin reabrir el caso")
    public ResponseEntity<ContencionPostCierreResponse> crear(@Valid @RequestBody JsonNode request,
                                                               Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request, auth));
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
