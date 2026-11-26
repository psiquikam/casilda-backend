package co.edu.udea.casilda.service;

import co.edu.udea.casilda.repository.CatalogoAlmaItemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Valida el contenido del primer contacto antes de persistir información clínica. */
@Service
@RequiredArgsConstructor
public class ValidacionPrimerContactoPsicologia {
    private static final Set<String> CAMPOS = Set.of("version", "consentimiento", "persona",
            "valoracionRiesgo", "dimensiones", "presuntoAgresor", "acuerdos", "proximoControl", "vincularA");
    private static final Set<String> CAMPOS_PERSONA = Set.of("primerNombre", "segundoNombre",
            "primerApellido", "segundoApellido", "telefonoPrincipal", "correoPersonal", "ciudad", "estrato");
    private static final Set<String> CAMPOS_AGRESOR = Set.of("nombres", "apellidos", "vinculo",
            "convivencia", "observaciones");
    private final AtencionAlmaService atencionService;
    private final CatalogoAlmaItemRepository catalogos;

    public record Resultado(ObjectNode datos, ObjectNode persona, Instant proximoControl) {}

    @Transactional(readOnly = true)
    public Resultado validar(JsonNode request, ObjectNode datosAtencion, Instant ahora) {
        ObjectNode datos = atencionService.comprobarObjeto(request, "", CAMPOS).deepCopy();
        atencionService.validarConsentimiento(datos.get("consentimiento"), ahora);

        ObjectNode atencion = datosAtencion.deepCopy();
        JsonNode caracterizacion = datos.get("persona");
        if (caracterizacion != null && !caracterizacion.isNull()) {
            ObjectNode persona = atencionService.comprobarObjeto(caracterizacion, "persona", CAMPOS_PERSONA);
            ObjectNode original = atencionService.comprobarObjeto(atencion.get("persona"), "persona",
                    Set.of("tipoIdentificacion", "numeroIdentificacion", "primerNombre", "segundoNombre",
                            "primerApellido", "segundoApellido", "telefonoPrincipal", "correoPersonal",
                            "ciudad", "estrato"));
            original.setAll(persona);
        }
        atencionService.validarTipos(atencion);
        atencionService.validarDatos(atencion, atencion.path("canalIngreso").asText().startsWith("REMISION_"));

        Map<String, String> errores = new LinkedHashMap<>();
        JsonNode riesgo = datos.get("valoracionRiesgo");
        if (riesgo == null || riesgo.isNull()) {
            errores.put("valoracionRiesgo.nivel", "Indique el nivel de riesgo valorado.");
            errores.put("valoracionRiesgo.descripcion", "Describa la valoración.");
        } else {
            ObjectNode valoracion = atencionService.comprobarObjeto(riesgo, "valoracionRiesgo",
                    Set.of("nivel", "descripcion"));
            if (!valoracion.path("nivel").isTextual()
                    || !Set.of("ALTO", "MEDIO", "BAJO").contains(valoracion.path("nivel").asText())) {
                errores.put("valoracionRiesgo.nivel", "Seleccione un nivel de riesgo válido.");
            }
            validarTexto(valoracion.get("descripcion"), "valoracionRiesgo.descripcion", 10, 2000, errores);
        }
        validarTexto(datos.get("acuerdos"), "acuerdos", 10, 2000, errores);

        Instant proximo = null;
        JsonNode control = datos.get("proximoControl");
        if (control != null && control.isTextual()) {
            try {
                proximo = OffsetDateTime.parse(control.asText()).toInstant();
            } catch (DateTimeParseException ex) {
                errores.put("proximoControl", "Ingrese una fecha ISO-8601 con zona horaria.");
            }
        } else {
            errores.put("proximoControl", "Indique la fecha y hora del próximo control.");
        }
        if (proximo != null && !proximo.isAfter(ahora)) {
            errores.put("proximoControl", "El próximo control debe ser una fecha futura.");
        }

        JsonNode dimensiones = datos.get("dimensiones");
        if (dimensiones != null && !dimensiones.isNull()) {
            if (!dimensiones.isArray()) {
                errores.put("dimensiones", "Seleccione dimensiones válidas del catálogo.");
            } else {
                for (JsonNode dimension : dimensiones) {
                    if (!dimension.isTextual() || !activo("dimensiones-salud-mental", dimension.asText())) {
                        errores.put("dimensiones", "Seleccione dimensiones activas del catálogo.");
                        break;
                    }
                }
            }
        }

        JsonNode agresor = datos.get("presuntoAgresor");
        if (agresor != null && !agresor.isNull()) {
            ObjectNode objeto = atencionService.comprobarObjeto(agresor, "presuntoAgresor", CAMPOS_AGRESOR);
            for (String campo : Set.of("nombres", "apellidos", "vinculo", "observaciones")) {
                atencionService.validarTipo(objeto, campo, JsonNode::isTextual, "presuntoAgresor.");
            }
            atencionService.validarTipo(objeto, "convivencia", JsonNode::isBoolean, "presuntoAgresor.");
            String vinculo = objeto.path("vinculo").asText(null);
            if (StringUtils.hasText(vinculo) && !activo("vinculos-agresor", vinculo)) {
                errores.put("presuntoAgresor.vinculo", "Seleccione un vínculo activo del catálogo.");
            }
            for (String campo : Set.of("nombres", "apellidos", "observaciones")) {
                JsonNode texto = objeto.get(campo);
                int maximo = campo.equals("observaciones") ? 2000 : 100;
                if (texto != null && texto.isTextual() && texto.asText().length() > maximo) {
                    errores.put("presuntoAgresor." + campo, "Ingrese máximo " + maximo + " caracteres.");
                }
            }
        }
        atencionService.validarTipo(datos, "vincularA", JsonNode::isTextual, "");
        if (!errores.isEmpty()) {
            throw atencionService.validacion(errores);
        }
        return new Resultado(datos, ((ObjectNode) atencion.get("persona")).deepCopy(), proximo);
    }

    private boolean activo(String catalogo, String codigo) {
        return catalogos.findByCatalogoAndCodigoAndActivoTrue(catalogo, codigo).isPresent();
    }

    private void validarTexto(JsonNode texto, String campo, int minimo, int maximo,
                              Map<String, String> errores) {
        if (texto == null || !texto.isTextual() || texto.asText().trim().length() < minimo
                || texto.asText().length() > maximo) {
            errores.put(campo, "Ingrese entre " + minimo + " y " + maximo + " caracteres.");
        }
    }
}
