package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.dto.response.AuditoriaAlmaResponse;
import co.edu.udea.casilda.exception.AtencionAlmaException;
import co.edu.udea.casilda.repository.UsuarioRepository;
import co.edu.udea.casilda.service.AuditoriaAlmaService;
import co.edu.udea.casilda.service.LimiteAuditoriaAlmaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Historial de auditoría de atenciones APH. */
@RestController
@RequestMapping("/alma/auditoria")
@ConditionalOnProperty(name = "app.alma.consulta-previa.habilitada", havingValue = "true")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AuditoriaAlmaController {
    private final AuditoriaAlmaService service;
    private final LimiteAuditoriaAlmaService limite;
    private final UsuarioRepository usuarios;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMA_COORDINACION')")
    @Operation(summary = "Consultar historial inmutable de una atención APH")
    public ResponseEntity<List<AuditoriaAlmaResponse>> consultar(
            @RequestParam(required = false) String tipoContexto,
            @RequestParam(required = false) String idContexto,
            Authentication autenticacion) {
        Long usuarioId = usuarios.findByEmail(autenticacion.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado.")).getId();
        limite.consumir(usuarioId);
        List<AuditoriaAlmaResponse> resultado = service.consultar(tipoContexto, idContexto, autenticacion);
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(resultado);
    }

    @ExceptionHandler(AtencionAlmaException.class)
    public ResponseEntity<Map<String, Object>> error(AtencionAlmaException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
                "codigo", ex.getCodigo(), "mensaje", ex.getMessage(), "detalles", ex.getDetalles()));
    }
}
