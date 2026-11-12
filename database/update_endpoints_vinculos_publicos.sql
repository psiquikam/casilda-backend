-- Habilita únicamente la lectura pública de los catálogos del formulario anónimo.
INSERT INTO endpoint (path, http_method, activo, publico) VALUES
    ('/maestros/vinculos-udea', 'GET', true, true),
    ('/maestros/vinculos-agresor-victima', 'GET', true, true)
ON CONFLICT (path, http_method) DO UPDATE
SET activo = EXCLUDED.activo,
    publico = EXCLUDED.publico;
