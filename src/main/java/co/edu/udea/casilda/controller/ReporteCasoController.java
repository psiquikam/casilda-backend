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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
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

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Crear reporte de caso con evidencias",
            description = "Recibe la parte JSON `reporte` y archivos opcionales en `evidencias`.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reporte creado"),
            @ApiResponse(responseCode = "400", description = "Datos o evidencias inválidos"),
            @ApiResponse(responseCode = "401", description = "Token inválido o vencido")
    })
    public ResponseEntity<ReporteCasoResponse> crearConEvidencias(
            @Valid @RequestPart("reporte") final ReporteCasoRequest request,
            @RequestPart(value = "evidencias", required = false)
            final java.util.List<MultipartFile> evidencias) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore().cachePrivate().sMaxAge(0, TimeUnit.SECONDS))
                .body(reporteCasoService.crear(request,
                        evidencias == null ? java.util.List.of() : evidencias));
    }
}
