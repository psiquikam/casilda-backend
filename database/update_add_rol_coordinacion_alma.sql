-- Crea el rol de coordinación APH y concede únicamente acceso a GET /alma/auditoria.
-- Ejecutar solo en entornos autorizados; no asigna usuarios al rol.
INSERT INTO rol (id, codigo, nombre, activo)
SELECT COALESCE(MAX(id), 0) + 1, 'ALMA_COORDINACION', 'Coordinación Alma', true
FROM rol
HAVING NOT EXISTS (
    SELECT 1
    FROM rol
    WHERE codigo = 'ALMA_COORDINACION'
);

UPDATE rol
SET activo = true
WHERE codigo = 'ALMA_COORDINACION';

SELECT setval(
    pg_get_serial_sequence('endpoint', 'id'),
    COALESCE((SELECT MAX(id) FROM endpoint), 1),
    EXISTS (SELECT 1 FROM endpoint)
);

INSERT INTO endpoint (path, http_method, activo, publico)
VALUES ('/alma/auditoria', 'GET', true, false)
ON CONFLICT (path, http_method) DO UPDATE
SET activo = true,
    publico = false;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo IN ('ADMIN', 'ALMA_COORDINACION')
WHERE e.path = '/alma/auditoria'
  AND e.http_method = 'GET'
ON CONFLICT (idendpoint, idrol) DO NOTHING;
