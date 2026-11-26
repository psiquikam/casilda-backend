package co.edu.udea.casilda.service.uad;

import co.edu.udea.casilda.model.entity.VinculoUdeA;
import co.edu.udea.casilda.model.enums.uad.VinculacionInvestigado;

import java.util.Locale;

/**
 * Homologación <strong>PROVISIONAL</strong> (D-UAD-05) del catálogo {@code vinculoudea} a
 * la lista de la bitácora UAD-FOR-10 (10 valores), para las NDI que nacen de {@code POST /reportes}.
 *
 * <ul>
 *   <li>Docente de cátedra y «Cátedra 50» → {@code DOCENTE_CATEDRA}.</li>
 *   <li>Egresados, jubilados, contratistas, prestadores y externos → {@code NO_TIENE_VINCULO}: no son
 *       estamentos sujetos a la competencia de la UAD según la tabla de la bitácora. La UAD los evalúa
 *       igual y puede remitir (UAD-FOR-11).</li>
 *   <li>«Otro tipo de vínculo» o sin dato → {@code NO_SE}: visible a ambos equipos hasta clasificar.</li>
 *   <li>{@code TRABAJADOR_OFICIAL} y {@code DIRECTIVO} no existen en {@code vinculoudea}: solo se obtienen
 *       al clasificar en la UAD.</li>
 * </ul>
 *
 * Al aprobarse D-UAD-05 se reemplaza esta clase; la noticia guarda la vinculación ya traducida.
 */
public final class UadMapeoVinculacion {

    private UadMapeoVinculacion() {
    }

    public static VinculacionInvestigado desdeVinculoUdeA(final VinculoUdeA vinculo) {
        if (vinculo == null || vinculo.getNombre() == null) return VinculacionInvestigado.NO_SE;
        return switch (vinculo.getNombre().trim().toLowerCase(Locale.ROOT)) {
            case "estudiante de pregrado" -> VinculacionInvestigado.ESTUDIANTE_PREGRADO;
            case "estudiante de posgrado" -> VinculacionInvestigado.ESTUDIANTE_POSGRADO;
            case "docente vinculado" -> VinculacionInvestigado.DOCENTE_VINCULADO;
            case "docente ocasional" -> VinculacionInvestigado.DOCENTE_OCASIONAL;
            case "docente de cátedra", "docente cátedra", "docente cátedra 50" ->
                    VinculacionInvestigado.DOCENTE_CATEDRA;
            case "personal administrativo", "empleado administrativo" -> VinculacionInvestigado.ADMINISTRATIVO;
            case "egresado pregrado", "egresado posgrado", "egresado", "jubilado / pensionado",
                    "jubilado", "pensionado", "contratista", "contratista cis", "prestador de servicios",
                    "externo", "no tiene vínculo con la universidad" ->
                    VinculacionInvestigado.NO_TIENE_VINCULO;
            default -> VinculacionInvestigado.NO_SE;
        };
    }
}
