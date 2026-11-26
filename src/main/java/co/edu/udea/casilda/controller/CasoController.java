package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.dto.request.*;
import co.edu.udea.casilda.dto.response.CasoResponse;
import co.edu.udea.casilda.dto.response.CasoListResponse;
import co.edu.udea.casilda.dto.response.AtencionResponse;
import co.edu.udea.casilda.service.CasoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para gestión de casos
 */
@RestController
@RequestMapping("/casos")
@RequiredArgsConstructor
@Tag(name = "Casos", description = "API para gestión de casos y registro demográfico")
public class CasoController {

    private final CasoService casoService;

    @GetMapping("/paginado")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Listar casos paginados", description = "Retorna casos de forma paginada. **Requiere autenticación.**")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de casos obtenida exitosamente")
    })
    public ResponseEntity<Page<CasoListResponse>> listarCasosPaginados(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(casoService.listarCasosPaginados(page, size));
    }

    @GetMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Consultar un caso por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Caso obtenido exitosamente"),
            @ApiResponse(responseCode = "404", description = "Caso no encontrado")
    })
    public ResponseEntity<CasoListResponse> obtenerCasoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(casoService.obtenerCasoPorId(id));
    }

    @PostMapping("/pestana/0")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CasoResponse> registrarPestanaDatosPersona(
            @Valid @RequestBody PestanaDatosPersonaRequest request) {
        if (request.getIdCaso() == null) {
            throw new IllegalArgumentException("El caso se crea al registrar la primera atención con consentimiento.");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(casoService.registrarPestanaDatosPersona(request));
    }

    @PostMapping("/pestana/1")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> registrarPestanaDatosComplementarios(
            @Valid @RequestBody PestanaDatosComplementariosRequest request) {
        casoService.registrarPestanaDatosComplementarios(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/pestana/2")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AtencionResponse> registrarPestanaDocumentacion(
            @Valid @RequestBody PestanaDocumentacionRequest request) {
        casoService.registrarPestanaDocumentacion(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/pestana/3")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> registrarPestana3(@Valid @RequestBody PestanaVBGRequest request) {
        casoService.registrarPestanaVBG(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/pestana/4")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> registrarPestanaPresuntoAgresor(
            @Valid @RequestBody PestanaPresuntoAgresorRequest request) {
        casoService.registrarPestanaPresuntoAgresor(request);
        return ResponseEntity.ok().build();
    }
}
