-- =====================================================================================================
-- Módulo UAD Equipos 3 y 4 — Entrega 3, parte 1 («Mis reportes» y respuesta a la solicitud de información)
-- Contrato: docs/contratos/uad/openapi-uad.yaml (frontend), operaciones listarMisReportes, obtenerMiReporte
-- y responderSolicitudInformacion.
--
-- REQUISITO: haber ejecutado database/uad_modulo_entrega1.sql.
-- CUÁNDO EJECUTAR: después de desplegar el backend con esta entrega. Las columnas nuevas de
-- uad_solicitud_informacion (respuesta, fecha_respuesta) las crea Hibernate (ddl-auto: update).
-- Idempotente: se puede ejecutar más de una vez.
--
-- QUÉ HACE: registra las tres rutas en `endpoint` y las autoriza al rol USUARIO.
-- La ruta solo autoriza el rol: UadMisReportesService filtra por dueño (ReporteCaso.usuario) y responde 404
-- ante un reporte ajeno (D-UAD-16). Las NDA y los reportes hechos sin sesión no tienen dueño y no aparecen.
--
-- QUÉ NO HACE: no da estas rutas al equipo de atención, a los equipos UAD ni a ADMIN. Si una persona de esos
-- equipos necesita ver sus propios reportes, se le asigna además el rol USUARIO.
-- =====================================================================================================

BEGIN;

INSERT INTO endpoint (path, http_method, activo, publico) VALUES
    ('/mis-reportes', 'GET', true, false),
    ('/mis-reportes/*', 'GET', true, false),
    ('/reportes/*/solicitudes-informacion/*/respuesta', 'POST', true, false)
ON CONFLICT (path, http_method) DO UPDATE SET activo = EXCLUDED.activo, publico = EXCLUDED.publico;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo = 'USUARIO'
WHERE (e.path, e.http_method) IN (
          ('/mis-reportes', 'GET'),
          ('/mis-reportes/*', 'GET'),
          ('/reportes/*/solicitudes-informacion/*/respuesta', 'POST'))
ON CONFLICT (idendpoint, idrol) DO NOTHING;

COMMIT;
