-- SOLO PARA ENTORNOS LOCALES O QA AUTORIZADOS; nunca ejecutar en producción.
-- Crea identidades sintéticas para probar PROFESIONAL, cargo individual,
-- tipo de servicio y membresías de grupos especializados y una dupla.
--
-- Prerrequisitos:
--   1. Haber aplicado database/update_schema_registro_usuario.sql.
--   2. Tener cargados los catálogos de cargo, tipo de identificación,
--      tipo de servicio y grupos profesionales de insert_datos_maestros.sql.
--   3. Existir el rol PROFESIONAL y la cuenta sintética fuente
--      revisor1@udea.edu.co con contraseña BCrypt.
--
-- No crea ni reasigna casos/asignaciones de atención. No modifica usuarios,
-- personas, profesionales ni grupos existentes con datos incompatibles.
BEGIN;

DO $$
DECLARE
    rol_profesional integer;
    tipo_cc integer;
    cargo_psicologa integer;
    cargo_abogada integer;
    servicio_dupla integer;
    servicio_juridico integer;
    servicio_psicologia integer;
    grupo_dupla integer;
    grupo_abogadas integer;
    grupo_psicologas integer;
    ciudad_nacimiento integer;
    hash_prueba text;
    id_persona_psicologa bigint;
    id_persona_abogada bigint;
    id_usuario_psicologa bigint;
    id_usuario_abogada bigint;
    id_persona_vinculada bigint;
    id_rol_usuario integer;
BEGIN
    SELECT id INTO rol_profesional FROM rol WHERE codigo = 'PROFESIONAL' AND activo = true;
    SELECT id INTO tipo_cc FROM tipoidentificacion WHERE codigo = 'CC' AND activo = true;
    SELECT id INTO cargo_psicologa FROM cargo WHERE nombre = 'Psicóloga';
    SELECT id INTO cargo_abogada FROM cargo WHERE nombre = 'Abogada';
    SELECT id INTO servicio_dupla FROM tiposervicio WHERE nombre = 'Dupla Psicojurídica';
    SELECT id INTO servicio_juridico FROM tiposervicio WHERE nombre = 'Jurídico';
    SELECT id INTO servicio_psicologia FROM tiposervicio WHERE nombre = 'Psicología';
    SELECT id INTO grupo_dupla FROM grupoprofesional
        WHERE nombre = 'Dupla N° 1' AND idtiposervicio = servicio_dupla;
    SELECT id INTO grupo_abogadas FROM grupoprofesional
        WHERE nombre = 'Abogada N° 1' AND idtiposervicio = servicio_juridico;
    SELECT id INTO grupo_psicologas FROM grupoprofesional
        WHERE nombre = 'Psicóloga N° 1' AND idtiposervicio = servicio_psicologia;
    SELECT MIN(id) INTO ciudad_nacimiento FROM municipio;

    IF rol_profesional IS NULL OR tipo_cc IS NULL
            OR cargo_psicologa IS NULL OR cargo_abogada IS NULL
            OR servicio_dupla IS NULL OR servicio_juridico IS NULL OR servicio_psicologia IS NULL
            OR grupo_dupla IS NULL OR grupo_abogadas IS NULL OR grupo_psicologas IS NULL
            OR ciudad_nacimiento IS NULL THEN
        RAISE EXCEPTION 'Faltan roles o catálogos requeridos; no se crearán usuarios de prueba';
    END IF;

    SELECT password INTO hash_prueba
    FROM usuario
    WHERE email = 'revisor1@udea.edu.co' AND activo = true;

    IF hash_prueba IS NULL OR hash_prueba NOT LIKE '$2%' THEN
        RAISE EXCEPTION 'La cuenta sintética fuente no existe, está inactiva o no tiene BCrypt';
    END IF;

    -- Persona sintética con cargo Psicóloga.
    SELECT id INTO id_persona_psicologa FROM persona
    WHERE numerodocumento = '9999999901' AND idtipoidentificacion = tipo_cc;
    IF id_persona_psicologa IS NULL THEN
        INSERT INTO persona (primernombre, primerapellido, idtipoidentificacion,
                             numerodocumento, idciudadnacimiento)
        VALUES ('Prueba', 'Psicología QA', tipo_cc, '9999999901', ciudad_nacimiento)
        RETURNING id INTO id_persona_psicologa;
    ELSIF NOT EXISTS (
        SELECT 1 FROM persona
        WHERE id = id_persona_psicologa
          AND primernombre = 'Prueba' AND primerapellido = 'Psicología QA'
    ) THEN
        RAISE EXCEPTION 'El documento sintético de Psicología ya pertenece a otra persona';
    END IF;

    -- Persona sintética con cargo Abogada.
    SELECT id INTO id_persona_abogada FROM persona
    WHERE numerodocumento = '9999999902' AND idtipoidentificacion = tipo_cc;
    IF id_persona_abogada IS NULL THEN
        INSERT INTO persona (primernombre, primerapellido, idtipoidentificacion,
                             numerodocumento, idciudadnacimiento)
        VALUES ('Prueba', 'Jurídico QA', tipo_cc, '9999999902', ciudad_nacimiento)
        RETURNING id INTO id_persona_abogada;
    ELSIF NOT EXISTS (
        SELECT 1 FROM persona
        WHERE id = id_persona_abogada
          AND primernombre = 'Prueba' AND primerapellido = 'Jurídico QA'
    ) THEN
        RAISE EXCEPTION 'El documento sintético de Jurídico ya pertenece a otra persona';
    END IF;

    -- Usuarios sintéticos. ON CONFLICT no modifica cuentas preexistentes.
    INSERT INTO usuario (email, password, nombre, idrol, idpersona, activo,
                         fechacreacion, fechaactualizacion, habeasdata)
    VALUES ('psicologa.aph.qa@casilda.test', hash_prueba, 'Psicóloga de prueba APH',
            rol_profesional, id_persona_psicologa, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
    ON CONFLICT (email) DO NOTHING;

    INSERT INTO usuario (email, password, nombre, idrol, idpersona, activo,
                         fechacreacion, fechaactualizacion, habeasdata)
    VALUES ('abogada.aph.qa@casilda.test', hash_prueba, 'Abogada de prueba APH',
            rol_profesional, id_persona_abogada, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
    ON CONFLICT (email) DO NOTHING;

    SELECT id, idpersona, idrol INTO id_usuario_psicologa, id_persona_vinculada, id_rol_usuario
    FROM usuario WHERE email = 'psicologa.aph.qa@casilda.test' AND activo = true;
    IF id_usuario_psicologa IS NULL OR id_persona_vinculada <> id_persona_psicologa
            OR id_rol_usuario <> rol_profesional THEN
        RAISE EXCEPTION 'La cuenta sintética de Psicología ya existe con datos incompatibles';
    END IF;

    SELECT id, idpersona, idrol INTO id_usuario_abogada, id_persona_vinculada, id_rol_usuario
    FROM usuario WHERE email = 'abogada.aph.qa@casilda.test' AND activo = true;
    IF id_usuario_abogada IS NULL OR id_persona_vinculada <> id_persona_abogada
            OR id_rol_usuario <> rol_profesional THEN
        RAISE EXCEPTION 'La cuenta sintética de Jurídico ya existe con datos incompatibles';
    END IF;

    INSERT INTO usuariorol (idusuario, idrol)
    VALUES (id_usuario_psicologa, rol_profesional), (id_usuario_abogada, rol_profesional)
    ON CONFLICT (idusuario, idrol) DO NOTHING;

    -- Si ya existe un registro profesional, debe coincidir con el cargo de prueba.
    IF EXISTS (SELECT 1 FROM profesional WHERE idpersona = id_persona_psicologa
               AND idcargo <> cargo_psicologa)
       OR EXISTS (SELECT 1 FROM profesional WHERE idpersona = id_persona_abogada
                  AND idcargo <> cargo_abogada) THEN
        RAISE EXCEPTION 'Una persona sintética ya tiene un cargo profesional incompatible';
    END IF;

    INSERT INTO profesional (idpersona, idcargo)
    VALUES (id_persona_psicologa, cargo_psicologa), (id_persona_abogada, cargo_abogada)
    ON CONFLICT (idpersona) DO NOTHING;

    -- Cada profesional pertenece a su grupo de especialidad y ambos a una dupla,
    -- para ensayar que el tipo de servicio del grupo no sustituye al cargo individual.
    INSERT INTO profesionalgrupoprofesional (idgrupoprofesional, idprofesional)
    VALUES (grupo_psicologas, id_persona_psicologa),
           (grupo_abogadas, id_persona_abogada),
           (grupo_dupla, id_persona_psicologa),
           (grupo_dupla, id_persona_abogada)
    ON CONFLICT (idgrupoprofesional, idprofesional) DO NOTHING;
END
$$;

COMMIT;

-- Verificación resumida: no muestra documentos ni hashes.
SELECT u.email, r.codigo AS rol, c.nombre AS cargo,
       string_agg(DISTINCT ts.nombre, ', ' ORDER BY ts.nombre) AS tipos_servicio
FROM usuario u
JOIN rol r ON r.id = u.idrol
JOIN profesional p ON p.idpersona = u.idpersona
JOIN cargo c ON c.id = p.idcargo
JOIN profesionalgrupoprofesional pgp ON pgp.idprofesional = p.idpersona
JOIN grupoprofesional gp ON gp.id = pgp.idgrupoprofesional
JOIN tiposervicio ts ON ts.id = gp.idtiposervicio
WHERE u.email IN ('psicologa.aph.qa@casilda.test', 'abogada.aph.qa@casilda.test')
GROUP BY u.email, r.codigo, c.nombre
ORDER BY u.email;
