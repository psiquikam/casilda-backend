package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.dto.request.FechaCierreCasoRequest;
import co.edu.udea.casilda.dto.response.FechaCierreCasoResponse;
import co.edu.udea.casilda.service.FechaCierreCasoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/** Corrección de la fecha de cierre de casos. */
@RestController
@RequestMapping("/solicitudes/acompanamiento/{solicitudId}/casos")
@RequiredArgsConstructor
@Tag(name = "Casos")
@SecurityRequirement(name = "bearerAuth")
public class FechaCierreCasoController {
    private final FechaCierreCasoService service;

    @PatchMapping("/{casoId}/fecha-cierre")
    @Operation(summary = "Corregir la fecha de cierre de un caso cerrado")
    public ResponseEntity<FechaCierreCasoResponse> actualizar(@PathVariable Long solicitudId,
            @PathVariable Long casoId, @Valid @RequestBody FechaCierreCasoRequest request, Authentication auth) {
        return ResponseEntity.ok(service.actualizar(solicitudId, casoId, request, auth));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> formatoInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", "Ingrese una fecha válida con zona horaria."));
    }
}
