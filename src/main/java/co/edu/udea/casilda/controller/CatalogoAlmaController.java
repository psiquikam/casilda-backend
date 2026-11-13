package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.dto.response.CatalogoAlmaItemResponse;
import co.edu.udea.casilda.service.CatalogoAlmaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Catálogos del nuevo módulo Línea Alma. */
@RestController
@RequestMapping("/alma/catalogos")
@RequiredArgsConstructor
@Tag(name = "Catálogos ALMA")
@SecurityRequirement(name = "bearerAuth")
public class CatalogoAlmaController {
    private final CatalogoAlmaService catalogoAlmaService;

    @GetMapping("/{nombre}")
    @Operation(summary = "Consultar un catálogo de Línea Alma")
    public ResponseEntity<List<CatalogoAlmaItemResponse>> obtener(@PathVariable String nombre) {
        return ResponseEntity.ok(catalogoAlmaService.obtenerCatalogo(nombre));
    }
}
