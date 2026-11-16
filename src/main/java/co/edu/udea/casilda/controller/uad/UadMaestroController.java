package co.edu.udea.casilda.controller.uad;

import co.edu.udea.casilda.dto.response.MaestroDTO;
import co.edu.udea.casilda.model.enums.uad.CatalogoUad;
import co.edu.udea.casilda.service.uad.UadCatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Catálogos UAD bajo {@code /maestros/uad-*}, con la forma de {@code MaestroDTO} del resto de maestros. */
@RestController
@RequestMapping("/maestros")
@RequiredArgsConstructor
@Tag(name = "UAD — Catálogos", description = "Causales, entidades de remisión y tipos de solicitud (pendientes de validación)")
public class UadMaestroController {

    private final UadCatalogoService service;

    @GetMapping("/uad-causales-inhibitorio")
    @Operation(operationId = "catalogoCausalesInhibitorio", summary = "Causales de decisión inhibitoria")
    public ResponseEntity<List<MaestroDTO>> causales() {
        return ResponseEntity.ok(service.listar(CatalogoUad.CAUSAL_INHIBITORIO));
    }

    @GetMapping("/uad-entidades-remision")
    @Operation(operationId = "catalogoEntidadesRemision", summary = "Entidades destinatarias de la remisión por competencia")
    public ResponseEntity<List<MaestroDTO>> entidades() {
        return ResponseEntity.ok(service.listar(CatalogoUad.ENTIDAD_REMISION));
    }

    @GetMapping("/uad-tipos-solicitud-informacion")
    @Operation(operationId = "catalogoTiposSolicitudInformacion", summary = "Tipos de solicitud de información adicional")
    public ResponseEntity<List<MaestroDTO>> tiposSolicitud() {
        return ResponseEntity.ok(service.listar(CatalogoUad.TIPO_SOLICITUD_INFORMACION));
    }
}
