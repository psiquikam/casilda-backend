package co.edu.udea.casilda.model.enums.uad;

/**
 * Estado de la noticia (no de la gestión UAD ni del caso CASILDA). Lista no exhaustiva:
 * reactivaciones y recursos están pendientes de validación (bitácora §16, D-UAD-08).
 *
 * <p>Contrato: {@code docs/contratos/uad/openapi-uad.yaml} del frontend.</p>
 */
public enum EstadoNoticia {
    RECIBIDA,
    PENDIENTE_CLASIFICACION,
    EN_EVALUACION_PRELIMINAR,
    PENDIENTE_SUBSANACION,
    SUBSANACION_RECIBIDA,
    EN_REEVALUACION,
    INHIBITORIA,
    REMITIDA_POR_COMPETENCIA,
    PROCEDE_TRAMITE
}
