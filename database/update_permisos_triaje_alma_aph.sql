-- Ejecutar después de update_add_rol_tecnico_aph.sql.
-- Solo concede el registro de triaje a TECNICO_APH.
INSERT INTO endpoint (path, http_method, activo, publico)
VALUES ('/alma/atenciones-aph/{idAtencion}/triaje', 'PUT', true, false)
ON CONFLICT (path, http_method) DO UPDATE SET activo = true, publico = false;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo = 'TECNICO_APH'
WHERE e.path = '/alma/atenciones-aph/{idAtencion}/triaje'
  AND e.http_method = 'PUT'
ON CONFLICT (idendpoint, idrol) DO NOTHING;
