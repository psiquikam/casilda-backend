package co.edu.udea.casilda.service.uad;

import co.edu.udea.casilda.dto.request.uad.DecisionEvaluacionRequest;
import co.edu.udea.casilda.model.enums.uad.ModoCaso;
import co.edu.udea.casilda.model.enums.uad.TipoNoticia;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Reglas cruzadas de la decisión de evaluación preliminar (UAD-EVA-02 a 12, UAD-REM-01 a 06).
 * Mismas reglas y rutas de campo que {@code validarDecision} del frontend; aquí son las que valen.
 */
public final class UadDecisionValidator {

    public static final int LIMITE_EXPEDIENTE = 40;
    public static final int MINIMO_INFORMACION = 10;

    /**
     * PROVISIONAL (D-UAD-13): obligatoria porque así aparece en el prototipo; la bitácora no lo define y
     * la inhibitoria está en sus pendientes jurídicos (§16).
     */
    public static final boolean JUSTIFICACION_INHIBITORIO_OBLIGATORIA = true;

    private UadDecisionValidator() {
    }

    /** Contexto que conoce el servidor al validar. Los catálogos se verifican con predicados. */
    public record Contexto(
            TipoNoticia tipoNoticia,
            String casoIdActual,
            Collection<String> casosAsociados,
            boolean tieneReportanteIdentificado,
            boolean vinculacionDeterminada,
            LocalDate hoy,
            Predicate<String> tipoSolicitudValido,
            Predicate<String> causalValida,
            Predicate<String> entidadValida
    ) {
    }

    /** Errores por campo con la ruta del cuerpo ({@code solicitudInformacion.fechaLimite}). Vacío = válida. */
    public static Map<String, String> validar(final DecisionEvaluacionRequest c, final Contexto ctx) {
        final Map<String, String> e = new LinkedHashMap<>();
        final long bloques = java.util.stream.Stream.of(c.continuarTramite(), c.solicitudInformacion(), c.inhibitorio(), c.remisionCompetencia())
                .filter(java.util.Objects::nonNull).count();
        if (bloques > 1) {
            e.put("tipo", "Registre una sola decisión por vez.");
            return e;
        }
        switch (c.tipo()) {
            case CONTINUAR_TRAMITE -> continuar(c.continuarTramite(), ctx, e);
            case SOLICITAR_INFORMACION -> solicitar(c.solicitudInformacion(), ctx, e);
            case INHIBITORIO -> inhibitorio(c.inhibitorio(), ctx, e);
            case REMISION_COMPETENCIA -> remision(c.remisionCompetencia(), ctx, e);
        }
        return e;
    }

    private static void continuar(final DecisionEvaluacionRequest.ContinuarTramite t, final Contexto ctx, final Map<String, String> e) {
        if (t == null) {
            e.put("continuarTramite", "Complete los datos para continuar el trámite.");
            return;
        }
        if (!ctx.vinculacionDeterminada()) {
            e.put("continuarTramite.vinculacion",
                    "Clasifique primero la vinculación de la persona presuntamente responsable: de ella depende el equipo que instruye.");
        }
        if (ctx.casoIdActual() != null) {
            // UAD-ID-06 / UAD-NOT-03: el hecho ya tiene ID de caso desde otro actor; se conserva, sin duplicar.
            if (t.modoCaso() != ModoCaso.CONSERVAR_ASIGNADO) {
                e.put("continuarTramite.modoCaso", "Esta noticia ya tiene un ID de caso Casilda: se conserva el mismo.");
            }
        } else if (t.modoCaso() == ModoCaso.CONSERVAR_ASIGNADO) {
            e.put("continuarTramite.modoCaso", "La noticia no tiene un ID de caso para conservar.");
        } else if (t.modoCaso() == ModoCaso.VINCULAR_EXISTENTE) {
            if (vacio(t.casoIdVincular())) {
                e.put("continuarTramite.casoIdVincular", "Seleccione el caso existente de los mismos hechos.");
            } else if (!ctx.casosAsociados().contains(t.casoIdVincular())) {
                e.put("continuarTramite.casoIdVincular", "El caso debe salir de la consulta de casos asociados.");
            }
            if (!Boolean.TRUE.equals(t.confirmaMismosHechos())) {
                e.put("continuarTramite.confirmaMismosHechos",
                        "Confirme que se trata de los mismos hechos. Una coincidencia de documento no basta para vincular.");
            }
        }
        if (t.expedienteUad() != null && t.expedienteUad().trim().length() > LIMITE_EXPEDIENTE) {
            e.put("continuarTramite.expedienteUad", "Máximo " + LIMITE_EXPEDIENTE + " caracteres.");
        }
    }

    private static void solicitar(final DecisionEvaluacionRequest.SolicitudInformacion s, final Contexto ctx, final Map<String, String> e) {
        if (ctx.tipoNoticia() == TipoNoticia.NDA || !ctx.tieneReportanteIdentificado()) {
            e.put("tipo", "Solo se puede pedir información a un reportante identificado (UAD-EVA-04).");
            return;
        }
        if (s == null || vacio(s.tipoSolicitudCodigo()) || !ctx.tipoSolicitudValido().test(s.tipoSolicitudCodigo())) {
            e.put("solicitudInformacion.tipoSolicitudCodigo", "Seleccione el tipo de solicitud.");
        }
        if (s == null || s.informacionRequerida() == null || s.informacionRequerida().trim().length() < MINIMO_INFORMACION) {
            e.put("solicitudInformacion.informacionRequerida", "Describa con claridad qué información necesita.");
        }
        if (s == null || s.fechaLimite() == null) {
            e.put("solicitudInformacion.fechaLimite", "Indique la fecha límite para responder.");
        } else if (!s.fechaLimite().isAfter(ctx.hoy())) {
            e.put("solicitudInformacion.fechaLimite", "La fecha límite debe ser posterior a hoy.");
        }
    }

    private static void inhibitorio(final DecisionEvaluacionRequest.Inhibitorio i, final Contexto ctx, final Map<String, String> e) {
        if (i == null || vacio(i.causalCodigo()) || !ctx.causalValida().test(i.causalCodigo())) {
            e.put("inhibitorio.causalCodigo", "Seleccione la causal.");
        }
        if (JUSTIFICACION_INHIBITORIO_OBLIGATORIA && (i == null || vacio(i.justificacion()))) {
            e.put("inhibitorio.justificacion", "Escriba la justificación de la decisión.");
        }
    }

    private static void remision(final DecisionEvaluacionRequest.RemisionCompetencia r, final Contexto ctx, final Map<String, String> e) {
        if (r == null || vacio(r.faseProceso())) {
            e.put("remisionCompetencia.faseProceso", "Seleccione la fase del proceso.");
        } else if (!"EVALUACION_PRELIMINAR".equals(r.faseProceso())) {
            // En la evaluación preliminar la fase siempre es esa (D-UAD-19).
            e.put("remisionCompetencia.faseProceso", "En la evaluación preliminar la fase es «Evaluación preliminar».");
        }
        if (r == null || vacio(r.entidadDestinatariaCodigo()) || !ctx.entidadValida().test(r.entidadDestinatariaCodigo())) {
            e.put("remisionCompetencia.entidadDestinatariaCodigo", "Seleccione la entidad destinataria.");
        }
        if (r == null || r.fechaRemision() == null) {
            e.put("remisionCompetencia.fechaRemision", "Indique la fecha de remisión.");
        } else if (r.fechaRemision().isAfter(ctx.hoy())) {
            e.put("remisionCompetencia.fechaRemision", "La fecha de remisión no puede ser futura.");
        }
    }

    private static boolean vacio(final String valor) {
        return valor == null || valor.isBlank();
    }
}
