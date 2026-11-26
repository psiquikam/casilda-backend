-- Hotfix de catálogos de Línea Alma según la revisión de flujos.
-- EN PROGRESO: no ejecutar en QA. Falta resolver las opciones históricas y validar rutas.
-- Ejecutar después de que existan las tablas del módulo y catalogoalmaitem.
-- Conserva los códigos históricos, desactiva solo opciones reemplazadas
-- y no elimina registros ni datos de atenciones existentes.
-- Requiere desplegar primero el backend que homologa vinculoudea por nombre.

BEGIN;

-- Detener la migración si un ID ya identifica un valor diferente.
-- Esta protección no sustituye la revisión de referencias ni autoriza el despliegue.
CREATE OR REPLACE FUNCTION pg_temp.proteger_catalogo_historico() RETURNS trigger AS $$
BEGIN
    IF (OLD.nombre IS DISTINCT FROM NEW.nombre
        AND NOT (TG_TABLE_NAME = 'sexo' AND (
            (lower(trim(OLD.nombre)) = 'mujer' AND NEW.nombre = 'Femenino')
            OR (lower(trim(OLD.nombre)) = 'hombre' AND NEW.nombre = 'Masculino')
        )))
       OR (TG_TABLE_NAME = 'sexo' AND (to_jsonb(OLD)->>'codigo') IS DISTINCT FROM (to_jsonb(NEW)->>'codigo')) THEN
        RAISE EXCEPTION 'No se puede reasignar el ID % de %: ya corresponde a %', OLD.id, TG_TABLE_NAME, OLD.nombre;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE
    tabla text;
BEGIN
    FOREACH tabla IN ARRAY ARRAY['sexo', 'orientacionsexual', 'identidadgenero', 'campus', 'regimen', 'eps']
    LOOP
        EXECUTE format('CREATE TRIGGER proteger_catalogo_historico BEFORE UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION pg_temp.proteger_catalogo_historico()', tabla);
    END LOOP;
END $$;

DO $$
DECLARE
    opcion record;
    coincidencias integer;
    id_alias integer;
    nuevo_id integer;
BEGIN
    LOCK TABLE sexo IN SHARE ROW EXCLUSIVE MODE;
    FOR opcion IN SELECT * FROM (VALUES
        ('Femenino', 'F', 'Mujer'),
        ('Masculino', 'M', 'Hombre'),
        ('Intersexual', 'I', NULL),
        ('Tránsgenero', 'T', NULL),
        ('Prefiere no responder', 'PR', NULL),
        ('Sin dato', 'SD', NULL)
    ) AS opciones(nombre, codigo, alias_nombre)
    LOOP
        SELECT count(*) INTO coincidencias FROM sexo
        WHERE lower(trim(nombre)) = lower(trim(opcion.nombre));
        IF coincidencias > 1 THEN
            RAISE EXCEPTION 'Hay más de un sexo llamado %', opcion.nombre;
        END IF;
        IF coincidencias = 0 AND opcion.alias_nombre IS NOT NULL THEN
            SELECT count(*), min(id) INTO coincidencias, id_alias FROM sexo
            WHERE lower(trim(nombre)) = lower(trim(opcion.alias_nombre));
            IF coincidencias > 1 THEN
                RAISE EXCEPTION 'Hay más de un sexo llamado %', opcion.alias_nombre;
            END IF;
            IF coincidencias = 1 THEN
                UPDATE sexo SET nombre = opcion.nombre WHERE id = id_alias;
            END IF;
        END IF;
        IF coincidencias = 0 THEN
            IF EXISTS (SELECT 1 FROM sexo WHERE lower(trim(codigo)) = lower(trim(opcion.codigo))) THEN
                RAISE EXCEPTION 'El código de sexo % ya pertenece a otro valor; revisar antes de continuar', opcion.codigo;
            END IF;
            SELECT COALESCE(max(id), 0) + 1 INTO nuevo_id FROM sexo;
            INSERT INTO sexo (id, codigo, nombre) VALUES (nuevo_id, opcion.codigo, opcion.nombre);
        END IF;
    END LOOP;
END $$;

DO $$
DECLARE
    tabla text;
    opcion text;
    coincidencias integer;
    nuevo_id integer;
    registro record;
BEGIN
    FOR tabla IN SELECT unnest(ARRAY['orientacionsexual', 'identidadgenero', 'campus', 'regimen'])
    LOOP
        EXECUTE format('LOCK TABLE %I IN SHARE ROW EXCLUSIVE MODE', tabla);
    END LOOP;
    FOR registro IN SELECT * FROM (VALUES
        ('orientacionsexual', 'Asexual'), ('orientacionsexual', 'Bisexual'),
        ('orientacionsexual', 'Heterosexual'), ('orientacionsexual', 'Homosexual'),
        ('orientacionsexual', 'Lesbiana'), ('orientacionsexual', 'Pansexual'),
        ('orientacionsexual', 'Otra'), ('orientacionsexual', 'Prefiere no responder'),
        ('orientacionsexual', 'Sin dato'),
        ('identidadgenero', 'Mujer cisgénero'), ('identidadgenero', 'Hombre cisgénero'),
        ('identidadgenero', 'Mujer trans'), ('identidadgenero', 'Hombre trans'),
        ('identidadgenero', 'No binaria'), ('identidadgenero', 'Prefiere no responder'),
        ('identidadgenero', 'Otra'), ('identidadgenero', 'Sin dato'),
        ('campus', 'Campus Amalfi'), ('campus', 'Campus Andes'), ('campus', 'Campus Apartadó'),
        ('campus', 'Campus Carepa'), ('campus', 'Campus Caucasia'),
        ('campus', 'Campus El Carmen de Viboral'), ('campus', 'Campus en Urabá'),
        ('campus', 'Campus Medellín - Antigua Escuela de Derecho'),
        ('campus', 'Campus Medellín - Casas Patrimoniales'),
        ('campus', 'Campus Medellín - Ciudad Universitaria'),
        ('campus', 'Campus Medellín - Ciudadela Robledo'),
        ('campus', 'Campus Medellín - Edificio Antioquia'),
        ('campus', 'Campus Medellín - Edificio de Extensión'),
        ('campus', 'Campus Medellín - Edificio San Ignacio'),
        ('campus', 'Campus Medellín - en el Área de la salud'),
        ('campus', 'Campus Medellín - Sede de Posgrado'),
        ('campus', 'Campus Puerto Berrío'), ('campus', 'Campus Santa Fe de Antioquia'),
        ('campus', 'Campus Segovia'), ('campus', 'Campus Sonsón'), ('campus', 'Campus Turbo'),
        ('campus', 'Campus Yarumal'), ('campus', 'No aplica'), ('campus', 'Desconocido'),
        ('campus', 'Otro-a'), ('regimen', 'Contributivo'), ('regimen', 'Subsidiado'),
        ('regimen', 'Especial o de Excepción')
    ) AS opciones(tabla, nombre)
    LOOP
        tabla := registro.tabla;
        opcion := registro.nombre;
        EXECUTE format('SELECT count(*) FROM %I WHERE lower(trim(nombre)) = lower(trim($1))', tabla)
            INTO coincidencias USING opcion;
        IF coincidencias > 1 THEN
            RAISE EXCEPTION 'Hay más de un registro en % llamado %', tabla, opcion;
        END IF;
        IF coincidencias = 0 THEN
            EXECUTE format('SELECT COALESCE(max(id), 0) + 1 FROM %I', tabla) INTO nuevo_id;
            EXECUTE format('INSERT INTO %I (id, nombre) VALUES ($1, $2)', tabla) USING nuevo_id, opcion;
        END IF;
    END LOOP;
END $$;

LOCK TABLE eps IN SHARE ROW EXCLUSIVE MODE;
CREATE TEMPORARY TABLE eps_linea_alma (
    nombre text PRIMARY KEY,
    especial boolean NOT NULL
) ON COMMIT DROP;
INSERT INTO eps_linea_alma (nombre, especial) VALUES
    ('Nueva EPS', false),
    ('EPS Sanitas', false),
    ('EPS Sura', false),
    ('Salud Total EPS', false),
    ('Compensar EPS', false),
    ('Famisanar', false),
    ('Coosalud EPS', false),
    ('Mutual Ser', false),
    ('Savia Salud', false),
    ('Capital Salud', false),
    ('Aliansalud', false),
    ('Comfenalco Valle', false),
    ('Servicio Occidental de Salud (SOS)', false),
    ('Emssanar', false),
    ('Asmet Salud', false),
    ('Cajacopi EPS', false),
    ('Capresoca', false),
    ('Salud Mía', false),
    ('EPS Familiar de Colombia', false),
    ('Anas Wayuu', false),
    ('Asociación Indígena del Cauca - AIC', false),
    ('Dusakawi', false),
    ('Mallamas', false),
    ('Pijaos Salud', false),
    ('Dirección General de Sanidad Militar (DGSM)', true),
    ('Dirección de Sanidad de la Policía Nacional (DISAN)', true),
    ('Fondo Nacional de Prestaciones Sociales del Magisterio (FOMAG)', true),
    ('Departamento de Salud de Ecopetrol', true),
    ('Unidades de Salud de Universidades Públicas', true);

DO $$
DECLARE
    nombre_duplicado text;
BEGIN
    SELECT objetivo.nombre INTO nombre_duplicado
    FROM eps_linea_alma objetivo
    JOIN eps existente ON lower(trim(existente.nombre)) = lower(trim(objetivo.nombre))
    GROUP BY objetivo.nombre HAVING count(*) > 1 LIMIT 1;
    IF nombre_duplicado IS NOT NULL THEN
        RAISE EXCEPTION 'Hay más de una EPS llamada %', nombre_duplicado;
    END IF;
END $$;

WITH base AS (SELECT COALESCE(max(id), 0) AS max_id FROM eps),
faltantes AS (
    SELECT objetivo.nombre FROM eps_linea_alma objetivo
    WHERE NOT EXISTS (
        SELECT 1 FROM eps existente
        WHERE lower(trim(existente.nombre)) = lower(trim(objetivo.nombre))
    )
)
INSERT INTO eps (id, nombre)
SELECT base.max_id + row_number() OVER (ORDER BY faltantes.nombre), faltantes.nombre
FROM base CROSS JOIN faltantes;

DO $$
DECLARE
    tabla text;
BEGIN
    FOREACH tabla IN ARRAY ARRAY['sexo', 'orientacionsexual', 'identidadgenero', 'campus', 'regimen', 'eps']
    LOOP
        EXECUTE format('DROP TRIGGER proteger_catalogo_historico ON %I', tabla);
    END LOOP;
END $$;

CREATE TABLE IF NOT EXISTS epsregimen (
    idregimen integer NOT NULL REFERENCES regimen(id),
    ideps integer NOT NULL REFERENCES eps(id),
    PRIMARY KEY (idregimen, ideps)
);

INSERT INTO epsregimen (idregimen, ideps)
SELECT r.id, e.id FROM eps_linea_alma objetivo
JOIN eps e ON lower(trim(e.nombre)) = lower(trim(objetivo.nombre))
JOIN regimen r ON lower(trim(r.nombre)) IN ('contributivo', 'subsidiado')
WHERE NOT objetivo.especial
UNION ALL
SELECT r.id, e.id FROM eps_linea_alma objetivo
JOIN eps e ON lower(trim(e.nombre)) = lower(trim(objetivo.nombre))
JOIN regimen r ON lower(trim(r.nombre)) = lower('Especial o de Excepción')
WHERE objetivo.especial
ON CONFLICT (idregimen, ideps) DO NOTHING;

WITH base AS (
    SELECT COALESCE(MAX(id), 0) AS max_id FROM programa
), faltantes AS (
    SELECT ua.id AS id_unidad
    FROM unidadacademica ua
    LEFT JOIN programa p
        ON p.idunidadacademica = ua.id
       AND lower(p.nombre) = 'no aplica'
    WHERE p.id IS NULL
)
INSERT INTO programa (id, nombre, idunidadacademica, aplicapregrado, aplicaposgrado)
SELECT b.max_id + row_number() OVER (ORDER BY f.id_unidad), 'No aplica', f.id_unidad, true, true
FROM faltantes f
CROSS JOIN base b;

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

UPDATE tiporeportealma
SET nombre = 'Indirecta'
WHERE id = 2
  AND nombre IS DISTINCT FROM 'Indirecta';

UPDATE canalcontacto
SET nombre = 'Telefónico'
WHERE id = 2
  AND nombre IS DISTINCT FROM 'Telefónico';

INSERT INTO catalogoalmaitem (catalogo, codigo, etiqueta, activo) VALUES
    ('parentescos-solicitante', 'MADRE_PADRE', 'Madre / Padre', true),
    ('parentescos-solicitante', 'HIJA_HIJO', 'Hija / Hijo', true),
    ('parentescos-solicitante', 'HERMANA_HERMANO', 'Hermana / Hermano', true),
    ('parentescos-solicitante', 'OTRO_FAMILIAR', 'Otro familiar', true),
    ('parentescos-solicitante', 'PAREJA', 'Pareja o cónyuge', true),
    ('parentescos-solicitante', 'EXPAREJA', 'Expareja', true),
    ('parentescos-solicitante', 'AMISTAD', 'Amiga / Amigo', true),
    ('parentescos-solicitante', 'COMPANERIA_ACADEMICA', 'Compañera / Compañero de estudio', true),
    ('parentescos-solicitante', 'COMPANERIA_TRABAJO', 'Compañera / Compañero de trabajo', true),
    ('parentescos-solicitante', 'DOCENTE', 'Docente', true),
    ('parentescos-solicitante', 'PERSONAL_ADMINISTRATIVO_UDEA', 'Personal administrativo de la Universidad', true),
    ('parentescos-solicitante', 'VECINA_VECINO', 'Vecina / Vecino', true),
    ('parentescos-solicitante', 'CONOCIDA_OTRO_VINCULO', 'Persona conocida (otro vínculo)', true),
    ('parentescos-solicitante', 'SIN_VINCULO_PREVIO', 'Sin vínculo previo', true),
    ('parentescos-solicitante', 'OTRO', 'Otro (¿Cuál?)', true),
    ('parentescos-solicitante', 'NO_INFORMA', 'No informa', true),
    ('destinos-internos', 'EA', 'Equipo de atención', true),
    ('destinos-internos', 'SPYB', 'Seguridad a personas y bienes', true),
    ('destinos-internos', 'UAD', 'Unidad de Asuntos Disciplinarios 3-4', true),
    ('destinos-internos', 'MASCULINIDADES', 'Programa de masculinidades', true),
    ('destinos-internos', 'BIENESTAR_FACULTAD', 'Bienestar universitario de la facultad, escuela o instituto correspondiente', true),
    ('destinos-internos', 'NO_ACEPTA_NO_DECIDE', 'No acepta/No toma ninguna decisión en esta sesión', true),
    ('destinos-internos', 'NO_APLICA', 'No aplica', true),
    ('rutas-externas', 'CODIGO_FUCSIA', 'Código Fucsia (ACV)', true),
    ('rutas-externas', 'COMISARIA_FAMILIA', 'Comisaría de Familia - CDF', true),
    ('rutas-externas', 'DEFENSORIA_PUEBLO', 'Defensoría del Pueblo', true),
    ('rutas-externas', 'DUPLA_PSICOJURIDICA_SECRETARIA_MUJERES_MEDELLIN', 'Dupla Psicojurídica - Secretaría Mujeres Medellín', true),
    ('rutas-externas', 'FISCALIA', 'Fiscalía General de la Nación - FGN', true),
    ('rutas-externas', 'GERENCIA_DIVERSIDADES_MEDELLIN', 'Gerencia de Diversidades Sexuales e Identidades de Género - Medellín', true),
    ('rutas-externas', 'INSPECCION_POLICIA', 'Inspección de Policía', true),
    ('rutas-externas', 'LINEA_123_AGENCIA_MUJER_MEDELLIN', 'Línea 123 Agencia Mujer Medellín', true),
    ('rutas-externas', 'LINEA_123_MUJER_ANTIOQUIA', 'Línea 123 Mujer Antioquia', true),
    ('rutas-externas', 'LINEA_122_FISCALIA', 'Línea 122 - Fiscalía', true),
    ('rutas-externas', 'LINEA_155_NACIONAL', 'Línea 155 - Nacional', true),
    ('rutas-externas', 'MODULO_DIVERSO_LINEA_123_SOCIAL', 'Módulo Diverso Línea 123 Social', true),
    ('rutas-externas', 'PROCURADURIA', 'Procuraduría General de la Nación', true),
    ('rutas-externas', 'RUTA_SALUD', 'Ruta de salud', true),
    ('rutas-externas', 'OTRA', 'Otra (¿Cuál?)', true),
    ('rutas-externas', 'NO_ACEPTA_NO_DECIDE', 'No acepta/No toma ninguna decisión en esta sesión', true),
    ('vinculos-agresor', 'AMIGO_AMIGA', 'Amigo/amiga', true),
    ('vinculos-agresor', 'PAREJA', 'Pareja', true),
    ('vinculos-agresor', 'EXPAREJA', 'Expareja', true),
    ('vinculos-agresor', 'CONOCIDO', 'Conocido', true),
    ('vinculos-agresor', 'FAMILIAR', 'Familiar', true),
    ('vinculos-agresor', 'DESCONOCIDO', 'Persona desconocida', true),
    ('vinculos-agresor', 'DOCENTE', 'Docente', true),
    ('vinculos-agresor', 'EMPLEADO_UNIVERSIDAD', 'Empleado de la universidad', true),
    ('vinculos-agresor', 'COMPANERO_ESTUDIO', 'Compañero o compañera de estudio', true),
    ('vinculos-agresor', 'COMPANERO_TRABAJO', 'Compañero o compañera de trabajo', true),
    ('vinculos-agresor', 'COMPARTEN_LUGAR_ESTUDIO', 'Comparten lugar de estudio', true),
    ('vinculos-agresor', 'COMPARTEN_LUGAR_TRABAJO', 'Comparten lugar de trabajo', true),
    ('vinculos-agresor', 'JEFE', 'Jefe', true),
    ('vinculos-agresor', 'OTRO', 'Otro', true),
    ('vinculos-agresor', 'NO_TIENE_VINCULO', 'No tiene vínculo', true)
ON CONFLICT (catalogo, codigo) DO UPDATE
SET etiqueta = EXCLUDED.etiqueta,
    activo = true;

UPDATE catalogoalmaitem
SET activo = false
WHERE (catalogo = 'destinos-internos' AND codigo IN ('DUPLA_PSICOJURIDICA', 'PSICOORIENTACION_GENERO'))
   OR (catalogo = 'rutas-externas' AND codigo IN ('LINEA_123', 'POLICIA_NACIONAL', 'ICBF', 'CAIVAS'));

UPDATE catalogoalmaitem
SET activo = false
WHERE catalogo = 'vinculos-agresor'
  AND codigo NOT IN (
      'AMIGO_AMIGA', 'PAREJA', 'EXPAREJA', 'CONOCIDO', 'FAMILIAR',
      'DOCENTE', 'EMPLEADO_UNIVERSIDAD', 'COMPANERO_ESTUDIO', 'COMPANERO_TRABAJO',
      'COMPARTEN_LUGAR_ESTUDIO', 'COMPARTEN_LUGAR_TRABAJO', 'JEFE', 'DESCONOCIDO',
      'OTRO', 'NO_TIENE_VINCULO'
  );

COMMIT;
