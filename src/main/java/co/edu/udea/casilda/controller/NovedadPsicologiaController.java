package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.dto.response.IntentoContactoPsicologiaResponse;
import co.edu.udea.casilda.exception.AtencionAlmaException;
import co.edu.udea.casilda.service.NovedadPsicologiaService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Consulta y gestión del detalle de novedades derivadas a Psicología. */
@RestController
@RequestMapping("/alma/psicologia/novedades")
@RequiredArgsConstructor
@Tag(name = "Novedades Psicología")
@SecurityRequirement(name = "bearerAuth")
public class NovedadPsicologiaController {
    private final NovedadPsicologiaService service;

    @GetMapping("/{idAtencion}")
    @Operation(summary = "Consultar el detalle de una novedad de Psicología")
    public ResponseEntity<JsonNode> obtener(@PathVariable String idAtencion, Authentication auth) {
        return ResponseEntity.ok(service.obtener(idAtencion, auth));
    }

    @GetMapping("/{idAtencion}/intentos")
    @Operation(summary = "Consultar intentos de contacto de una novedad")
    public ResponseEntity<List<IntentoContactoPsicologiaResponse>> listarIntentos(
            @PathVariable String idAtencion, Authentication auth) {
        return ResponseEntity.ok(service.listarIntentos(idAtencion, auth));
    }

    @PostMapping("/{idAtencion}/intentos")
    @Operation(summary = "Registrar un intento de contacto")
    public ResponseEntity<JsonNode> registrarIntento(@PathVariable String idAtencion,
                                                     @RequestBody JsonNode request,
                                                     Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarIntento(idAtencion, request, auth));
    }

    @ExceptionHandler(AtencionAlmaException.class)
    public ResponseEntity<Map<String, Object>> error(AtencionAlmaException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
                "codigo", ex.getCodigo(), "mensaje", ex.getMessage(), "detalles", ex.getDetalles()));
    }
}
