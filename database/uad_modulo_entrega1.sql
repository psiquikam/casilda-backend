-- =====================================================================================================
-- Módulo UAD Equipos 3 y 4 — Entrega 1 (bandejas NDI/NDA, evaluación preliminar, queja anónima)
-- Contrato: docs/contratos/uad/openapi-uad.yaml (frontend). Decisiones: DECISIONES_PENDIENTES_UAD.md.
--
-- CUÁNDO EJECUTAR: después de desplegar el backend con estas entidades, porque las tablas uad_* las crea
-- Hibernate (ddl-auto: update). El script es idempotente: se puede ejecutar más de una vez.
--
-- QUÉ HACE
--   1. Crea los roles UAD_COORDINACION, UAD_EQUIPO_3 y UAD_EQUIPO_4 (catálogo de roles, propuesta del
--      11/11/2026). Los ids se asignan como MAX(id)+1 bajo el mismo advisory lock que usa
--      docs/GUIA_CREACION_ROLES.md. Si el código ya existe, no se duplica.
--   2. Registra las rutas de la Entrega 1 en `endpoint` y sus permisos en `endpointrole`.
--      Sin esto, DynamicEndpointAuthorizationManager responde 403 a todo /uad, incluso a ADMIN.
--   3. Siembra los catálogos UAD. Valores PENDIENTES DE VALIDACIÓN jurídica (D-UAD-13).
--
-- QUÉ NO HACE: no asigna los roles a ningún usuario. Eso se hace con usuariorol, según la guía.
--
-- MATRIZ DE PERMISOS (docs/ContratoCreacionRoles.md):
--   UAD_EQUIPO_3 / UAD_EQUIPO_4 : LECTURA y CREACIÓN en las rutas de abajo. Equipo, fase y visibilidad de
--                                 cada noticia los valida UadNoticiaService (la ruta sola no basta).
--   UAD_COORDINACION            : solo GET /uad/contexto. Sin lectura de bandejas hasta aprobar su matriz (D-UAD-03).
--   ADMIN                       : solo GET /uad/contexto. Sin acceso a expedientes reservados (catálogo de roles §1).
--   PÚBLICO                     : POST /uad/noticias-anonimas (la queja anónima no exige ni asocia sesión).
-- =====================================================================================================

BEGIN;

SELECT pg_advisory_xact_lock(74839201);

-- 1. Roles ------------------------------------------------------------------------------------------
INSERT INTO rol (id, codigo, nombre, activo)
SELECT (SELECT COALESCE(MAX(id), 0) FROM rol) + v.orden, v.codigo, v.nombre, true
FROM (VALUES
          (1, 'UAD_COORDINACION', 'Coordinación UAD'),
          (2, 'UAD_EQUIPO_3', 'Profesional UAD Equipo 3'),
          (3, 'UAD_EQUIPO_4', 'Profesional UAD Equipo 4')
     ) AS v(orden, codigo, nombre)
WHERE NOT EXISTS (SELECT 1 FROM rol r WHERE r.codigo = v.codigo)
ORDER BY v.orden;

-- 2. Rutas ------------------------------------------------------------------------------------------
INSERT INTO endpoint (path, http_method, activo, publico) VALUES
    ('/uad/contexto', 'GET', true, false),
    ('/uad/noticias', 'GET', true, false),
    ('/uad/noticias/*', 'GET', true, false),
    ('/uad/noticias/*/clasificacion', 'POST', true, false),
    ('/uad/noticias/*/casos-asociados', 'GET', true, false),
    ('/uad/noticias/*/decisiones', 'POST', true, false),
    ('/uad/noticias/*/historial', 'GET', true, false),
    ('/uad/evidencias/*/descarga', 'GET', true, false),
    ('/maestros/uad-causales-inhibitorio', 'GET', true, false),
    ('/maestros/uad-entidades-remision', 'GET', true, false),
    ('/maestros/uad-tipos-solicitud-informacion', 'GET', true, false),
    ('/uad/noticias-anonimas', 'POST', true, true)
ON CONFLICT (path, http_method) DO UPDATE SET activo = EXCLUDED.activo, publico = EXCLUDED.publico;

-- Equipos 3 y 4: todas las rutas privadas de la Entrega 1.
INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo IN ('UAD_EQUIPO_3', 'UAD_EQUIPO_4')
WHERE (e.path, e.http_method) IN (
          ('/uad/contexto', 'GET'),
          ('/uad/noticias', 'GET'),
          ('/uad/noticias/*', 'GET'),
          ('/uad/noticias/*/clasificacion', 'POST'),
          ('/uad/noticias/*/casos-asociados', 'GET'),
          ('/uad/noticias/*/decisiones', 'POST'),
          ('/uad/noticias/*/historial', 'GET'),
          ('/uad/evidencias/*/descarga', 'GET'),
          ('/maestros/uad-causales-inhibitorio', 'GET'),
          ('/maestros/uad-entidades-remision', 'GET'),
          ('/maestros/uad-tipos-solicitud-informacion', 'GET'))
ON CONFLICT (idendpoint, idrol) DO NOTHING;

-- Coordinación UAD y ADMIN: solo el contexto (la UI muestra «Acceso restringido»).
INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo IN ('UAD_COORDINACION', 'ADMIN')
WHERE e.path = '/uad/contexto' AND e.http_method = 'GET'
ON CONFLICT (idendpoint, idrol) DO NOTHING;

-- 3. Catálogos (PENDIENTES DE VALIDACIÓN) -----------------------------------------------------------
INSERT INTO uad_catalogo (catalogo, codigo, nombre, orden, activo) VALUES
    ('CAUSAL_INHIBITORIO', 'NO_FALTA_DISCIPLINARIA', 'Los hechos no constituyen falta disciplinaria', 1, true),
    ('CAUSAL_INHIBITORIO', 'NO_COMPETENCIA_UAD', 'La situación no es de competencia de la UAD', 2, true),
    ('CAUSAL_INHIBITORIO', 'FALTA_ELEMENTOS_MINIMOS', 'Faltan elementos mínimos de configuración', 3, true),
    ('CAUSAL_INHIBITORIO', 'PRESCRIPCION', 'Prescripción de la acción disciplinaria', 4, true),
    ('CAUSAL_INHIBITORIO', 'DUPLICIDAD', 'Duplicidad de actuación en curso', 5, true),
    ('ENTIDAD_REMISION', 'PROCURADURIA_REGIONAL', 'Procuraduría Regional de Instrucción', 1, true),
    ('ENTIDAD_REMISION', 'FISCALIA', 'Fiscalía General de la Nación', 2, true),
    ('ENTIDAD_REMISION', 'OTRA_AUTORIDAD', 'Otra autoridad competente', 3, true),
    ('TIPO_SOLICITUD_INFORMACION', 'ACLARACION_HECHOS', 'Aclaración o ampliación de información', 1, true),
    ('TIPO_SOLICITUD_INFORMACION', 'APORTE_EVIDENCIAS', 'Aporte de evidencias', 2, true),
    ('TIPO_SOLICITUD_INFORMACION', 'VINCULO_PRESUNTO_RESPONSABLE', 'Precisar vínculo del presunto responsable', 3, true)
ON CONFLICT (catalogo, codigo) DO UPDATE SET nombre = EXCLUDED.nombre, orden = EXCLUDED.orden, activo = EXCLUDED.activo;

COMMIT;
