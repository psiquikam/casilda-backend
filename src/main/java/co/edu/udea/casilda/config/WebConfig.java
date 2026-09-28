package co.edu.udea.casilda.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Expone las imágenes almacenadas en disco (ver
 * {@link co.edu.udea.casilda.service.ContenidoImagenStorageService}) como
 * recursos estáticos públicos. El endpoint queda registrado igualmente en el
 * registro dinámico de endpoint/endpointrole como público, ya que la
 * autorización aplica sobre cualquier ruta, incluidas las de recursos
 * estáticos.
 */
@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.contenidos.imagenes.directorio:uploads/contenidos}")
    private String directorio;

    @Value("${app.contenidos.imagenes.url-base:/contenidos/imagenes}")
    private String urlBase;

    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        registry.addResourceHandler(urlBase + "/**")
                .addResourceLocations(ubicacionConSlashFinal());
    }

    /**
     * {@link Path#toUri()} solo agrega el '/' final cuando el directorio ya
     * existe en el sistema de archivos; si aún no existe (por ejemplo, en un
     * entorno recién desplegado o tras una limpieza), el recurso estático
     * queda mal resuelto y cualquier subruta responde 404. Por eso se crea
     * el directorio de antemano y, de todas formas, se fuerza el slash final
     * de forma explícita.
     */
    private String ubicacionConSlashFinal() {
        Path directorioBase = Paths.get(directorio).toAbsolutePath().normalize();
        try {
            Files.createDirectories(directorioBase);
        } catch (IOException ex) {
            log.warn("No fue posible crear el directorio de imágenes '{}': {}",
                    directorioBase, ex.getMessage());
        }
        String uri = directorioBase.toUri().toString();
        return uri.endsWith("/") ? uri : uri + "/";
    }
}
