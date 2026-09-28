package co.edu.udea.casilda.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gestiona el almacenamiento en disco de las imágenes de los contenidos del
 * Home. Los formatos permitidos son PNG, WEBP y SVG (este último se sanitiza
 * para eliminar cualquier posibilidad de ejecución de scripts). El
 * almacenamiento es local por ahora; en un futuro sprint, con más recursos,
 * se migrará a una solución más robusta (S3, MinIO, etc.) sin necesidad de
 * cambiar el contrato público (el campo `imagen` seguirá siendo una ruta/URL).
 */
@Service
@Slf4j
public class ContenidoImagenStorageService {

    private static final int TAMANIO_MAXIMO_BYTES = 400 * 1024;
    private static final int TAMANIO_RECOMENDADO_BYTES = 300 * 1024;

    private static final Map<String, String> EXTENSIONES_PERMITIDAS = Map.of(
            "image/png", "png",
            "image/webp", "webp",
            "image/svg+xml", "svg");

    private static final Set<String> ETIQUETAS_PELIGROSAS = Set.of(
            "script", "foreignobject", "iframe", "embed", "object");

    @Value("${app.contenidos.imagenes.directorio:uploads/contenidos}")
    private String directorio;

    @Value("${app.contenidos.imagenes.url-base:/contenidos/imagenes}")
    private String urlBase;

    /**
     * Valida, sanitiza (si aplica) y almacena la imagen recibida.
     *
     * @return la ruta pública (relativa) por la que quedará disponible la
     * imagen, para guardar en el campo `imagen` del contenido.
     */
    public String guardar(final MultipartFile archivo) {
        String extension = validarYObtenerExtension(archivo);
        byte[] contenidoArchivo = leerBytes(archivo);

        if ("svg".equals(extension)) {
            contenidoArchivo = sanitizarSvg(contenidoArchivo);
        }

        String nombreArchivo = UUID.randomUUID() + "." + extension;
        escribirArchivo(nombreArchivo, contenidoArchivo);

        return urlBase + "/" + nombreArchivo;
    }

    /**
     * Elimina físicamente el archivo asociado a una ruta pública previamente
     * generada por {@link #guardar}. Es una operación de mejor esfuerzo: si el
     * archivo no existe o la ruta no pertenece a este almacenamiento, no
     * lanza error (evita romper el flujo por inconsistencias de datos legacy).
     */
    public void eliminar(final String rutaPublica) {
        if (!StringUtils.hasText(rutaPublica) || !rutaPublica.startsWith(urlBase + "/")) {
            return;
        }
        String nombreArchivo = rutaPublica.substring((urlBase + "/").length());
        try {
            Files.deleteIfExists(directorioBase().resolve(nombreArchivo));
        } catch (IOException ex) {
            log.warn("No fue posible eliminar la imagen '{}': {}", rutaPublica, ex.getMessage());
        }
    }

    private String validarYObtenerExtension(final MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo de imagen no puede estar vacío");
        }
        if (archivo.getSize() > TAMANIO_MAXIMO_BYTES) {
            throw new IllegalArgumentException(
                    "La imagen supera el tamaño máximo permitido de 400KB");
        }
        if (archivo.getSize() > TAMANIO_RECOMENDADO_BYTES) {
            log.warn("La imagen '{}' supera el tamaño recomendado de 300KB ({} bytes)",
                    archivo.getOriginalFilename(), archivo.getSize());
        }

        String tipoContenido = archivo.getContentType() == null
                ? "" : archivo.getContentType().toLowerCase(Locale.ROOT);
        String extension = EXTENSIONES_PERMITIDAS.get(tipoContenido);
        if (extension == null) {
            throw new IllegalArgumentException(
                    "Formato de imagen no soportado. Solo se permite PNG, WEBP o SVG");
        }
        return extension;
    }

    private byte[] leerBytes(final MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException("No fue posible leer el archivo de imagen", ex);
        }
    }

    private void escribirArchivo(final String nombreArchivo, final byte[] contenido) {
        try {
            Path directorioBase = directorioBase();
            Files.createDirectories(directorioBase);
            Files.write(directorioBase.resolve(nombreArchivo), contenido);
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible almacenar la imagen", ex);
        }
    }

    private Path directorioBase() {
        return Paths.get(directorio);
    }

    /**
     * Sanitiza un SVG eliminando cualquier elemento o atributo capaz de
     * ejecutar script (etiquetas &lt;script&gt;, manejadores de eventos
     * "on*", y referencias con esquema "javascript:"). Se usa un parser XML
     * con procesamiento seguro (sin DTDs ni entidades externas) para evitar
     * ataques XXE.
     */
    private byte[] sanitizarSvg(final byte[] contenidoOriginal) {
        Document documento = parsearSvg(contenidoOriginal);
        Element raiz = documento.getDocumentElement();
        sanitizarAtributos(raiz);
        sanitizarNodo(raiz);
        return serializar(documento);
    }

    private Document parsearSvg(final byte[] contenidoOriginal) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            try (InputStream entrada = new ByteArrayInputStream(contenidoOriginal)) {
                Document documento = builder.parse(entrada);
                Element raiz = documento.getDocumentElement();
                if (raiz == null || !"svg".equalsIgnoreCase(raiz.getLocalName())) {
                    throw new IllegalArgumentException("El archivo SVG no es válido");
                }
                return documento;
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("El archivo SVG no es válido o está corrupto", ex);
        }
    }

    private void sanitizarNodo(final Node nodo) {
        List<Node> hijosAEliminar = new ArrayList<>();
        NodeList hijos = nodo.getChildNodes();
        for (int i = 0; i < hijos.getLength(); i++) {
            Node hijo = hijos.item(i);
            if (hijo.getNodeType() == Node.ELEMENT_NODE) {
                String nombre = hijo.getLocalName() != null
                        ? hijo.getLocalName().toLowerCase(Locale.ROOT)
                        : hijo.getNodeName().toLowerCase(Locale.ROOT);
                if (ETIQUETAS_PELIGROSAS.contains(nombre)) {
                    hijosAEliminar.add(hijo);
                    continue;
                }
                sanitizarAtributos((Element) hijo);
                sanitizarNodo(hijo);
            }
        }
        hijosAEliminar.forEach(nodo::removeChild);
    }

    private void sanitizarAtributos(final Element elemento) {
        List<String> atributosAEliminar = new ArrayList<>();
        var atributos = elemento.getAttributes();
        for (int i = 0; i < atributos.getLength(); i++) {
            Node atributo = atributos.item(i);
            String nombre = atributo.getNodeName().toLowerCase(Locale.ROOT);
            String valor = atributo.getNodeValue() == null
                    ? "" : atributo.getNodeValue().trim().toLowerCase(Locale.ROOT);
            boolean esManejadorEvento = nombre.startsWith("on");
            boolean esReferenciaScript = (nombre.equals("href") || nombre.endsWith(":href"))
                    && valor.startsWith("javascript:");
            if (esManejadorEvento || esReferenciaScript) {
                atributosAEliminar.add(atributo.getNodeName());
            }
        }
        atributosAEliminar.forEach(elemento::removeAttribute);
    }

    private byte[] serializar(final Document documento) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(documento), new StreamResult(salida));
            return salida.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible sanitizar el SVG", ex);
        }
    }
}
