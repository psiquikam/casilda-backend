-- El servicio limita PROFESIONAL a usuarios con persona registrada como Psicóloga.
BEGIN;
INSERT INTO endpoint (path, http_method, activo, publico)
VALUES ('/alma/contenciones-post-cierre', 'POST', true, false)
ON CONFLICT (path, http_method) DO NOTHING;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id FROM endpoint e
JOIN rol r ON r.codigo IN ('TECNICO_APH', 'PROFESIONAL')
WHERE e.path = '/alma/contenciones-post-cierre' AND e.http_method = 'POST'
ON CONFLICT (idendpoint, idrol) DO NOTHING;
COMMIT;
