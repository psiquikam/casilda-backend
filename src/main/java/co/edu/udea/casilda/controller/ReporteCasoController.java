package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.dto.request.ReporteCasoRequest;
import co.edu.udea.casilda.dto.response.ReporteCasoResponse;
import co.edu.udea.casilda.service.ReporteCasoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * Controlador REST para crear reportes iniciales de caso.
 */
@RestController
@RequestMapping("/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "API para creación de reportes de caso")
public class ReporteCasoController {

    private final ReporteCasoService reporteCasoService;

    @PostMapping
    @Operation(summary = "Crear reporte de caso",
            description = "Crea un reporte anónimo o asociado a un JWT válido. La ruta queda solicitada.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reporte creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Token inválido o vencido"),
            @ApiResponse(responseCode = "429", description = "Límite de tasa excedido")
    })
    public ResponseEntity<ReporteCasoResponse> crear(
            @Valid @RequestBody final ReporteCasoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore().cachePrivate().sMaxAge(0, TimeUnit.SECONDS))
                .body(reporteCasoService.crear(request));
    }
}
