package co.edu.udea.casilda.controller.uad;

import co.edu.udea.casilda.dto.request.uad.ExpedienteRequest;
import co.edu.udea.casilda.dto.request.uad.RegistrarActuacionesRequest;
import co.edu.udea.casilda.dto.request.uad.RemisionCompetenciaCasoRequest;
import co.edu.udea.casilda.dto.request.uad.TrasladoRequest;
import co.edu.udea.casilda.dto.response.uad.UadResponses;
import co.edu.udea.casilda.model.enums.uad.FaseUad;
import co.edu.udea.casilda.service.uad.UadCasoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static co.edu.udea.casilda.controller.uad.UadNoticiaController.SIN_CACHE;
import static co.edu.udea.casilda.controller.uad.UadNoticiaController.uuid;

/**
 * Bandeja de casos y «Gestionar caso» (Entrega 2 de {@code openapi-uad.yaml}). La ruta se autoriza por rol en la
 * tabla {@code endpoint}; el equipo, la fase y el estado los valida {@link UadCasoService}. Las acciones responden
 * 200 con {@code ResultadoAccionCaso}. Todas las respuestas van con {@code Cache-Control: no-store}.
 */
@RestController
@RequestMapping("/uad/casos")
@RequiredArgsConstructor
@Tag(name = "UAD — Casos", description = "Bandeja de casos y gestión del caso (Equipos 3 y 4)")
public class UadCasoController {

    private static final int TAMANO_MAXIMO = 50;

    private final UadCasoService service;

    @GetMapping
    @Operation(operationId = "listarCasosUad", summary = "Casos activos del equipo del usuario en su fase actual")
    public ResponseEntity<Page<UadResponses.CasoUadResumen>> listar(
            @RequestParam(required = false) FaseUad fase,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        final PageRequest pagina = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), TAMANO_MAXIMO),
                Sort.by(Sort.Direction.DESC, "fechaAdmision"));
        return ResponseEntity.ok().cacheControl(SIN_CACHE).body(service.listar(fase, pagina));
    }

    @GetMapping("/{casoUadId}")
    @Operation(operationId = "obtenerCasoUad", summary = "Detalle del caso (no cambia fase ni estado)")
    public ResponseEntity<UadResponses.CasoUadDetalle> obtener(@PathVariable String casoUadId) {
        return ResponseEntity.ok().cacheControl(SIN_CACHE).body(service.obtener(uuid(casoUadId)));
    }

    @PatchMapping("/{casoUadId}/expediente")
    @Operation(operationId = "registrarExpedienteUad", summary = "Registrar o corregir el número de expediente UAD")
    public ResponseEntity<UadResponses.ResultadoAccionCaso> expediente(
            @PathVariable String casoUadId, @Valid @RequestBody ExpedienteRequest cuerpo) {
        return ResponseEntity.ok().cacheControl(SIN_CACHE).body(service.registrarExpediente(uuid(casoUadId), cuerpo));
    }

    @GetMapping("/{casoUadId}/actuaciones/catalogo")
    @Operation(operationId = "catalogoActuaciones", summary = "Actuaciones ordinarias y acciones permanentes para la fase y el equipo actuales")
    public ResponseEntity<List<UadResponses.ActuacionDisponible>> catalogo(@PathVariable String casoUadId) {
        return ResponseEntity.ok().cacheControl(SIN_CACHE).body(service.catalogoActuaciones(uuid(casoUadId)));
    }

    @PostMapping("/{casoUadId}/actuaciones")
    @Operation(operationId = "registrarActuaciones", summary = "Guardar actuaciones ordinarias (no cambian la fase)")
    public ResponseEntity<UadResponses.ResultadoAccionCaso> actuaciones(
            @PathVariable String casoUadId, @Valid @RequestBody RegistrarActuacionesRequest cuerpo) {
        return ResponseEntity.ok().cacheControl(SIN_CACHE).body(service.registrarActuaciones(uuid(casoUadId), cuerpo));
    }

    @GetMapping("/{casoUadId}/historial")
    @Operation(operationId = "historialCaso", summary = "Historial append-only del caso")
    public ResponseEntity<List<UadResponses.EventoHistorial>> historial(@PathVariable String casoUadId) {
        return ResponseEntity.ok().cacheControl(SIN_CACHE).body(service.historial(uuid(casoUadId)));
    }

    @PostMapping("/{casoUadId}/traslado-decision")
    @Operation(operationId = "finalizarInstruccion", summary = "Finalizar la instrucción y trasladar al equipo decisor")
    public ResponseEntity<UadResponses.ResultadoAccionCaso> traslado(
            @PathVariable String casoUadId, @Valid @RequestBody TrasladoRequest cuerpo) {
        return ResponseEntity.ok().cacheControl(SIN_CACHE).body(service.finalizarInstruccion(uuid(casoUadId), cuerpo));
    }

    @PostMapping("/{casoUadId}/remisiones-competencia")
    @Operation(operationId = "remitirCasoPorCompetencia", summary = "Remisión por competencia: finaliza la gestión UAD, no el caso CASILDA")
    public ResponseEntity<UadResponses.ResultadoAccionCaso> remision(
            @PathVariable String casoUadId, @Valid @RequestBody RemisionCompetenciaCasoRequest cuerpo) {
        return ResponseEntity.ok().cacheControl(SIN_CACHE).body(service.remitirPorCompetencia(uuid(casoUadId), cuerpo));
    }

    @GetMapping("/{casoUadId}/evidencias")
    @Operation(operationId = "evidenciasCaso", summary = "Metadatos de las pruebas del caso (carga deshabilitada, D-UAD-09)")
    public ResponseEntity<List<UadResponses.EvidenciaMetadato>> evidencias(@PathVariable String casoUadId) {
        return ResponseEntity.ok().cacheControl(SIN_CACHE).body(service.evidencias(uuid(casoUadId)));
    }
}
