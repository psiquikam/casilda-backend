package co.edu.udea.casilda.controller.uad;

import co.edu.udea.casilda.dto.response.uad.UadResponses;
import co.edu.udea.casilda.service.uad.UadMisReportesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * «Mis reportes» de la persona usuaria y respuesta a la solicitud de información de la UAD (Entrega 3 de
 * {@code openapi-uad.yaml}). La ruta autoriza el rol; {@link UadMisReportesService} filtra por dueño: un reporte
 * ajeno responde 404. Respuestas con {@code Cache-Control: no-store}.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "UAD — Mis reportes", description = "Reportes propios y respuesta a la subsanación")
public class UadMisReportesController {

    private static final int TAMANO_MAXIMO = 50;

    private final UadMisReportesService service;

    @GetMapping("/mis-reportes")
    @Operation(operationId = "listarMisReportes", summary = "Reportes del usuario autenticado que llegaron a la UAD")
    public ResponseEntity<Page<UadResponses.MiReporteResumen>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        final PageRequest pagina = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), TAMANO_MAXIMO),
                Sort.by(Sort.Direction.DESC, "fechaRecepcion"));
        return ResponseEntity.ok().cacheControl(UadNoticiaController.SIN_CACHE).body(service.listar(pagina));
    }

    @GetMapping("/mis-reportes/{codigo}")
    @Operation(operationId = "obtenerMiReporte", summary = "Detalle de un reporte propio con su solicitud de información (404 si es ajeno)")
    public ResponseEntity<UadResponses.MiReporteDetalle> obtener(@PathVariable String codigo) {
        return ResponseEntity.ok().cacheControl(UadNoticiaController.SIN_CACHE).body(service.obtener(codigo));
    }

    @PostMapping(path = "/reportes/{codigo}/solicitudes-informacion/{solicitudId}/respuesta", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(operationId = "responderSolicitudInformacion",
            summary = "Responder una solicitud de información (solo texto por ahora; la noticia pasa a SUBSANACION_RECIBIDA)")
    public ResponseEntity<UadResponses.MiReporteDetalle> responder(
            @PathVariable String codigo,
            @PathVariable UUID solicitudId,
            @RequestParam(required = false) String respuesta,
            @RequestParam(required = false) List<MultipartFile> evidencias) {
        final boolean conEvidencias = evidencias != null && evidencias.stream().anyMatch(e -> e != null && !e.isEmpty());
        return ResponseEntity.ok().cacheControl(UadNoticiaController.SIN_CACHE).body(service.responder(codigo, solicitudId, respuesta, conEvidencias));
    }
}
