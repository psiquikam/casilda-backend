-- Aplicar a una base existente después de revisar los roles y permisos vigentes.
-- No concede permisos de reparto a profesionales.
BEGIN;

INSERT INTO endpoint (path, http_method, activo, publico) VALUES
    ('/solicitudes/acompanamiento', 'POST', true, false),
    ('/solicitudes/acompanamiento/{id}/contacto', 'POST', true, false),
    ('/citas/caso/{casoId}/seguimientos', 'POST', true, false)
ON CONFLICT (path, http_method) DO NOTHING;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo IN ('ADMIN', 'COORDINADOR', 'PROFESIONAL', 'REVISOR')
WHERE e.path = '/solicitudes/acompanamiento' AND e.http_method = 'POST'
ON CONFLICT (idendpoint, idrol) DO NOTHING;

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

COMMIT;
