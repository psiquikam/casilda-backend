-- Ejecutar después de iniciar la versión que crea rutaexternaalma.
INSERT INTO endpoint (path, http_method, activo, publico) VALUES
    ('/alma/rutas-externas', 'POST', true, false),
    ('/alma/rutas-externas', 'GET', true, false),
    ('/alma/rutas-externas/{idRuta}', 'PATCH', true, false)
ON CONFLICT (path, http_method) DO UPDATE SET activo = true, publico = false;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id FROM endpoint e
JOIN rol r ON r.codigo IN ('ADMIN', 'TECNICO_APH')
WHERE (e.path = '/alma/rutas-externas' AND e.http_method IN ('GET', 'POST'))
   OR (e.path = '/alma/rutas-externas/{idRuta}' AND e.http_method = 'PATCH')
ON CONFLICT (idendpoint, idrol) DO NOTHING;
