package co.edu.udea.casilda.model.enums.uad;

/**
 * Acciones que el servidor habilita sobre una noticia según estado, equipo y vinculación.
 *
 * <p>Contrato: {@code docs/contratos/uad/openapi-uad.yaml} del frontend.</p>
 */
public enum AccionNoticia {
    CLASIFICAR_VINCULACION,
    CONSULTAR_CASOS_ASOCIADOS,
    CONTINUAR_TRAMITE,
    SOLICITAR_INFORMACION,
    INHIBITORIO,
    REMISION_COMPETENCIA,
    DESCARGAR_EVIDENCIAS
}
