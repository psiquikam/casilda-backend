package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.dto.response.NotificacionesPsicologiaResponse;
import co.edu.udea.casilda.service.NovedadPsicologiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Contadores para la bandeja de Psicología. */
@RestController
@RequestMapping("/alma/psicologia/notificaciones")
@RequiredArgsConstructor
@Tag(name = "Notificaciones Psicología")
@SecurityRequirement(name = "bearerAuth")
public class NotificacionesPsicologiaController {
    private final NovedadPsicologiaService service;

    @GetMapping
    @Operation(summary = "Consultar contadores de novedades de Psicología")
    public ResponseEntity<NotificacionesPsicologiaResponse> obtener(Authentication auth) {
        return ResponseEntity.ok(service.notificaciones(auth));
    }
}
