package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.dto.response.RutaExternaAlmaResponse;
import co.edu.udea.casilda.exception.AtencionAlmaException;
import co.edu.udea.casilda.service.RutaExternaAlmaService;
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

/** Gestión de rutas externas asociadas a atenciones APH. */
@RestController
@RequestMapping("/alma/rutas-externas")
@RequiredArgsConstructor
@Tag(name = "Rutas externas APH")
@SecurityRequirement(name = "bearerAuth")
public class RutaExternaAlmaController {
    private final RutaExternaAlmaService service;

    @PostMapping
    @Operation(summary = "Activar una ruta externa")
    public ResponseEntity<RutaExternaAlmaResponse> crear(@Valid @RequestBody JsonNode request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request, auth));
    }

    @GetMapping
    @Operation(summary = "Consultar rutas externas de una atención APH")
    public ResponseEntity<List<RutaExternaAlmaResponse>> listar(
            @RequestParam(required = false) String tipoContexto, @RequestParam(required = false) String idContexto,
            Authentication auth) {
        return ResponseEntity.ok(service.listar(tipoContexto, idContexto, auth));
    }

    @PatchMapping("/{idRuta}")
    @Operation(summary = "Actualizar una ruta externa")
    public ResponseEntity<RutaExternaAlmaResponse> actualizar(@PathVariable String idRuta,
            @Valid @RequestBody JsonNode request, Authentication auth) {
        return ResponseEntity.ok(service.actualizar(idRuta, request, auth));
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
