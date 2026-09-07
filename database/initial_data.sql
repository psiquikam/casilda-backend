-- Datos iniciales para ejecutar después de que Hibernate cree las tablas.
-- Este archivo no crea tablas.

INSERT INTO rol (id, codigo, nombre, activo) VALUES
    (1, 'ADMIN', 'Admin', true),
    (2, 'COORDINADOR', 'Coordinador', true),
    (3, 'PROFESIONAL', 'Profesional', true),
    (4, 'REVISOR', 'Revisor', true),
    (5, 'USUARIO', 'Usuario', true)
ON CONFLICT (id) DO UPDATE
SET codigo = EXCLUDED.codigo,
    nombre = EXCLUDED.nombre,
    activo = EXCLUDED.activo;

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
    ('/solicitudes/**', 'PUT', true, false),
    ('/solicitudes/**', 'DELETE', true, false),
    ('/atenciones/**', 'GET', true, false),
    ('/atenciones/**', 'POST', true, false),
    ('/citas/**', 'GET', true, false),
    ('/citas/**', 'PUT', true, false),
    ('/compromisos/**', 'GET', true, false),
    ('/compromisos/**', 'POST', true, false),
    ('/compromisos/**', 'DELETE', true, false),
    ('/linea-alma/**', 'GET', true, false),
    ('/linea-alma/**', 'POST', true, false),
    ('/personas/**', 'GET', true, false),
    ('/parametros/**', 'GET', true, false),
    ('/parametros/**', 'PUT', true, false),
    ('/maestros/**', 'GET', true, false),
    ('/maestros/**', 'POST', true, false),
    ('/maestros/**', 'PUT', true, false),
    ('/maestros/**', 'DELETE', true, false)
ON CONFLICT (path, http_method) DO UPDATE
SET activo = EXCLUDED.activo,
    publico = EXCLUDED.publico;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo IN ('ADMIN', 'COORDINADOR', 'PROFESIONAL', 'REVISOR', 'USUARIO')
WHERE e.http_method = 'GET'
  AND e.path IN ('/maestros/**', '/personas/**')
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
WHERE e.path LIKE '/solicitudes/%'
  AND e.http_method IN ('GET', 'POST', 'PUT', 'DELETE')
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
WHERE e.path = '/solicitudes/**'
  AND e.http_method IN ('GET', 'POST')
ON CONFLICT (idendpoint, idrol) DO NOTHING;
