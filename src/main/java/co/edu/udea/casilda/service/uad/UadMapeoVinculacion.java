package co.edu.udea.casilda.service.uad;

import co.edu.udea.casilda.model.enums.VinculoUdeAEnum;
import co.edu.udea.casilda.model.enums.uad.VinculacionInvestigado;

/**
 * Homologación <strong>PROVISIONAL</strong> (D-UAD-05) del catálogo {@code vinculoudea} (14 valores) a
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

    public static VinculacionInvestigado desdeVinculoUdeA(final Integer idVinculoUdeA) {
        if (idVinculoUdeA == null) return VinculacionInvestigado.NO_SE;
        final VinculoUdeAEnum vinculo;
        try {
            vinculo = VinculoUdeAEnum.fromId(idVinculoUdeA);
        } catch (IllegalArgumentException ex) {
            return VinculacionInvestigado.NO_SE;
        }
        if (vinculo == null) return VinculacionInvestigado.NO_SE;
        return switch (vinculo) {
            case ESTUDIANTE_PREGRADO -> VinculacionInvestigado.ESTUDIANTE_PREGRADO;
            case ESTUDIANTE_DE_POSGRADO -> VinculacionInvestigado.ESTUDIANTE_POSGRADO;
            case DOCENTE_VINCULADO -> VinculacionInvestigado.DOCENTE_VINCULADO;
            case DOCENTE_OCASIONAL -> VinculacionInvestigado.DOCENTE_OCASIONAL;
            case DOCENTE_DE_CATEDRA, DOCENTE_CATEDRA_50 -> VinculacionInvestigado.DOCENTE_CATEDRA;
            case PERSONAL_ADMINISTRATIVO -> VinculacionInvestigado.ADMINISTRATIVO;
            case EGRESADO_PREGRADO, EGRESADO_POSGRADO, JUBILADO_PENSIONADO, CONTRATISTA, PRESTADOR_DE_SERVICIOS, EXTERNO ->
                    VinculacionInvestigado.NO_TIENE_VINCULO;
            case OTRO_TIPO_DE_VINCULO -> VinculacionInvestigado.NO_SE;
        };
    }
}
