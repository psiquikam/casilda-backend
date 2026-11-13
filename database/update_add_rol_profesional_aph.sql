-- Agrega el rol requerido por el módulo APH en bases ya inicializadas.
-- Solo habilita GET /maestros/roles; no asigna usuarios ni permisos a otras rutas.
INSERT INTO rol (id, codigo, nombre, activo)
SELECT COALESCE(MAX(id), 0) + 1, 'PROFESIONAL_APH', 'Profesional APH', true
FROM rol
HAVING NOT EXISTS (
    SELECT 1
    FROM rol
    WHERE codigo = 'PROFESIONAL_APH'
);

INSERT INTO endpoint (path, http_method, activo, publico)
VALUES ('/maestros/roles', 'GET', true, false)
ON CONFLICT (path, http_method) DO UPDATE
SET activo = true,
    publico = false;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo = 'PROFESIONAL_APH'
WHERE e.path = '/maestros/roles'
  AND e.http_method = 'GET'
ON CONFLICT (idendpoint, idrol) DO NOTHING;
