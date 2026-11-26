-- Configuración inicial de roles y endpoints, para ejecutar después de que Hibernate cree las tablas.
-- Este archivo no crea tablas ni carga datos de prueba de la aplicación.

INSERT INTO rol (id, codigo, nombre, activo) VALUES
    (1, 'ADMIN', 'Admin', true),
    (2, 'COORDINADOR', 'Coordinador', true),
    (3, 'PROFESIONAL', 'Profesional', true),
    (4, 'REVISOR', 'Revisor', true),
    (5, 'USUARIO', 'Usuario', true),
    (6, 'GESTOR_CONTENIDO', 'Gestor de contenidos', true)
ON CONFLICT (id) DO UPDATE
SET codigo = EXCLUDED.codigo,
    nombre = EXCLUDED.nombre,
    activo = EXCLUDED.activo;

INSERT INTO rol (id, codigo, nombre, activo)
SELECT COALESCE(MAX(id), 0) + 1, 'TECNICO_APH', 'Técnico APH', true
FROM rol
HAVING NOT EXISTS (
    SELECT 1
    FROM rol
    WHERE codigo = 'TECNICO_APH'
);

INSERT INTO endpoint (path, http_method, activo, publico) VALUES
    ('/auth/**', 'ANY', true, true),
    ('/api-docs/**', 'ANY', true, true),
    ('/swagger-ui/**', 'ANY', true, true),
    ('/swagger-ui.html', 'ANY', true, true),
    ('/usuarios', 'GET', true, false),
    ('/usuarios/paginado', 'GET', true, false),
    ('/usuarios/{id}', 'GET', true, false),
    ('/usuarios', 'POST', true, false),
    ('/usuarios/{id}', 'PUT', true, false),
    ('/usuarios/{id}', 'DELETE', true, false),
    ('/usuarios/{id}/estado', 'PATCH', true, false),
    ('/casos/**', 'GET', true, false),
    ('/casos/**', 'POST', true, false),
    ('/solicitudes/**', 'GET', true, false),
    ('/solicitudes/**', 'POST', true, false),
    ('/solicitudes/acompanamiento', 'POST', true, false),
    ('/solicitudes/acompanamiento/{id}/contacto', 'POST', true, false),
    ('/solicitudes/**', 'PUT', true, false),
    ('/solicitudes/**', 'DELETE', true, false),
    ('/atenciones/**', 'GET', true, false),
    ('/atenciones/**', 'POST', true, false),
    ('/citas/**', 'GET', true, false),
    ('/citas/**', 'PUT', true, false),
    ('/citas/caso/{casoId}/seguimientos', 'POST', true, false),
    ('/compromisos/**', 'GET', true, false),
    ('/compromisos/**', 'POST', true, false),
    ('/compromisos/**', 'DELETE', true, false),
    ('/linea-alma/**', 'GET', true, false),
    ('/linea-alma/**', 'POST', true, false),
    ('/alma/atenciones-aph', 'POST', true, false),
    ('/alma/atenciones-aph/{idAtencion}', 'GET', true, false),
    ('/alma/atenciones-aph/{idAtencion}', 'PATCH', true, false),
    ('/alma/atenciones-aph/{idAtencion}/triaje', 'PUT', true, false),
    ('/alma/atenciones-aph/{idAtencion}/derivaciones/psicologia', 'POST', true, false),
    ('/alma/bandeja/psicologia', 'GET', true, false),
    ('/alma/bandeja/psicologia/{idAtencion}/tomar', 'POST', true, false),
    ('/alma/bandeja/psicologia/{idAtencion}/liberar', 'POST', true, false),
    ('/alma/psicologia/novedades/{idAtencion}', 'GET', true, false),
    ('/alma/psicologia/novedades/{idAtencion}/intentos', 'GET', true, false),
    ('/alma/psicologia/novedades/{idAtencion}/intentos', 'POST', true, false),
    ('/alma/psicologia/notificaciones', 'GET', true, false),
    ('/alma/contenciones-post-cierre', 'POST', true, false),
    ('/alma/remisiones-aph', 'POST', true, false),
    ('/alma/bandeja/aph', 'GET', true, false),
    ('/alma/bandeja/aph/{idAtencion}/tomar', 'POST', true, false),
    ('/alma/rutas-externas', 'POST', true, false),
    ('/alma/rutas-externas', 'GET', true, false),
    ('/alma/rutas-externas/{idRuta}', 'PATCH', true, false),
    ('/alma/derivaciones/internas', 'POST', true, false),
    ('/alma/derivaciones/internas', 'GET', true, false),
    ('/alma/atenciones-aph/{idAtencion}/cierre', 'POST', true, false),
    ('/personas/**', 'GET', true, false),
    ('/parametros/**', 'GET', true, false),
    ('/parametros/**', 'PUT', true, false),
    ('/maestros/**', 'GET', true, false),
    ('/maestros/roles', 'GET', true, false),
    ('/maestros/**', 'POST', true, false),
    ('/maestros/**', 'PUT', true, false),
    ('/maestros/**', 'DELETE', true, false),
    ('/maestros/tipos-identificacion', 'GET', true, true),
    ('/maestros/campus', 'GET', true, true),
    ('/maestros/rutas-activacion', 'GET', true, true),
    ('/maestros/tipos-solicitud', 'GET', true, true),
    ('/maestros/unidades-administrativas', 'GET', true, true),
    ('/maestros/unidades-academicas', 'GET', true, true),
    ('/maestros/identidades-genero', 'GET', true, true),
    ('/maestros/cargos', 'GET', true, true),
    ('/maestros/medio-solicitud', 'GET', true, true),
    ('/maestros/vinculos-udea', 'GET', true, true),
    ('/maestros/vinculos-agresor-victima', 'GET', true, true),
    ('/contenidos/home', 'GET', true, true),
    ('/contenidos', 'GET', true, false),
    ('/contenidos/{id}', 'GET', true, false),
    ('/contenidos', 'POST', true, false),
    ('/contenidos/{id}', 'PUT', true, false),
    ('/contenidos/{id}', 'DELETE', true, false),
    ('/contenidos/imagenes/**', 'GET', true, true),
    ('/reportes', 'POST', true, true)
ON CONFLICT (path, http_method) DO UPDATE
SET activo = EXCLUDED.activo,
    publico = EXCLUDED.publico;

INSERT INTO catalogoalmaitem (catalogo, codigo, etiqueta, activo) VALUES
    ('resultados-intento', 'NO_CONTESTA', 'No contesta', true),
    ('resultados-intento', 'BUZON_DE_VOZ', 'Buzón de voz', true),
    ('resultados-intento', 'OCUPADO', 'Línea ocupada', true),
    ('resultados-intento', 'APAGADO', 'Teléfono apagado o fuera de servicio', true),
    ('resultados-intento', 'NUMERO_ERRADO', 'Número equivocado', true),
    ('resultados-intento', 'CONTESTA_OTRA_PERSONA', 'Contesta otra persona', true),
    ('resultados-intento', 'PIDE_OTRO_HORARIO', 'Contesta y pide que se le llame en otro momento', true)
ON CONFLICT (catalogo, codigo) DO UPDATE
SET etiqueta = EXCLUDED.etiqueta,
    activo = true;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo IN ('ADMIN', 'COORDINADOR', 'PROFESIONAL', 'REVISOR', 'USUARIO')
WHERE e.http_method = 'GET'
  AND e.path IN ('/maestros/**', '/personas/**')
ON CONFLICT (idendpoint, idrol) DO NOTHING;

-- Lectura de maestros para el equipo APH y los roles de coordinación y remisión existentes.
INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo IN (
    'TECNICO_APH', 'EA_COORDINACION', 'ALMA_COORDINACION', 'UAD_COORDINACION',
    'SPB_COORDINACION', 'SPB_ANALISTA', 'UAD_EQUIPO_3', 'UAD_EQUIPO_4'
)
WHERE e.path = '/maestros/**'
  AND e.http_method = 'GET'
ON CONFLICT (idendpoint, idrol) DO NOTHING;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo = 'TECNICO_APH'
WHERE e.path = '/maestros/roles'
  AND e.http_method = 'GET'
ON CONFLICT (idendpoint, idrol) DO NOTHING;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON (e.http_method = 'GET'
    AND r.codigo IN ('ADMIN', 'COORDINADOR', 'PROFESIONAL', 'REVISOR', 'TECNICO_APH'))
    OR (e.http_method IN ('POST', 'PATCH')
    AND r.codigo IN ('ADMIN', 'COORDINADOR', 'TECNICO_APH'))
WHERE (e.path = '/alma/atenciones-aph' AND e.http_method = 'POST')
   OR (e.path = '/alma/atenciones-aph/{idAtencion}' AND e.http_method IN ('GET', 'PATCH'))
ON CONFLICT (idendpoint, idrol) DO NOTHING;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo = 'TECNICO_APH'
WHERE e.path = '/alma/atenciones-aph/{idAtencion}/triaje'
  AND e.http_method = 'PUT'
ON CONFLICT (idendpoint, idrol) DO NOTHING;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id FROM endpoint e
JOIN rol r ON (e.path = '/alma/atenciones-aph/{idAtencion}/derivaciones/psicologia'
                   AND e.http_method = 'POST' AND r.codigo = 'TECNICO_APH')
    OR (e.path = '/alma/bandeja/psicologia' AND e.http_method = 'GET'
                   AND r.codigo IN ('ADMIN', 'COORDINADOR', 'PROFESIONAL', 'REVISOR'))
ON CONFLICT (idendpoint, idrol) DO NOTHING;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo IN ('ADMIN', 'COORDINADOR', 'PROFESIONAL')
WHERE e.path IN ('/alma/bandeja/psicologia/{idAtencion}/tomar',
                 '/alma/bandeja/psicologia/{idAtencion}/liberar')
  AND e.http_method = 'POST'
ON CONFLICT (idendpoint, idrol) DO NOTHING;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON (
    e.path IN ('/alma/psicologia/novedades/{idAtencion}',
               '/alma/psicologia/novedades/{idAtencion}/intentos',
               '/alma/psicologia/notificaciones')
    AND e.http_method = 'GET'
    AND r.codigo IN ('ADMIN', 'COORDINADOR', 'PROFESIONAL', 'REVISOR')
) OR (
    e.path = '/alma/psicologia/novedades/{idAtencion}/intentos'
    AND e.http_method = 'POST'
    AND r.codigo IN ('ADMIN', 'COORDINADOR', 'PROFESIONAL')
)
ON CONFLICT (idendpoint, idrol) DO NOTHING;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id FROM endpoint e
JOIN rol r ON r.codigo IN ('TECNICO_APH', 'PROFESIONAL')
WHERE e.path = '/alma/contenciones-post-cierre' AND e.http_method = 'POST'
ON CONFLICT (idendpoint, idrol) DO NOTHING;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo IN ('ADMIN', 'COORDINADOR', 'PROFESIONAL', 'REVISOR')
WHERE e.path IN ('/casos/**', '/atenciones/**', '/citas/**', '/compromisos/**', '/linea-alma/**')
  AND e.http_method IN ('GET', 'POST', 'PUT', 'DELETE')
ON CONFLICT (idendpoint, idrol) DO NOTHING;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo IN ('ADMIN', 'COORDINADOR')
WHERE e.path = '/solicitudes/**'
  AND e.http_method IN ('GET', 'POST', 'PUT', 'DELETE')
ON CONFLICT (idendpoint, idrol) DO NOTHING;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id FROM endpoint e
JOIN rol r ON (e.path = '/alma/remisiones-aph' AND e.http_method = 'POST'
    AND r.codigo IN ('ADMIN', 'EA_COORDINACION', 'PROFESIONAL', 'COORDINADOR',
                     'SPB_ANALISTA', 'UAD_COORDINACION', 'UAD_EQUIPO_3', 'UAD_EQUIPO_4'))
    OR (e.path = '/alma/bandeja/aph' AND e.http_method = 'GET'
    AND r.codigo IN ('ADMIN', 'COORDINADOR', 'REVISOR', 'TECNICO_APH'))
    OR (e.path = '/alma/bandeja/aph/{idAtencion}/tomar' AND e.http_method = 'POST'
    AND r.codigo = 'TECNICO_APH')
ON CONFLICT (idendpoint, idrol) DO NOTHING;

-- Consulta de solicitudes y reparto para todo el equipo de atención.
INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id FROM endpoint e
JOIN rol r ON r.codigo IN ('ADMIN', 'TECNICO_APH')
WHERE (e.path = '/alma/rutas-externas' AND e.http_method IN ('GET', 'POST'))
   OR (e.path = '/alma/rutas-externas/{idRuta}' AND e.http_method = 'PATCH')
   OR (e.path = '/alma/derivaciones/internas' AND e.http_method IN ('GET', 'POST'))
   OR (e.path = '/alma/atenciones-aph/{idAtencion}/cierre' AND e.http_method = 'POST')
ON CONFLICT (idendpoint, idrol) DO NOTHING;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo IN ('PROFESIONAL', 'REVISOR')
WHERE e.path = '/solicitudes/**'
  AND e.http_method = 'GET'
ON CONFLICT (idendpoint, idrol) DO NOTHING;

-- La radicación está disponible para todos los roles del equipo.
INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo IN ('ADMIN', 'COORDINADOR', 'PROFESIONAL', 'REVISOR')
WHERE e.path = '/solicitudes/acompanamiento' AND e.http_method = 'POST'
ON CONFLICT (idendpoint, idrol) DO NOTHING;

-- El profesional puede registrar contactos, sin obtener permisos de reparto.
INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo IN ('ADMIN', 'COORDINADOR', 'PROFESIONAL')
WHERE e.path = '/solicitudes/acompanamiento/{id}/contacto' AND e.http_method = 'POST'
ON CONFLICT (idendpoint, idrol) DO NOTHING;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo IN ('ADMIN', 'COORDINADOR', 'PROFESIONAL')
WHERE e.path = '/citas/caso/{casoId}/seguimientos' AND e.http_method = 'POST'
ON CONFLICT (idendpoint, idrol) DO NOTHING;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo = 'ADMIN'
WHERE e.path IN ('/usuarios', '/usuarios/paginado', '/usuarios/{id}', '/usuarios/{id}/estado')
   OR e.path = '/parametros/**'
   OR (e.path = '/maestros/**' AND e.http_method IN ('POST', 'PUT', 'DELETE'))
ON CONFLICT (idendpoint, idrol) DO NOTHING;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo = 'USUARIO'
WHERE (e.path = '/solicitudes/**' AND e.http_method = 'GET')
   OR (e.path = '/solicitudes/acompanamiento' AND e.http_method = 'POST')
ON CONFLICT (idendpoint, idrol) DO NOTHING;

-- USUARIO solo puede crear solicitudes; no debe alcanzar /asignar ni otros POST.
DELETE FROM endpointrole er
USING endpoint e, rol r
WHERE er.idendpoint = e.id AND er.idrol = r.id
  AND r.codigo = 'USUARIO'
  AND e.path = '/solicitudes/**' AND e.http_method = 'POST';

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo IN ('ADMIN', 'GESTOR_CONTENIDO')
WHERE e.path IN ('/contenidos', '/contenidos/{id}')
  AND e.http_method IN ('GET', 'POST', 'PUT', 'DELETE')
ON CONFLICT (idendpoint, idrol) DO NOTHING;
