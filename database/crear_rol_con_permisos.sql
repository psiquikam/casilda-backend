-- Crea un rol y registra sus permisos de endpoint de forma atómica.
--
-- El procedimiento falla completo si alguna validación no se cumple.
-- La ruta debe enviarse sin el contexto /api/v1.
--
-- Ejemplo desde psql: asigna un ID libre y serializa altas concurrentes.
-- BEGIN;
-- SELECT pg_advisory_xact_lock(74839201);
-- SELECT COALESCE(MAX(id), 0) + 1 AS nuevo_id FROM rol
-- \gset
-- CALL crear_rol_con_permisos(
--     :'nuevo_id',
--     'AUDITOR',
--     'Auditor',
--     '[
--       {"path":"/personas/**","http_method":"GET"},
--       {"path":"/casos/**","http_method":"GET"}
--     ]'::jsonb
-- );
-- COMMIT;

CREATE OR REPLACE PROCEDURE crear_rol_con_permisos(
    IN p_id integer,
    IN p_codigo varchar(50),
    IN p_nombre varchar(255),
    IN p_permisos jsonb,
    IN p_activo boolean DEFAULT true
)
LANGUAGE plpgsql
AS $$
DECLARE
    permiso record;
    id_endpoint bigint;
    endpoint_publico boolean;
    codigo_normalizado varchar(50);
    nombre_normalizado varchar(255);
BEGIN
    codigo_normalizado := upper(trim(p_codigo));
    nombre_normalizado := trim(p_nombre);

    IF p_id IS NULL OR p_id <= 0 THEN
        RAISE EXCEPTION 'El id del rol debe ser un entero positivo';
    END IF;

    IF codigo_normalizado IS NULL OR codigo_normalizado = '' THEN
        RAISE EXCEPTION 'El código del rol es obligatorio';
    END IF;

    IF codigo_normalizado !~ '^[A-Z][A-Z0-9_]*$' THEN
        RAISE EXCEPTION
            'El código del rol solo puede contener letras mayúsculas, números y guion bajo';
    END IF;

    IF nombre_normalizado IS NULL OR nombre_normalizado = '' THEN
        RAISE EXCEPTION 'El nombre del rol es obligatorio';
    END IF;

    IF p_permisos IS NULL OR jsonb_typeof(p_permisos) <> 'array' THEN
        RAISE EXCEPTION 'p_permisos debe ser un arreglo JSON';
    END IF;

    -- Evita que dos altas simultáneas calculen o validen el mismo rol.
    PERFORM pg_advisory_xact_lock(74839201);

    IF EXISTS (SELECT 1 FROM rol WHERE id = p_id) THEN
        RAISE EXCEPTION 'Ya existe un rol con id %', p_id;
    END IF;

    IF EXISTS (SELECT 1 FROM rol WHERE codigo = codigo_normalizado) THEN
        RAISE EXCEPTION 'Ya existe un rol con código %', codigo_normalizado;
    END IF;

    IF EXISTS (SELECT 1 FROM rol WHERE nombre = nombre_normalizado) THEN
        RAISE EXCEPTION 'Ya existe un rol con nombre %', nombre_normalizado;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM jsonb_to_recordset(p_permisos) AS p(path text, http_method text)
        WHERE path IS NULL
           OR btrim(path) = ''
           OR http_method IS NULL
           OR upper(btrim(http_method)) NOT IN
               ('GET', 'POST', 'PUT', 'PATCH', 'DELETE')
    ) THEN
        RAISE EXCEPTION
            'Cada permiso debe tener path y un método HTTP válido: GET, POST, PUT, PATCH o DELETE';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM jsonb_to_recordset(p_permisos) AS p(path text, http_method text)
        GROUP BY btrim(path), upper(btrim(http_method))
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'p_permisos contiene endpoints repetidos';
    END IF;

    INSERT INTO rol (id, codigo, nombre, activo)
    VALUES (p_id, codigo_normalizado, nombre_normalizado, coalesce(p_activo, true));

    FOR permiso IN
        SELECT btrim(path) AS path, upper(btrim(http_method)) AS http_method
        FROM jsonb_to_recordset(p_permisos) AS p(path text, http_method text)
    LOOP
        INSERT INTO endpoint (path, http_method, activo, publico)
        VALUES (permiso.path, permiso.http_method, true, false)
        ON CONFLICT (path, http_method) DO UPDATE
            SET activo = true
        RETURNING id INTO id_endpoint;

        SELECT publico
        INTO endpoint_publico
        FROM endpoint
        WHERE id = id_endpoint;

        IF endpoint_publico THEN
            RAISE EXCEPTION
                'El endpoint % % es público y no puede asociarse a un rol',
                permiso.http_method, permiso.path;
        END IF;

        INSERT INTO endpointrole (idendpoint, idrol)
        VALUES (id_endpoint, p_id);
    END LOOP;
END;
$$;
