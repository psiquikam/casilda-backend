package co.edu.udea.casilda.service.uad;

import co.edu.udea.casilda.dto.response.uad.UadResponses;
import co.edu.udea.casilda.model.enums.uad.CodigoActuacion;
import co.edu.udea.casilda.model.enums.uad.EquipoUad;
import co.edu.udea.casilda.model.enums.uad.EstadoGestionUad;
import co.edu.udea.casilda.model.enums.uad.FaseUad;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static co.edu.udea.casilda.model.enums.uad.CodigoActuacion.*;

/**
 * Catálogo de «Gestionar caso» (bitácora §7 y §8). Misma regla que {@code dominio/catalogo-actuaciones.ts} del
 * frontend; aquí es la que <strong>autoriza</strong>.
 *
 * <pre>
 * Actuación ordinaria            Fase y equipo
 * Indagación previa              Equipo 3, instrucción (personal)
 * Investigación disciplinaria    Equipo 3, instrucción (personal)
 * Averiguaciones preliminares    Equipo 4, instrucción (estudiantes)
 * Práctica de pruebas            Instrucción y decisión, ambos equipos
 * Pliego de cargos               Instrucción, ambos equipos
 * Audiencias                     Decisión, ambos equipos
 * Mediación                      Decisión, ambos equipos (pendiente de validación, §16)
 * Fallo                          Decisión, ambos equipos
 * Reposición y/o apelación       Decisión, ambos equipos
 * </pre>
 *
 * Las acciones permanentes (§8) se muestran siempre. Cada una se ejecuta por su propia operación, nunca junto con
 * ordinarias (D-UAD-20). Inhibitorio, terminación y remisión a la Ruta quedan deshabilitadas hasta su validación.
 */
public final class UadCatalogoActuaciones {

    static final String GESTION_FINALIZADA = "La gestión de la UAD sobre este caso ya terminó.";

    private record Ordinaria(CodigoActuacion codigo, Set<EquipoUad> instruccion, Set<EquipoUad> decision, String pendiente) {
    }

    private static final Set<EquipoUad> AMBOS = EnumSet.allOf(EquipoUad.class);
    private static final Set<EquipoUad> NINGUNO = EnumSet.noneOf(EquipoUad.class);

    private static final List<Ordinaria> ORDINARIAS = List.of(
            new Ordinaria(INDAGACION_PREVIA, EnumSet.of(EquipoUad.EQUIPO_3), NINGUNO, null),
            new Ordinaria(INVESTIGACION_DISCIPLINARIA, EnumSet.of(EquipoUad.EQUIPO_3), NINGUNO, null),
            new Ordinaria(AVERIGUACIONES_PRELIMINARES, EnumSet.of(EquipoUad.EQUIPO_4), NINGUNO, null),
            new Ordinaria(PRACTICA_PRUEBAS, AMBOS, AMBOS, null),
            new Ordinaria(PLIEGO_CARGOS, AMBOS, NINGUNO, null),
            new Ordinaria(AUDIENCIAS, NINGUNO, AMBOS, null),
            new Ordinaria(MEDIACION, NINGUNO, AMBOS,
                    "Sujeta a procedencia: falta validar su viabilidad en cada régimen y en asuntos de VBG/VS."),
            new Ordinaria(FALLO, NINGUNO, AMBOS, null),
            new Ordinaria(REPOSICION_APELACION, NINGUNO, AMBOS, null));

    /** Acciones permanentes en orden de presentación, con su motivo de bloqueo si está pendiente de validación. */
    private static final Map<CodigoActuacion, String> PERMANENTES = new LinkedHashMap<>();

    static {
        PERMANENTES.put(REMISION_COMPETENCIA, null);
        PERMANENTES.put(INHIBITORIO, "Pendiente de validación jurídica: procedencia después del inicio formal de la etapa.");
        PERMANENTES.put(TERMINACION, "Pendiente de validación jurídica: alcance y diferencia con suspensión y archivo.");
        PERMANENTES.put(FINALIZAR_INSTRUCCION, null);
        PERMANENTES.put(REMISION_RUTA, "Los campos de la remisión interna están pendientes de aprobación. No cierra el caso institucional.");
    }

    private UadCatalogoActuaciones() {
    }

    public static boolean esPermanente(final CodigoActuacion codigo) {
        return PERMANENTES.containsKey(codigo);
    }

    /** Ordinarias de la fase y el equipo del caso, más todas las permanentes. */
    public static List<UadResponses.ActuacionDisponible> paraCaso(final FaseUad fase, final EquipoUad equipo, final EstadoGestionUad estado) {
        final boolean activa = estado == EstadoGestionUad.ACTIVA;
        final List<UadResponses.ActuacionDisponible> lista = new ArrayList<>();
        for (Ordinaria o : ORDINARIAS) {
            final Set<EquipoUad> aplica = fase == FaseUad.INSTRUCCION ? o.instruccion() : o.decision();
            if (!aplica.contains(equipo)) continue;
            final String motivo = !activa ? GESTION_FINALIZADA : o.pendiente();
            lista.add(new UadResponses.ActuacionDisponible(o.codigo(), o.codigo().nombre(), false, motivo == null, motivo, o.pendiente() != null));
        }
        PERMANENTES.forEach((codigo, pendiente) -> {
            String motivo = pendiente;
            if (!activa) {
                motivo = GESTION_FINALIZADA;
            } else if (codigo == FINALIZAR_INSTRUCCION && fase != FaseUad.INSTRUCCION) {
                motivo = "Solo se ejecuta durante la instrucción. El caso ya está en decisión.";
            }
            lista.add(new UadResponses.ActuacionDisponible(codigo, codigo.nombre(), true, motivo == null, motivo, pendiente != null));
        });
        return lista;
    }

    /** Valida un registro de ordinarias contra el catálogo vigente. Devuelve errores por campo; vacío si es válido. */
    public static Map<String, String> validar(final List<CodigoActuacion> codigos, final List<UadResponses.ActuacionDisponible> catalogo) {
        if (codigos == null || codigos.isEmpty()) return Map.of("actuaciones", "Marque al menos una actuación.");
        if (codigos.stream().anyMatch(Objects::isNull)) return Map.of("actuaciones", "Hay una actuación no reconocida.");
        if (Set.copyOf(codigos).size() != codigos.size()) return Map.of("actuaciones", "Hay actuaciones repetidas.");
        if (codigos.stream().anyMatch(UadCatalogoActuaciones::esPermanente)) {
            return Map.of("actuaciones", "Las acciones permanentes se registran por separado, no junto con actuaciones ordinarias.");
        }
        final List<String> noDisponibles = codigos.stream()
                .filter(c -> catalogo.stream().noneMatch(a -> a.codigo() == c && !a.permanente() && a.ejecutable()))
                .map(CodigoActuacion::nombre)
                .toList();
        if (!noDisponibles.isEmpty()) {
            return Map.of("actuaciones", "No disponible en la fase y el equipo actuales: " + String.join(", ", noDisponibles) + ".");
        }
        return Map.of();
    }
}
