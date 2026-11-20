-- Permisos provisionales de pruebas; revisar antes de desplegar.
-- PROFESIONAL puede incluir usuarios de otras áreas; no habilitar en producción sin restringirlo.
INSERT INTO endpoint (path, http_method, activo, publico) VALUES
    ('/alma/atenciones-aph/{idAtencion}/derivaciones/psicologia', 'POST', true, false),
    ('/alma/bandeja/psicologia', 'GET', true, false)
ON CONFLICT (path, http_method) DO UPDATE SET activo = true, publico = false;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id FROM endpoint e
JOIN rol r ON (e.path = '/alma/atenciones-aph/{idAtencion}/derivaciones/psicologia'
                   AND e.http_method = 'POST' AND r.codigo = 'TECNICO_APH')
    OR (e.path = '/alma/bandeja/psicologia' AND e.http_method = 'GET'
                   AND r.codigo IN ('ADMIN', 'COORDINADOR', 'PROFESIONAL', 'REVISOR'))
ON CONFLICT (idendpoint, idrol) DO NOTHING;
