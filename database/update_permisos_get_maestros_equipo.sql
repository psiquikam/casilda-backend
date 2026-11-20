-- Lectura de maestros para roles existentes del equipo de atención y remisión.
-- No crea roles ni modifica permisos de escritura u otras rutas.
INSERT INTO endpoint (path, http_method, activo, publico)
VALUES ('/maestros/**', 'GET', true, false)
ON CONFLICT (path, http_method) DO UPDATE
SET activo = true,
    publico = false;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo IN (
    'ADMIN', 'COORDINADOR', 'PROFESIONAL', 'REVISOR', 'TECNICO_APH',
    'EA_COORDINACION', 'ALMA_COORDINACION', 'UAD_COORDINACION', 'SPB_COORDINACION',
    'SPB_ANALISTA', 'UAD_EQUIPO_3', 'UAD_EQUIPO_4'
)
WHERE e.path = '/maestros/**'
  AND e.http_method = 'GET'
ON CONFLICT (idendpoint, idrol) DO NOTHING;
