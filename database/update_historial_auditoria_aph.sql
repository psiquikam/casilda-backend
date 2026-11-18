ALTER TABLE eventoatencionalma
    ADD COLUMN IF NOT EXISTS detalle VARCHAR(500);

UPDATE eventoatencionalma
SET detalle = CASE accion
    WHEN 'CREACION' THEN 'Se creó la atención APH.'
    WHEN 'CAMBIO_ESTADO' THEN 'Se registró un cambio de estado en la atención APH.'
    WHEN 'DERIVACION' THEN 'Se registró una derivación relacionada con la atención APH.'
    WHEN 'ACTUALIZACION' THEN 'Se actualizaron datos de la atención APH.'
    ELSE 'Se registró una acción en la atención APH.'
END
WHERE detalle IS NULL;

ALTER TABLE eventoatencionalma
    ALTER COLUMN detalle SET NOT NULL;

INSERT INTO rol (id, codigo, nombre, activo)
SELECT COALESCE(MAX(id), 0) + 1, 'ALMA_COORDINACION', 'Coordinación Alma', true
FROM rol
HAVING NOT EXISTS (
    SELECT 1
    FROM rol
    WHERE codigo = 'ALMA_COORDINACION'
);

CREATE TABLE IF NOT EXISTS limiteauditoriaaph (
    idusuario BIGINT PRIMARY KEY REFERENCES usuario(id),
    inicio TIMESTAMP WITH TIME ZONE NOT NULL,
    cantidad INTEGER NOT NULL CHECK (cantidad > 0)
);

CREATE TABLE IF NOT EXISTS eventocaso (
    id BIGSERIAL PRIMARY KEY,
    idcaso BIGINT NOT NULL REFERENCES caso(id),
    idusuario BIGINT NOT NULL REFERENCES usuario(id),
    accion VARCHAR(30) NOT NULL,
    detalle VARCHAR(500) NOT NULL,
    fecha TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_eventoatencionalma_historial
    ON eventoatencionalma (idatencion, fecha, id);

CREATE INDEX IF NOT EXISTS idx_eventocaso_historial
    ON eventocaso (idcaso, fecha, id);

INSERT INTO endpoint (path, http_method, activo, publico) VALUES
    ('/alma/auditoria', 'GET', true, false)
ON CONFLICT (path, http_method) DO UPDATE SET activo = true, publico = false;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id FROM endpoint e
JOIN rol r ON r.codigo IN ('ADMIN', 'ALMA_COORDINACION')
WHERE e.path = '/alma/auditoria' AND e.http_method = 'GET'
ON CONFLICT (idendpoint, idrol) DO NOTHING;
