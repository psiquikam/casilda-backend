-- SOLO PARA ENTORNOS LOCALES DE PRUEBA; no ejecutar en QA ni producción.
-- Crea una cuenta sintética con la misma contraseña que el usuario de prueba indicado.
-- Antes de ejecutar, confirmar que la cuenta fuente sea realmente de prueba.
-- Requiere haber ejecutado update_add_rol_profesional_aph.sql.
DO $$
DECLARE
    correo_nuevo constant text := 'profesional.aph@casilda.test';
    nombre_nuevo constant text := 'Profesional APH de prueba';
    correo_fuente constant text := 'revisor1@udea.edu.co';
    id_rol integer;
    clave_fuente text;
    id_usuario bigint;
BEGIN
    SELECT id INTO id_rol
    FROM rol
    WHERE codigo = 'PROFESIONAL_APH' AND activo = true;

    IF id_rol IS NULL THEN
        RAISE EXCEPTION 'El rol PROFESIONAL_APH no existe o está inactivo';
    END IF;

    SELECT password INTO clave_fuente
    FROM usuario
    WHERE email = correo_fuente AND activo = true;

    IF clave_fuente IS NULL OR clave_fuente NOT LIKE '$2%' THEN
        RAISE EXCEPTION 'La cuenta de prueba fuente no existe, está inactiva o no tiene contraseña BCrypt';
    END IF;

    IF EXISTS (SELECT 1 FROM usuario WHERE email = correo_nuevo) THEN
        RAISE EXCEPTION 'Ya existe la cuenta %. No se modificará', correo_nuevo;
    END IF;

    INSERT INTO usuario (email, password, nombre, idrol, activo, fechacreacion, fechaactualizacion, habeasdata)
    VALUES (correo_nuevo, clave_fuente, nombre_nuevo, id_rol, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
    RETURNING id INTO id_usuario;

    INSERT INTO usuariorol (idusuario, idrol) VALUES (id_usuario, id_rol);
END
$$;
