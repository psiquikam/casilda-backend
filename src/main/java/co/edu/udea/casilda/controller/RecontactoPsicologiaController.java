package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.exception.AtencionAlmaException;
import co.edu.udea.casilda.service.RecontactoPsicologiaService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Consulta y resolución de recontactos de Psicología. */
@RestController
@RequestMapping("/alma/psicologia/recontactos")
@RequiredArgsConstructor
@Tag(name = "Recontactos Psicología")
@SecurityRequirement(name = "bearerAuth")
public class RecontactoPsicologiaController {
    private final RecontactoPsicologiaService servicio;

    @GetMapping
    @Operation(summary = "Consultar recontactos pendientes")
    public ResponseEntity<List<RecontactoPsicologiaService.Recontacto>> listar(Authentication auth) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.listar(auth));
    }

    @GetMapping("/{idAtencion}/resolucion")
    @Operation(summary = "Consultar la resolución clínica de un recontacto")
    public ResponseEntity<RecontactoPsicologiaService.DetalleResolucion> obtener(
            @PathVariable String idAtencion, Authentication auth) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.obtenerResolucion(idAtencion, auth));
    }

    @PostMapping("/{idAtencion}/resolucion")
    @Operation(summary = "Resolver un recontacto")
    public ResponseEntity<RecontactoPsicologiaService.Resolucion> resolver(
            @PathVariable String idAtencion, @RequestBody JsonNode request, Authentication auth) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.resolver(idAtencion, request, auth));
    }

    @ExceptionHandler(AtencionAlmaException.class)
    public ResponseEntity<Map<String, Object>> error(AtencionAlmaException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
                "codigo", ex.getCodigo(), "mensaje", ex.getMessage(), "detalles", ex.getDetalles()));
    }
}
