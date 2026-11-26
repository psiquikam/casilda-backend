-- Ejecutar después de que Hibernate cree las tablas de Equipo de Atención.
-- No modifica ni elimina rutas compartidas existentes.

BEGIN;
LOCK TABLE rutaactivacion IN SHARE ROW EXCLUSIVE MODE;
ALTER TABLE rutaatencion ADD COLUMN IF NOT EXISTS cual VARCHAR(255);

WITH opciones(orden, codigo, nombre) AS (
    VALUES
        (1, 'EA_SALUD', 'Ruta de salud'),
        (2, 'EA_FISCALIA', 'Fiscalía General de la Nación - FGN'),
        (3, 'EA_COMISARIA', 'Comisaría de Familia - CDF'),
        (4, 'EA_INSPECCION', 'Inspección de Policía'),
        (5, 'EA_PROCURADURIA', 'Procuraduría General de la Nación'),
        (6, 'otras-rutas-internas', 'Otra'),
        (7, 'otras-rutas-externas', 'Otra')
), pendientes AS (
    SELECT orden, codigo, nombre FROM opciones
    WHERE NOT EXISTS (SELECT 1 FROM rutaactivacion existente WHERE existente.codigo = opciones.codigo)
)
INSERT INTO rutaactivacion (id, codigo, nombre, activo)
SELECT (SELECT COALESCE(MAX(id), 0) FROM rutaactivacion) + ROW_NUMBER() OVER (ORDER BY orden),
       codigo, nombre, true
FROM pendientes;

INSERT INTO rutaequipoatencion (idrutaactivacion, tipo)
SELECT id, 'INTERNA' FROM rutaactivacion
WHERE codigo IN ('UAD_3_4', 'URC', 'SEGURIDAD_PERSONAS_BIENES',
                 'AMENAZA', 'PROTECCION_ACADEMICA', 'PROTECCION_LABORAL',
                 'otras-rutas-internas', 'NO_APLICA')
ON CONFLICT (idrutaactivacion, tipo) DO NOTHING;

INSERT INTO rutaequipoatencion (idrutaactivacion, tipo)
SELECT id, 'EXTERNA' FROM rutaactivacion
WHERE codigo IN ('EA_SALUD', 'EA_FISCALIA', 'EA_COMISARIA', 'EA_INSPECCION',
                 'EA_PROCURADURIA', 'otras-rutas-externas', 'NO_APLICA')
ON CONFLICT (idrutaactivacion, tipo) DO NOTHING;
COMMIT;
