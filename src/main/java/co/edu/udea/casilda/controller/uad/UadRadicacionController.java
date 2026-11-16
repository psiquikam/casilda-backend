package co.edu.udea.casilda.controller.uad;

import co.edu.udea.casilda.dto.request.uad.RadicacionNdaRequest;
import co.edu.udea.casilda.dto.response.uad.UadResponses;
import co.edu.udea.casilda.service.uad.UadRadicacionService;
import io.swagger.v3.oas.annotations.Operation;
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

/**
 * Queja anónima ante la UAD (NDA). Ruta <strong>pública</strong> ({@code publico = true} en la tabla
 * {@code endpoint}). El servicio no lee la sesión: aunque llegue un token, la noticia no se asocia a nadie.
 *
 * <p>PENDIENTE (D-UAD-29): límite de envíos por origen (429). No se implementó para no procesar la IP en
 * la aplicación sin una política aprobada; puede hacerse en el proxy.</p>
 */
@RestController
@RequestMapping("/uad/noticias-anonimas")
@RequiredArgsConstructor
@Tag(name = "UAD — Radicación", description = "Queja anónima (NDA)")
public class UadRadicacionController {

    private final UadRadicacionService service;

    @PostMapping
    @Operation(operationId = "radicarNoticiaAnonima", summary = "Radicar una queja anónima (201 con el consecutivo NDA)")
    public ResponseEntity<UadResponses.RadicacionNda> radicar(@Valid @RequestBody RadicacionNdaRequest cuerpo) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(service.radicarNda(cuerpo));
    }
}
