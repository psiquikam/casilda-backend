package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.dto.request.ContenidoRequest;
import co.edu.udea.casilda.dto.response.ContenidoHomeResponse;
import co.edu.udea.casilda.dto.response.ContenidoResponse;
import co.edu.udea.casilda.service.ContenidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Controlador REST para los contenidos destacados del Home: la consulta
 * pública (sin autenticación) y el CRUD administrativo del gestor de
 * contenidos.
 */
@RestController
@RequestMapping("/contenidos")
@RequiredArgsConstructor
@Tag(name = "Contenidos", description = "API para gestión de contenidos destacados del Home")
public class ContenidoController {

    private final ContenidoService service;

    /**
     * Endpoint público consumido por el Home antes del login. Devuelve un
     * arreglo plano (sin envoltorio de paginación) con los contenidos
     * vigentes al instante de la petición.
     */
    @GetMapping("/home")
    @Operation(
            summary = "Obtener contenidos vigentes del Home",
            description = "Devuelve los contenidos destacados vigentes para la landing pública. **No requiere autenticación.**"
    )
    @ApiResponse(responseCode = "200", description = "Lista de contenidos vigentes (puede ser vacía)")
    public ResponseEntity<List<ContenidoHomeResponse>> obtenerContenidosHome() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .body(service.listarVigentesHome());
    }

    /**
     * Lista paginada de todos los contenidos (vigentes, futuros y expirados)
     * para la tabla de administración.
     */
    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Listar contenidos paginados",
            description = "Obtiene los contenidos administrables en formato paginado. **Requiere autenticación.**"
    )
    @ApiResponse(responseCode = "200", description = "Página de contenidos obtenida exitosamente")
    public ResponseEntity<Page<ContenidoResponse>> listarPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.listarPaginado(page, size));
    }

    @GetMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Obtener contenido por ID", description = "**Requiere autenticación.**")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contenido encontrado"),
            @ApiResponse(responseCode = "404", description = "Contenido no encontrado")
    })
    public ResponseEntity<ContenidoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Crear contenido",
            description = "Crea un nuevo contenido destacado del Home. Recibe `multipart/form-data`: la parte "
                    + "`contenido` con el JSON de los datos y, opcionalmente, la parte `imagen` con el archivo "
                    + "(PNG, WEBP o SVG, máximo 400KB). **Requiere autenticación.**"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Contenido creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada o imagen inválidos")
    })
    public ResponseEntity<ContenidoResponse> crear(
            @RequestPart("contenido") @Valid ContenidoRequest request,
            @RequestPart(value = "imagen", required = false) MultipartFile imagen) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request, imagen));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Actualizar contenido",
            description = "Actualiza un contenido destacado existente. Recibe `multipart/form-data`: la parte "
                    + "`contenido` con el JSON de los datos y, opcionalmente, la parte `imagen` con un archivo nuevo "
                    + "(reemplaza la actual) o el parámetro `eliminarImagen=true` para quitarla sin reemplazo. Si no "
                    + "se envía ninguno de los dos, la imagen actual se conserva. **Requiere autenticación.**"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contenido actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada o imagen inválidos"),
            @ApiResponse(responseCode = "404", description = "Contenido no encontrado")
    })
    public ResponseEntity<ContenidoResponse> actualizar(
            @PathVariable Long id,
            @RequestPart("contenido") @Valid ContenidoRequest request,
            @RequestPart(value = "imagen", required = false) MultipartFile imagen,
            @RequestParam(value = "eliminarImagen", defaultValue = "false") boolean eliminarImagen) {
        return ResponseEntity.ok(service.actualizar(id, request, imagen, eliminarImagen));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Eliminar contenido",
            description = "Elimina lógicamente un contenido destacado (nunca físicamente). **Requiere autenticación.**"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Contenido eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Contenido no encontrado")
    })
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
