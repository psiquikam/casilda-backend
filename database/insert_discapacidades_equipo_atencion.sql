-- Conserva los subtipos existentes y sus referencias.
BEGIN;

LOCK TABLE tipodiscapacidad, subtipodiscapacidad IN SHARE ROW EXCLUSIVE MODE;

DO $$
BEGIN
    IF (SELECT count(*) FROM tipodiscapacidad WHERE nombre = 'Clasificación funcional') > 1 THEN
        RAISE EXCEPTION 'Existe más de un tipo de discapacidad con el nombre Clasificación funcional';
    END IF;
END $$;

INSERT INTO tipodiscapacidad (id, nombre)
SELECT COALESCE(MAX(id), 0) + 1, 'Clasificación funcional'
FROM tipodiscapacidad
HAVING NOT EXISTS (SELECT 1 FROM tipodiscapacidad WHERE nombre = 'Clasificación funcional');

WITH opciones(orden, nombre) AS (
    VALUES
        (1, 'Persona ciega'),
        (2, 'Persona con baja visión'),
        (3, 'Persona sordo-oralizado'),
        (4, 'Persona con hipoacusia'),
        (5, 'Persona con compromiso en miembros superiores'),
        (6, 'Persona con compromiso en miembros inferiores'),
        (7, 'Persona con compromiso en miembros superiores e inferiores'),
        (8, 'Discapacidad intelectual'),
        (9, 'Discapacidad psicosocial'),
        (10, 'Discapacidad múltiple'),
        (11, 'Talla baja'),
        (12, 'Sordoceguera'),
        (13, 'Persona sordoseñante')
), pendientes AS (
    SELECT opciones.orden, opciones.nombre, tipo.id AS idtipo
    FROM opciones
    CROSS JOIN tipodiscapacidad tipo
    WHERE tipo.nombre = 'Clasificación funcional'
      AND NOT EXISTS (
          SELECT 1 FROM subtipodiscapacidad existente
          WHERE existente.idtipo = tipo.id AND existente.nombre = opciones.nombre
      )
)
INSERT INTO subtipodiscapacidad (id, nombre, idtipo)
SELECT (SELECT COALESCE(MAX(id), 0) FROM subtipodiscapacidad)
       + ROW_NUMBER() OVER (ORDER BY orden), nombre, idtipo
FROM pendientes;

COMMIT;
