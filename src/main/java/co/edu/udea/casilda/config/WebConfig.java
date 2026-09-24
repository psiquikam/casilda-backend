package co.edu.udea.casilda.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.contenidos.imagenes.directorio:uploads/contenidos}")
    private String directorio;

    @Value("${app.contenidos.imagenes.url-base:/contenidos/imagenes}")
    private String urlBase;

    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        String ubicacion = Paths.get(directorio).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler(urlBase + "/**")
                .addResourceLocations(ubicacion);
    }
}
