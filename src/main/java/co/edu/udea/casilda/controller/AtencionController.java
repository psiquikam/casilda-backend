package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.dto.request.*;
import co.edu.udea.casilda.dto.response.AtencionResponse;
import co.edu.udea.casilda.service.AtencionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para gestión de atenciones
 */
@RestController
@RequestMapping("/atenciones")
@RequiredArgsConstructor
@Tag(name = "Atenciones", description = "API para gestión de atenciones y seguimientos")
public class AtencionController {

    private final AtencionService atencionService;

    @GetMapping("/cita/{citaId}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AtencionResponse> obtenerAtencionPorCita(@PathVariable Long citaId) {
        return ResponseEntity.ok(atencionService.obtenerAtencionPorCita(citaId));
    }

    @PostMapping("/pestana/5")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AtencionResponse> registrarPestanaAtencion(
            @Valid @RequestBody PestanaRegistroAtencionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(atencionService.registrarPestanaAtencion(request));
    }

    @PostMapping("/pestana/6")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> registrarPestanaApreciaciones(@Valid @RequestBody PestanaApreciacionesRequest request) {
        atencionService.registrarPestanaApreciaciones(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/pestana/7")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> registrarPestanaAcuerdos(@Valid @RequestBody PestanaAcuerdosRequest request) {
        atencionService.registrarPestanaAcuerdos(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/pestana/8")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> registrarPestanaMedidasProteccion(@Valid @RequestBody PestanaMedidasProteccionRequest request) {
        atencionService.registrarPestanaMedidasProteccion(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/pestana/9")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AtencionResponse> registrarPestanaOtrosCompromisos(
            @Valid @RequestBody PestanaOtrosCompromisosRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(atencionService.registrarPestanaOtrosCompromisos(request));
    }

    @PostMapping("/pestana/10")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> registrarPestanaSeguimientos(@Valid @RequestBody PestanaSeguimientosRequest request) {
        atencionService.registrarPestanaSeguimientos(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/pestana/11")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> registrarPestanaEstadoCaso(@Valid @RequestBody PestanaEstadoCasoRequest request) {
        atencionService.registrarPestanaEstadoCaso(request);
        return ResponseEntity.ok().build();
    }
}
