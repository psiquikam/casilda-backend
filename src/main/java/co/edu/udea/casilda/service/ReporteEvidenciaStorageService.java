package co.edu.udea.casilda.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Almacena evidencias fuera de la raíz pública de la aplicación.
 */
@Service
@Slf4j
public class ReporteEvidenciaStorageService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final long MAX_REQUEST_SIZE = 25L * 1024 * 1024;
    private static final int MAX_FILES = 5;

    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "application/pdf", "pdf",
            "image/jpeg", "jpg",
            "image/png", "png",
            "video/mp4", "mp4",
            "video/quicktime", "mov",
            "video/3gpp", "3gp");

    @Value("${app.reportes.evidencias.directorio:uploads/reportes}")
    private String directorio;

    /**
     * Valida y almacena una evidencia con un nombre generado por el servidor.
     */
    public StoredEvidence guardar(final MultipartFile archivo) {
        validarArchivo(archivo);
        final String tipoContenido = normalizarTipo(archivo.getContentType());
        final String extension = ALLOWED_TYPES.get(tipoContenido);
        validarFirma(archivo, tipoContenido);
        final String nombreAlmacenado = UUID.randomUUID() + "." + extension;
        try {
            final Path directorioBase = directorioBase();
            Files.createDirectories(directorioBase);
            Files.copy(archivo.getInputStream(), directorioBase.resolve(nombreAlmacenado));
            return new StoredEvidence(nombreAlmacenado, nombreOriginal(archivo), tipoContenido, archivo.getSize());
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible almacenar la evidencia", ex);
        }
    }

    public void eliminar(final String nombreAlmacenado) {
        if (nombreAlmacenado == null || nombreAlmacenado.contains("/")
                || nombreAlmacenado.contains("\\")) {
            return;
        }
        try {
            Files.deleteIfExists(directorioBase().resolve(nombreAlmacenado).normalize());
        } catch (IOException ex) {
            log.error("No fue posible limpiar una evidencia almacenada");
        }
    }

    public void validarCantidadYTamanoTotal(final List<MultipartFile> archivos) {
        if (archivos == null || archivos.isEmpty()) {
            return;
        }
        if (archivos.size() > MAX_FILES) {
            throw new IllegalArgumentException("Se permiten máximo " + MAX_FILES + " evidencias");
        }
        final long total = archivos.stream().mapToLong(MultipartFile::getSize).sum();
        if (total > MAX_REQUEST_SIZE) {
            throw new IllegalArgumentException("Las evidencias superan el tamaño total máximo de 25MB");
        }
    }

    private void validarArchivo(final MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("La evidencia no puede estar vacía");
        }
        if (archivo.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Cada evidencia puede pesar máximo 10MB");
        }
        if (!ALLOWED_TYPES.containsKey(normalizarTipo(archivo.getContentType()))) {
            throw new IllegalArgumentException(
                    "Formato de evidencia no soportado. Se permiten PDF, JPG, PNG, MP4, MOV y 3GP");
        }
    }

    private void validarFirma(final MultipartFile archivo, final String tipoContenido) {
        try (InputStream input = archivo.getInputStream()) {
            final byte[] cabecera = input.readNBytes(12);
            boolean valida = switch (tipoContenido) {
                case "application/pdf" -> comienzaCon(cabecera, new byte[]{'%', 'P', 'D', 'F', '-'});
                case "image/jpeg" -> comienzaCon(cabecera, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
                case "image/png" -> comienzaCon(cabecera,
                        new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
                case "video/mp4", "video/quicktime", "video/3gpp" -> contieneMarcaIsoBmff(cabecera);
                default -> false;
            };
            if (!valida) {
                throw new IllegalArgumentException("El contenido de la evidencia no coincide con su tipo");
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("No fue posible validar la evidencia", ex);
        }
    }

    private boolean comienzaCon(final byte[] actual, final byte[] esperado) {
        if (actual.length < esperado.length) {
            return false;
        }
        for (int i = 0; i < esperado.length; i++) {
            if (actual[i] != esperado[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean contieneMarcaIsoBmff(final byte[] cabecera) {
        if (cabecera.length < 8) {
            return false;
        }
        return cabecera[4] == 'f' && cabecera[5] == 't'
                && cabecera[6] == 'y' && cabecera[7] == 'p';
    }

    private String normalizarTipo(final String tipoContenido) {
        return tipoContenido == null ? "" : tipoContenido.toLowerCase(Locale.ROOT);
    }

    private String nombreOriginal(final MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || nombre.isBlank()) {
            return "evidencia";
        }
        return Paths.get(nombre).getFileName().toString();
    }

    private Path directorioBase() {
        return Paths.get(directorio).toAbsolutePath().normalize();
    }

    public record StoredEvidence(
            String nombreAlmacenado,
            String nombreOriginal,
            String tipoContenido,
            long tamanoBytes) {
    }
}
