-- Hotfix de catálogos de Línea Alma según la revisión de flujos.
-- Ejecutar después de que existan las tablas del módulo y catalogoalmaitem.
-- Conserva los códigos históricos, desactiva solo opciones reemplazadas
-- y no elimina registros ni datos de atenciones existentes.
-- Requiere desplegar primero el backend que homologa vinculoudea por nombre.

BEGIN;

INSERT INTO sexo (id, codigo, nombre) VALUES
    (1, 'F', 'Femenino'),
    (2, 'M', 'Masculino'),
    (3, 'I', 'Intersexual'),
    (4, 'T', 'Tránsgenero'),
    (5, 'PR', 'Prefiere no responder'),
    (6, 'SD', 'Sin dato')
ON CONFLICT (id) DO UPDATE
SET codigo = EXCLUDED.codigo,
    nombre = EXCLUDED.nombre;

INSERT INTO orientacionsexual (id, nombre) VALUES
    (1, 'Asexual'),
    (2, 'Bisexual'),
    (3, 'Heterosexual'),
    (4, 'Homosexual'),
    (5, 'Lesbiana'),
    (6, 'Pansexual'),
    (7, 'Otra'),
    (8, 'Prefiere no responder'),
    (9, 'Sin dato')
ON CONFLICT (id) DO UPDATE
SET nombre = EXCLUDED.nombre;

INSERT INTO identidadgenero (id, nombre) VALUES
    (1, 'Mujer cisgénero'),
    (2, 'Hombre cisgénero'),
    (3, 'Mujer trans'),
    (4, 'Hombre trans'),
    (5, 'No binaria'),
    (6, 'Prefiere no responder'),
    (7, 'Otra'),
    (8, 'Sin dato')
ON CONFLICT (id) DO UPDATE
SET nombre = EXCLUDED.nombre;

INSERT INTO campus (id, nombre) VALUES
    (1, 'Campus Amalfi'),
    (2, 'Campus Andes'),
    (3, 'Campus Apartadó'),
    (4, 'Campus Carepa'),
    (5, 'Campus Caucasia'),
    (6, 'Campus El Carmen de Viboral'),
    (7, 'Campus en Urabá'),
    (8, 'Campus Medellín - Antigua Escuela de Derecho'),
    (9, 'Campus Medellín - Casas Patrimoniales'),
    (10, 'Campus Medellín - Ciudad Universitaria'),
    (11, 'Campus Medellín - Ciudadela Robledo'),
    (12, 'Campus Medellín - Edificio Antioquia'),
    (13, 'Campus Medellín - Edificio de Extensión'),
    (14, 'Campus Medellín - Edificio San Ignacio'),
    (15, 'Campus Medellín - en el Área de la salud'),
    (16, 'Campus Medellín - Sede de Posgrado'),
    (17, 'Campus Puerto Berrío'),
    (18, 'Campus Santa Fe de Antioquia'),
    (19, 'Campus Segovia'),
    (20, 'Campus Sonsón'),
    (21, 'Campus Turbo'),
    (22, 'Campus Yarumal'),
    (23, 'No aplica'),
    (24, 'Desconocido'),
    (25, 'Otro-a')
ON CONFLICT (id) DO UPDATE
SET nombre = EXCLUDED.nombre;

INSERT INTO regimen (id, nombre) VALUES
    (1, 'Contributivo'),
    (2, 'Subsidiado'),
    (3, 'Especial o de Excepción')
ON CONFLICT (id) DO UPDATE
SET nombre = EXCLUDED.nombre;

INSERT INTO eps (id, nombre) VALUES
    (1, 'Nueva EPS'),
    (2, 'EPS Sanitas'),
    (3, 'EPS Sura'),
    (4, 'Salud Total EPS'),
    (5, 'Compensar EPS'),
    (6, 'Famisanar'),
    (7, 'Coosalud EPS'),
    (8, 'Mutual Ser'),
    (9, 'Savia Salud'),
    (10, 'Capital Salud'),
    (11, 'Aliansalud'),
    (12, 'Comfenalco Valle'),
    (13, 'Servicio Occidental de Salud (SOS)'),
    (14, 'Emssanar'),
    (15, 'Asmet Salud'),
    (16, 'Cajacopi EPS'),
    (17, 'Capresoca'),
    (18, 'Salud Mía'),
    (19, 'EPS Familiar de Colombia'),
    (20, 'Anas Wayuu'),
    (21, 'Asociación Indígena del Cauca - AIC'),
    (22, 'Dusakawi'),
    (23, 'Mallamas'),
    (24, 'Pijaos Salud'),
    (25, 'Dirección General de Sanidad Militar (DGSM)'),
    (26, 'Dirección de Sanidad de la Policía Nacional (DISAN)'),
    (27, 'Fondo Nacional de Prestaciones Sociales del Magisterio (FOMAG)'),
    (28, 'Departamento de Salud de Ecopetrol'),
    (29, 'Unidades de Salud de Universidades Públicas')
ON CONFLICT (id) DO UPDATE
SET nombre = EXCLUDED.nombre;

CREATE TABLE IF NOT EXISTS epsregimen (
    idregimen integer NOT NULL REFERENCES regimen(id),
    ideps integer NOT NULL REFERENCES eps(id),
    PRIMARY KEY (idregimen, ideps)
);

DELETE FROM epsregimen;
INSERT INTO epsregimen (idregimen, ideps)
SELECT 1, id FROM eps WHERE id BETWEEN 1 AND 24
UNION ALL
SELECT 2, id FROM eps WHERE id BETWEEN 1 AND 24
UNION ALL
SELECT 3, id FROM eps WHERE id BETWEEN 25 AND 29;

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
