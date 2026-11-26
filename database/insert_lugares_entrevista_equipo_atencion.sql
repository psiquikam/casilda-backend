BEGIN;

LOCK TABLE lugarentrevista IN SHARE ROW EXCLUSIVE MODE;

WITH opciones(orden, nombre) AS (
    VALUES (1, 'Otro'), (2, 'Oficina de Bienestar Universitario')
), pendientes AS (
    SELECT orden, nombre FROM opciones
    WHERE NOT EXISTS (SELECT 1 FROM lugarentrevista existente WHERE existente.nombre = opciones.nombre)
)
INSERT INTO lugarentrevista (id, nombre)
SELECT (SELECT COALESCE(MAX(id), 0) FROM lugarentrevista)
       + ROW_NUMBER() OVER (ORDER BY orden), nombre
FROM pendientes;

COMMIT;
