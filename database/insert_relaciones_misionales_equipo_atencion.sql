-- No se modifican los IDs ni las relaciones históricas.
BEGIN;

LOCK TABLE actividadmisional, formaocurrencia, lugarocurrencia IN SHARE ROW EXCLUSIVE MODE;
ALTER TABLE caso ADD COLUMN IF NOT EXISTS ambitoocurrencia VARCHAR(50);
ALTER TABLE caso ADD COLUMN IF NOT EXISTS ambitoocurrenciaotro VARCHAR(255);

WITH opciones(orden, nombre) AS (
    VALUES
        (1, 'Misionales: Extensión'),
        (2, 'Misionales: Docencia'),
        (3, 'Misionales: Investigación'),
        (4, 'Actividades institucionales'),
        (5, 'Actividades en representación de la Universidad'),
        (6, 'Dentro de los bienes inmuebles de la Universidad o por fuera de estos, en razón o como consecuencia de su vínculo o del vínculo del presunto-a agresor-a con la institución educativa'),
        (7, 'No aplica')
), pendientes AS (
    SELECT orden, nombre FROM opciones
    WHERE NOT EXISTS (SELECT 1 FROM actividadmisional existente WHERE existente.nombre = opciones.nombre)
)
INSERT INTO actividadmisional (id, nombre)
SELECT (SELECT COALESCE(MAX(id), 0) FROM actividadmisional) + ROW_NUMBER() OVER (ORDER BY orden), nombre
FROM pendientes;

WITH opciones(orden, nombre) AS (
    VALUES (1, 'Presencial'), (2, 'Virtual'), (3, 'Mixta'), (4, 'N/A')
), pendientes AS (
    SELECT orden, nombre FROM opciones
    WHERE NOT EXISTS (SELECT 1 FROM formaocurrencia existente WHERE existente.nombre = opciones.nombre)
)
INSERT INTO formaocurrencia (id, nombre)
SELECT (SELECT COALESCE(MAX(id), 0) FROM formaocurrencia) + ROW_NUMBER() OVER (ORDER BY orden), nombre
FROM pendientes;

WITH opciones(orden, nombre) AS (
    VALUES (1, 'Dentro de la Universidad'), (2, 'Fuera de la Universidad'), (3, 'Mixto'), (4, 'N/A')
), pendientes AS (
    SELECT orden, nombre FROM opciones
    WHERE NOT EXISTS (SELECT 1 FROM lugarocurrencia existente WHERE existente.nombre = opciones.nombre)
)
INSERT INTO lugarocurrencia (id, nombre)
SELECT (SELECT COALESCE(MAX(id), 0) FROM lugarocurrencia) + ROW_NUMBER() OVER (ORDER BY orden), nombre
FROM pendientes;

COMMIT;
