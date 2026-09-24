package co.edu.udea.casilda.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias de ContenidoImagenStorageService: validación de formato y
 * tamaño, sanitización de SVG (remoción de scripts/manejadores de eventos) y
 * borrado de archivos.
 */
class ContenidoImagenStorageServiceTest {

    private static final String URL_BASE = "/contenidos/imagenes";

    @TempDir
    private Path directorioTemporal;

    private ContenidoImagenStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new ContenidoImagenStorageService();
        ReflectionTestUtils.setField(storageService, "directorio", directorioTemporal.toString());
        ReflectionTestUtils.setField(storageService, "urlBase", URL_BASE);
    }

    @Test
    void rechazaArchivoVacio() {
        MockMultipartFile vacio = new MockMultipartFile("imagen", "vacio.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> storageService.guardar(vacio))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaFormatoNoSoportado() {
        MockMultipartFile jpg = new MockMultipartFile("imagen", "foto.jpg", "image/jpeg", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> storageService.guardar(jpg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Formato de imagen no soportado");
    }

    @Test
    void rechazaArchivoQueSuperaElTamanioMaximo() {
        byte[] masDe400Kb = new byte[401 * 1024];
        MockMultipartFile grande = new MockMultipartFile("imagen", "grande.png", "image/png", masDe400Kb);

        assertThatThrownBy(() -> storageService.guardar(grande))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("400KB");
    }

    @Test
    void almacenaPngValidoYRetornaRutaPublica() throws IOException {
        MockMultipartFile png = new MockMultipartFile("imagen", "logo.png", "image/png", new byte[]{1, 2, 3, 4});

        String ruta = storageService.guardar(png);

        assertThat(ruta).startsWith(URL_BASE + "/").endsWith(".png");
        try (Stream<Path> archivos = Files.list(directorioTemporal)) {
            assertThat(archivos).hasSize(1);
        }
    }

    @Test
    void almacenaWebpValidoYRetornaRutaPublica() {
        MockMultipartFile webp = new MockMultipartFile("imagen", "logo.webp", "image/webp", new byte[]{1, 2, 3, 4});

        String ruta = storageService.guardar(webp);

        assertThat(ruta).startsWith(URL_BASE + "/").endsWith(".webp");
    }

    @Test
    void sanitizaSvgEliminandoScriptsYManejadoresDeEventos() throws IOException {
        String svgMalicioso = """
                <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" onload="alert(1)">
                    <script>alert('xss')</script>
                    <circle cx="5" cy="5" r="4" onclick="alert(2)" />
                    <a xlink:href="javascript:alert(3)">enlace</a>
                </svg>
                """;
        MockMultipartFile svg = new MockMultipartFile(
                "imagen", "icono.svg", "image/svg+xml", svgMalicioso.getBytes(StandardCharsets.UTF_8));

        String ruta = storageService.guardar(svg);

        String nombreArchivo = ruta.substring((URL_BASE + "/").length());
        String contenidoAlmacenado = Files.readString(directorioTemporal.resolve(nombreArchivo), StandardCharsets.UTF_8);

        assertThat(contenidoAlmacenado)
                .doesNotContain("<script")
                .doesNotContainIgnoringCase("onload")
                .doesNotContainIgnoringCase("onclick")
                .doesNotContainIgnoringCase("javascript:");
    }

    @Test
    void rechazaSvgInvalido() {
        MockMultipartFile svgInvalido = new MockMultipartFile(
                "imagen", "roto.svg", "image/svg+xml", "esto no es xml".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> storageService.guardar(svgInvalido))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void eliminarBorraElArchivoFisicoDeUnaRutaPropia() throws IOException {
        MockMultipartFile png = new MockMultipartFile("imagen", "logo.png", "image/png", new byte[]{1, 2, 3, 4});
        String ruta = storageService.guardar(png);

        storageService.eliminar(ruta);

        try (Stream<Path> archivos = Files.list(directorioTemporal)) {
            assertThat(archivos).isEmpty();
        }
    }

    @Test
    void eliminarIgnoraRutasQueNoPertenecenAEsteAlmacenamiento() {
        // No debe lanzar excepción aunque la ruta no exista o sea externa.
        storageService.eliminar("/otro/prefijo/archivo.png");
        storageService.eliminar(null);
        storageService.eliminar("");
    }
}
