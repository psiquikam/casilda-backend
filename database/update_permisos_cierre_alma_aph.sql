INSERT INTO endpoint (path, http_method, activo, publico) VALUES
    ('/alma/atenciones-aph/{idAtencion}/cierre', 'POST', true, false)
ON CONFLICT (path, http_method) DO UPDATE SET activo = true, publico = false;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id FROM endpoint e
JOIN rol r ON r.codigo IN ('ADMIN', 'TECNICO_APH')
WHERE e.path = '/alma/atenciones-aph/{idAtencion}/cierre' AND e.http_method = 'POST'
ON CONFLICT (idendpoint, idrol) DO NOTHING;
