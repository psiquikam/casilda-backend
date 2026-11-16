package co.edu.udea.casilda.controller.uad;

import co.edu.udea.casilda.dto.request.uad.ClasificacionUadRequest;
import co.edu.udea.casilda.dto.request.uad.DecisionEvaluacionRequest;
import co.edu.udea.casilda.dto.response.uad.UadResponses;
import co.edu.udea.casilda.exception.UadException;
import co.edu.udea.casilda.model.enums.uad.EstadoNoticia;
import co.edu.udea.casilda.model.enums.uad.TipoNoticia;
import co.edu.udea.casilda.service.uad.UadNoticiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Bandejas NDI/NDA y evaluación preliminar UAD (Entrega 1 de {@code openapi-uad.yaml}). Las rutas se
 * autorizan por rol en la tabla {@code endpoint}; el equipo, la visibilidad y el estado los valida
 * {@link UadNoticiaService}. Todas las respuestas con datos reservados van con {@code Cache-Control: no-store}.
 */
@RestController
@RequestMapping("/uad")
@RequiredArgsConstructor
@Tag(name = "UAD — Noticias", description = "Bandejas NDI/NDA y evaluación preliminar (Equipos 3 y 4)")
public class UadNoticiaController {

    private static final int TAMANO_MAXIMO = 50;
    private static final CacheControl SIN_CACHE = CacheControl.noStore().cachePrivate();

    private final UadNoticiaService service;

    @GetMapping("/contexto")
    @Operation(operationId = "obtenerContextoUad", summary = "Equipos y capacidades UAD del usuario autenticado")
    public ResponseEntity<UadResponses.ContextoUsuario> contexto() {
        return ResponseEntity.ok().cacheControl(SIN_CACHE).body(service.contexto());
    }

    @GetMapping("/noticias")
    @Operation(operationId = "listarNoticias", summary = "Bandeja NDI o NDA filtrada por competencia del equipo")
    public ResponseEntity<Page<UadResponses.NoticiaResumen>> listar(
            @RequestParam TipoNoticia tipo,
            @RequestParam(required = false) EstadoNoticia estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaRecepcionDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaRecepcionHasta,
            @RequestParam(required = false) String consecutivo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        final PageRequest pagina = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), TAMANO_MAXIMO),
                Sort.by(Sort.Direction.DESC, "fechaRecepcion"));
        return ResponseEntity.ok().cacheControl(SIN_CACHE)
                .body(service.listar(tipo, estado, fechaRecepcionDesde, fechaRecepcionHasta, consecutivo, pagina));
    }

    @GetMapping("/noticias/{noticiaId}")
    @Operation(operationId = "obtenerNoticia", summary = "Detalle para evaluación preliminar (no cambia estado ni crea casoId)")
    public ResponseEntity<UadResponses.NoticiaDetalle> obtener(@PathVariable String noticiaId) {
        return ResponseEntity.ok().cacheControl(SIN_CACHE).body(service.obtener(uuid(noticiaId)));
    }

    @PostMapping("/noticias/{noticiaId}/clasificacion")
    @Operation(operationId = "clasificarVinculacion", summary = "Clasificar o reclasificar la vinculación del presunto responsable")
    public ResponseEntity<UadResponses.Clasificacion> clasificar(
            @PathVariable String noticiaId, @Valid @RequestBody ClasificacionUadRequest cuerpo) {
        return ResponseEntity.ok().cacheControl(SIN_CACHE).body(service.clasificar(uuid(noticiaId), cuerpo));
    }

    @GetMapping("/noticias/{noticiaId}/casos-asociados")
    @Operation(operationId = "consultarCasosAsociados", summary = "Posibles casos de la persona en otros actores (no vincula)")
    public ResponseEntity<List<UadResponses.CasoAsociado>> casosAsociados(@PathVariable String noticiaId) {
        return ResponseEntity.ok().cacheControl(SIN_CACHE).body(service.casosAsociados(uuid(noticiaId)));
    }

    @PostMapping("/noticias/{noticiaId}/decisiones")
    @Operation(operationId = "registrarDecisionEvaluacion", summary = "Registrar la decisión de la evaluación preliminar (200, no 201)")
    public ResponseEntity<UadResponses.DecisionEvaluacion> decidir(
            @PathVariable String noticiaId, @Valid @RequestBody DecisionEvaluacionRequest cuerpo) {
        return ResponseEntity.ok().cacheControl(SIN_CACHE).body(service.decidir(uuid(noticiaId), cuerpo));
    }

    @GetMapping("/noticias/{noticiaId}/historial")
    @Operation(operationId = "historialNoticia", summary = "Historial append-only de la noticia")
    public ResponseEntity<List<UadResponses.EventoHistorial>> historial(@PathVariable String noticiaId) {
        return ResponseEntity.ok().cacheControl(SIN_CACHE).body(service.historial(uuid(noticiaId)));
    }

    @GetMapping("/evidencias/{evidenciaId}/descarga")
    @Operation(operationId = "descargarEvidencia", summary = "Descarga autorizada y registrada de una evidencia")
    public ResponseEntity<org.springframework.core.io.Resource> descargar(@PathVariable String evidenciaId) {
        final Long id;
        try {
            id = Long.valueOf(evidenciaId);
        } catch (NumberFormatException ex) {
            throw UadException.noEncontrado();
        }
        final UadNoticiaService.Descarga d = service.descargarEvidencia(id);
        return ResponseEntity.ok()
                .cacheControl(SIN_CACHE)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(d.nombre(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(d.tipo()))
                .body(d.recurso());
    }

    /** Un ID mal formado responde 404 igual que uno inexistente. */
    private static UUID uuid(final String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException ex) {
            throw UadException.noEncontrado();
        }
    }
}
