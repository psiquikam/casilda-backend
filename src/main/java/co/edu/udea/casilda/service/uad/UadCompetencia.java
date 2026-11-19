package co.edu.udea.casilda.service.uad;

import co.edu.udea.casilda.model.enums.uad.EquipoUad;
import co.edu.udea.casilda.model.enums.uad.TipoNoticia;
import co.edu.udea.casilda.model.enums.uad.VinculacionInvestigado;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static co.edu.udea.casilda.model.enums.uad.VinculacionInvestigado.*;

/**
 * Reglas de competencia UAD (UAD-NOT-05 a 09, UAD-CAS-04 a 07; catálogo de roles §3.c).
 *
 * <pre>
 * Persona presuntamente responsable                        Instrucción  Decisión
 * Docentes, administrativos, directivos, trab. oficiales   Equipo 3     Equipo 4
 * Estudiantes de pregrado y posgrado                       Equipo 4     Equipo 3
 * </pre>
 *
 * Misma regla que {@code dominio/competencia.ts} del frontend; aquí es la que <strong>autoriza</strong>.
 */
public final class UadCompetencia {

    public static final Set<VinculacionInvestigado> ESTUDIANTES = EnumSet.of(ESTUDIANTE_PREGRADO, ESTUDIANTE_POSGRADO);
    public static final Set<VinculacionInvestigado> PERSONAL =
            EnumSet.of(DOCENTE_VINCULADO, DOCENTE_OCASIONAL, DOCENTE_CATEDRA, ADMINISTRATIVO, TRABAJADOR_OFICIAL, DIRECTIVO);
    /** «No sé» y «No tiene vínculo»: visibles a ambos equipos (UAD-NOT-07; «No tiene vínculo» provisional, D-UAD-12). */
    public static final Set<VinculacionInvestigado> INDETERMINADAS = EnumSet.of(NO_SE, NO_TIENE_VINCULO);

    private UadCompetencia() {
    }

    public static boolean esDeterminada(final VinculacionInvestigado v) {
        return v != null && (ESTUDIANTES.contains(v) || PERSONAL.contains(v));
    }

    public static EquipoUad equipoInstructor(final VinculacionInvestigado v) {
        if (v == null) return null;
        if (ESTUDIANTES.contains(v)) return EquipoUad.EQUIPO_4;
        if (PERSONAL.contains(v)) return EquipoUad.EQUIPO_3;
        return null;
    }

    public static EquipoUad equipoDecisor(final VinculacionInvestigado v) {
        if (v == null) return null;
        if (ESTUDIANTES.contains(v)) return EquipoUad.EQUIPO_3;
        if (PERSONAL.contains(v)) return EquipoUad.EQUIPO_4;
        return null;
    }

    /** NDA: ambos siempre (UAD-NOT-09). NDI sin estamento: ambos hasta clasificar. NDI con estamento: el instructor. */
    public static List<EquipoUad> equiposConVisibilidad(final TipoNoticia tipo, final VinculacionInvestigado v) {
        if (tipo == TipoNoticia.NDA) return List.of(EquipoUad.EQUIPO_3, EquipoUad.EQUIPO_4);
        final EquipoUad instructor = equipoInstructor(v);
        return instructor == null ? List.of(EquipoUad.EQUIPO_3, EquipoUad.EQUIPO_4) : List.of(instructor);
    }

    public static boolean esVisiblePara(final Collection<EquipoUad> equipos, final TipoNoticia tipo, final VinculacionInvestigado v) {
        final List<EquipoUad> visibles = equiposConVisibilidad(tipo, v);
        return equipos.stream().anyMatch(visibles::contains);
    }

    /** Vinculaciones cuyas NDI ve el equipo en su bandeja: las que instruye más las indeterminadas. */
    public static Set<VinculacionInvestigado> vinculacionesVisibles(final Collection<EquipoUad> equipos) {
        final Set<VinculacionInvestigado> visibles = EnumSet.copyOf(INDETERMINADAS);
        if (equipos.contains(EquipoUad.EQUIPO_3)) visibles.addAll(PERSONAL);
        if (equipos.contains(EquipoUad.EQUIPO_4)) visibles.addAll(ESTUDIANTES);
        return visibles;
    }
}
