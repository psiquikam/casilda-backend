package co.edu.udea.casilda.model.enums.uad;

/**
 * Tipos de evento del historial append-only (UAD-NOT-12, UAD-HIS).
 *
 * <p>Contrato: {@code docs/contratos/uad/openapi-uad.yaml} del frontend.</p>
 */
public enum TipoEventoUad {
    NOTICIA_RECIBIDA,
    ASIGNADA_EQUIPO,
    CLASIFICACION_VINCULACION,
    CASOS_ASOCIADOS_CONSULTADOS,
    SOLICITUD_INFORMACION,
    SUBSANACION_RECIBIDA,
    DECISION_REGISTRADA,
    CASO_VINCULADO,
    CASO_CREADO,
    NOTIFICACION,
    ACTUACION,
    TRASLADO_FASE,
    EXPEDIENTE_ACTUALIZADO,
    REMISION_COMPETENCIA,
    RECTIFICACION
}
