-- Ejecutar después de update_add_rol_tecnico_aph.sql.
-- No modifica roles ni otros endpoints.
INSERT INTO endpoint (path, http_method, activo, publico) VALUES
    ('/alma/atenciones-aph', 'POST', true, false),
    ('/alma/atenciones-aph/{idAtencion}', 'GET', true, false),
    ('/alma/atenciones-aph/{idAtencion}', 'PATCH', true, false)
ON CONFLICT (path, http_method) DO UPDATE SET activo = true, publico = false;

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
