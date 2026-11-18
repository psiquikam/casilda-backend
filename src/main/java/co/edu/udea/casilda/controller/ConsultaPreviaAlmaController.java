package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.dto.response.ConsultaPreviaAlmaResponse;
import co.edu.udea.casilda.exception.AtencionAlmaException;
import co.edu.udea.casilda.repository.UsuarioRepository;
import co.edu.udea.casilda.service.AuditoriaRecursosService;
import co.edu.udea.casilda.service.ConsultaPreviaAlmaService;
import co.edu.udea.casilda.service.LimiteConsultaPreviaAlmaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta previa APH, habilitable únicamente para pruebas autorizadas. */
@RestController
@RequestMapping("/alma/personas/consulta")
@ConditionalOnProperty(name = "app.alma.consulta-previa.habilitada", havingValue = "true")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ConsultaPreviaAlmaController {
    private final ConsultaPreviaAlmaService service;
    private final LimiteConsultaPreviaAlmaService limite;
    private final UsuarioRepository usuarios;
    private final AuditoriaRecursosService auditoria;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TECNICO_APH', 'REVISOR')")
    @Operation(summary = "Consultar seguimiento y caso cerrado para pruebas APH")
    public ResponseEntity<ConsultaPreviaAlmaResponse> consultar(
            @RequestParam(required = false) String tipoIdentificacion,
            @RequestParam(required = false) String numeroIdentificacion,
            Authentication autenticacion) {
        Long usuarioId = usuarios.findByEmail(autenticacion.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado.")).getId();
        limite.consumir(usuarioId);
        ConsultaPreviaAlmaResponse resultado = service.consultar(tipoIdentificacion, numeroIdentificacion);
        auditoria.registrarConsultaPreviaPersona(autenticacion.getName(),
                resultado.persona() == null ? null : resultado.persona().idPersona());
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(resultado);
    }

    @ExceptionHandler(AtencionAlmaException.class)
    public ResponseEntity<Map<String, Object>> error(AtencionAlmaException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
                "codigo", ex.getCodigo(), "mensaje", ex.getMessage(), "detalles", ex.getDetalles()));
    }
}
