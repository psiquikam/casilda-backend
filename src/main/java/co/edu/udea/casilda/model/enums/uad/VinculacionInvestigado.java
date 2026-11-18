package co.edu.udea.casilda.model.enums.uad;

/**
 * Vinculación de la persona presuntamente responsable (UAD-FOR-10). No coincide con
 * {@code VinculoUdeAEnum}: la homologación es provisional (D-UAD-05, {@code UadMapeoVinculacion}).
 *
 * <p>Contrato: {@code docs/contratos/uad/openapi-uad.yaml} del frontend.</p>
 */
public enum VinculacionInvestigado {
    ESTUDIANTE_PREGRADO,
    ESTUDIANTE_POSGRADO,
    DOCENTE_VINCULADO,
    DOCENTE_OCASIONAL,
    DOCENTE_CATEDRA,
    ADMINISTRATIVO,
    TRABAJADOR_OFICIAL,
    DIRECTIVO,
    NO_TIENE_VINCULO,
    NO_SE
}
