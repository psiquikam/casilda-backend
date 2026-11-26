-- Update vinculoudea catalog entries
INSERT INTO vinculoudea (id, nombre) VALUES
(1, 'Estudiante de Pregrado'),
(2, 'Estudiante de Posgrado'),
(3, 'Egresado Pregrado'),
(4, 'Egresado Posgrado'),
(5, 'Personal Administrativo'),
(6, 'Docente Vinculado'),
(7, 'Docente Ocasional'),
(8, 'Docente de Cátedra'),
(9, 'Contratista'),
(10, 'Otro tipo de vínculo'),
(11, 'Docente Cátedra 50'),
(12, 'Jubilado / Pensionado'),
(13, 'Prestador de Servicios'),
(14, 'Externo')
ON CONFLICT (id) DO UPDATE SET nombre = EXCLUDED.nombre;

-- Los nuevos vínculos conservan los IDs ya existentes en cada base.
DO $$
DECLARE
    nombre_nuevo TEXT;
    coincidencias INTEGER;
    nuevo_id INTEGER;
BEGIN
    LOCK TABLE vinculoudea IN SHARE ROW EXCLUSIVE MODE;
    FOR nombre_nuevo IN SELECT * FROM (VALUES
        ('Contratista CIS'),
        ('Estudiante de tecnología'),
        ('Jubilado'),
        ('Pensionado'),
        ('Personal no docente'),
        ('No tiene vínculo con la universidad'),
        ('Egresado')
    ) AS opciones(nombre)
    LOOP
        SELECT count(*) INTO coincidencias FROM vinculoudea
        WHERE lower(trim(nombre)) = lower(trim(nombre_nuevo));
        IF coincidencias > 1 THEN
            RAISE EXCEPTION 'Hay más de un vínculo UdeA llamado %', nombre_nuevo;
        END IF;
        IF coincidencias = 0 THEN
            SELECT COALESCE(max(id), 0) + 1 INTO nuevo_id FROM vinculoudea;
            INSERT INTO vinculoudea (id, nombre) VALUES (nuevo_id, nombre_nuevo);
        END IF;
    END LOOP;
END $$;
