-- =====================================================================================================
-- Módulo UAD Equipos 3 y 4 — Entrega 2 (bandeja de casos y «Gestionar caso»)
-- Contrato: docs/contratos/uad/openapi-uad.yaml (frontend), operaciones con x-entrega: 2.
--
-- REQUISITO: haber ejecutado database/uad_modulo_entrega1.sql (crea los roles UAD_EQUIPO_3 y UAD_EQUIPO_4).
-- CUÁNDO EJECUTAR: después de desplegar el backend de la Entrega 2. Las columnas nuevas las crea Hibernate.
-- Idempotente: se puede ejecutar más de una vez.
--
-- QUÉ HACE: registra las rutas de casos en `endpoint` y las autoriza a UAD_EQUIPO_3 y UAD_EQUIPO_4.
-- Equipo, fase y estado de cada caso los valida UadCasoService (la ruta sola no basta).
-- Coordinación UAD y ADMIN no reciben estas rutas (D-UAD-03, catálogo de roles §1).
-- =====================================================================================================

BEGIN;

INSERT INTO endpoint (path, http_method, activo, publico) VALUES
    ('/uad/casos', 'GET', true, false),
    ('/uad/casos/*', 'GET', true, false),
    ('/uad/casos/*/expediente', 'PATCH', true, false),
    ('/uad/casos/*/actuaciones/catalogo', 'GET', true, false),
    ('/uad/casos/*/actuaciones', 'POST', true, false),
    ('/uad/casos/*/historial', 'GET', true, false),
    ('/uad/casos/*/traslado-decision', 'POST', true, false),
    ('/uad/casos/*/remisiones-competencia', 'POST', true, false),
    ('/uad/casos/*/evidencias', 'GET', true, false)
ON CONFLICT (path, http_method) DO UPDATE SET activo = EXCLUDED.activo, publico = EXCLUDED.publico;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo IN ('UAD_EQUIPO_3', 'UAD_EQUIPO_4')
WHERE (e.path, e.http_method) IN (
          ('/uad/casos', 'GET'),
          ('/uad/casos/*', 'GET'),
          ('/uad/casos/*/expediente', 'PATCH'),
          ('/uad/casos/*/actuaciones/catalogo', 'GET'),
          ('/uad/casos/*/actuaciones', 'POST'),
          ('/uad/casos/*/historial', 'GET'),
          ('/uad/casos/*/traslado-decision', 'POST'),
          ('/uad/casos/*/remisiones-competencia', 'POST'),
          ('/uad/casos/*/evidencias', 'GET'))
ON CONFLICT (idendpoint, idrol) DO NOTHING;

COMMIT;
