-- Ejecutar después de que Hibernate actualice el esquema, durante una ventana de mantenimiento.
-- Los IDs existentes cambian de significado, también en los registros históricos.
-- Si un ID adicional está referenciado, la transacción falla sin borrar datos históricos.
BEGIN;

INSERT INTO tiposervicio (id, nombre, activo) VALUES
    (1, 'Jurídico', true),
    (2, 'Dupla Psicojurídica', true),
    (3, 'Psicología', true),
    (4, 'Psicoorientación', true),
    (5, 'Trabajo Social', true)
ON CONFLICT (id) DO UPDATE SET nombre = EXCLUDED.nombre, activo = true;

INSERT INTO grupoprofesional (id, nombre, idtiposervicio, activo) VALUES
    (1, 'Dupla N° 1', 2, true),
    (2, 'Dupla N° 2', 2, true),
    (3, 'Abogada N° 1', 1, true),
    (4, 'Abogada N° 2', 1, true),
    (5, 'Psicóloga N° 1', 3, true),
    (6, 'Psicóloga N° 2', 3, true),
    (7, 'Psicoorientadora', 4, true),
    (8, 'Trabajadora social', 5, true)
ON CONFLICT (id) DO UPDATE SET nombre = EXCLUDED.nombre,
    idtiposervicio = EXCLUDED.idtiposervicio, activo = true;

DELETE FROM grupoprofesional WHERE id NOT BETWEEN 1 AND 8;
DELETE FROM tiposervicio WHERE id NOT BETWEEN 1 AND 5;

ALTER TABLE grupoprofesional ALTER COLUMN idtiposervicio SET NOT NULL;

DO $$
DECLARE
    seq text;
    ultimo bigint;
    tabla text;
BEGIN
    FOREACH tabla IN ARRAY ARRAY['tiposervicio', 'grupoprofesional'] LOOP
        seq := pg_get_serial_sequence(tabla, 'id');
        IF seq IS NOT NULL THEN
            EXECUTE format('SELECT last_value FROM %s', seq) INTO ultimo;
            EXECUTE format('SELECT setval(%L, GREATEST($1, max(id)), true) FROM %I', seq, tabla)
                USING ultimo;
        END IF;
    END LOOP;
END $$;

COMMIT;
