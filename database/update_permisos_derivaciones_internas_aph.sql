-- Registrar permisos después de iniciar la versión que crea derivacioninternaalma.
INSERT INTO endpoint (path, http_method, activo, publico) VALUES
    ('/alma/derivaciones/internas', 'POST', true, false),
    ('/alma/derivaciones/internas', 'GET', true, false)
ON CONFLICT (path, http_method) DO UPDATE SET activo = true, publico = false;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id FROM endpoint e
JOIN rol r ON r.codigo IN ('ADMIN', 'TECNICO_APH')
WHERE e.path = '/alma/derivaciones/internas' AND e.http_method IN ('GET', 'POST')
ON CONFLICT (idendpoint, idrol) DO NOTHING;
