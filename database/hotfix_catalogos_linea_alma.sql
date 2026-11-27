-- Hotfix de catálogos de Línea Alma según la revisión de flujos.
-- Ejecutar después de que existan las tablas del módulo y catalogoalmaitem.
-- Reemplaza el contenido de sexo, orientacionsexual, identidadgenero, campus,
-- regimen y eps por las opciones de los formularios. Esos catálogos son
-- compartidos, así que el cambio aplica a todos los formularios que los usan.
-- No modifica las rutas de Línea 123.
-- Requiere desplegar primero el backend que homologa vinculoudea por nombre.

BEGIN;

-- Catálogos compartidos: quedan exactamente con las opciones de los formularios,
-- numeradas en el orden del documento. Los registros que usaban una opción que
-- continúa (aunque cambie de nombre) la conservan; los que usaban una opción
-- retirada pasan a la opción de respaldo indicada al invocar la función.
CREATE TEMPORARY TABLE catalogo_objetivo (
    tabla text NOT NULL,
    posicion integer NOT NULL,
    nombre text NOT NULL,
    codigo text,
    PRIMARY KEY (tabla, posicion)
) ON COMMIT DROP;

CREATE TEMPORARY TABLE catalogo_equivalencia (
    tabla text NOT NULL,
    nombre_anterior text NOT NULL,
    nombre text NOT NULL,
    PRIMARY KEY (tabla, nombre_anterior)
) ON COMMIT DROP;

CREATE TEMPORARY TABLE catalogo_mapa (
    tabla text NOT NULL,
    id_anterior integer NOT NULL,
    id_nuevo integer NOT NULL,
    PRIMARY KEY (tabla, id_anterior)
) ON COMMIT DROP;

INSERT INTO catalogo_objetivo (tabla, posicion, nombre, codigo) VALUES
    ('sexo', 1, 'Femenino', 'F'),
    ('sexo', 2, 'Masculino', 'M'),
    ('sexo', 3, 'Intersexual', 'I'),
    ('sexo', 4, 'Tránsgenero', 'T'),
    ('sexo', 5, 'Prefiere no responder', 'PR'),
    ('sexo', 6, 'Sin dato', 'SD');

INSERT INTO catalogo_objetivo (tabla, posicion, nombre)
SELECT 'orientacionsexual', posicion, nombre FROM unnest(ARRAY[
    'Asexual',
    'Bisexual',
    'Heterosexual',
    'Homosexual',
    'Lesbiana',
    'Pansexual',
    'Otra',
    'Prefiere no responder',
    'Sin dato'
]) WITH ORDINALITY AS opciones(nombre, posicion);

INSERT INTO catalogo_objetivo (tabla, posicion, nombre)
SELECT 'identidadgenero', posicion, nombre FROM unnest(ARRAY[
    'Mujer cisgénero',
    'Hombre cisgénero',
    'Mujer trans',
    'Hombre trans',
    'No binaria',
    'Prefiere no responder',
    'Otra',
    'Sin dato'
]) WITH ORDINALITY AS opciones(nombre, posicion);

INSERT INTO catalogo_objetivo (tabla, posicion, nombre)
SELECT 'campus', posicion, nombre FROM unnest(ARRAY[
    'Campus Amalfi',
    'Campus Andes',
    'Campus Apartadó',
    'Campus Carepa',
    'Campus Caucasia',
    'Campus El Carmen de Viboral',
    'Campus en Urabá',
    'Campus Medellín - Antigua Escuela de Derecho',
    'Campus Medellín - Casas Patrimoniales',
    'Campus Medellín - Ciudad Universitaria',
    'Campus Medellín - Ciudadela Robledo',
    'Campus Medellín - Edificio Antioquia',
    'Campus Medellín - Edificio de Extensión',
    'Campus Medellín - Edificio San Ignacio',
    'Campus Medellín - en el Área de la salud',
    'Campus Medellín - Sede de Posgrado',
    'Campus Puerto Berrío',
    'Campus Santa Fe de Antioquia',
    'Campus Segovia',
    'Campus Sonsón',
    'Campus Turbo',
    'Campus Yarumal',
    'No aplica',
    'Desconocido',
    'Otro-a'
]) WITH ORDINALITY AS opciones(nombre, posicion);

INSERT INTO catalogo_objetivo (tabla, posicion, nombre)
SELECT 'regimen', posicion, nombre FROM unnest(ARRAY[
    'Contributivo',
    'Subsidiado',
    'Especial o de Excepción'
]) WITH ORDINALITY AS opciones(nombre, posicion);

-- Las EPS 25 a 29 pertenecen al régimen especial o de excepción.
INSERT INTO catalogo_objetivo (tabla, posicion, nombre)
SELECT 'eps', posicion, nombre FROM unnest(ARRAY[
    'Nueva EPS',
    'EPS Sanitas',
    'EPS Sura',
    'Salud Total EPS',
    'Compensar EPS',
    'Famisanar',
    'Coosalud EPS',
    'Mutual Ser',
    'Savia Salud',
    'Capital Salud',
    'Aliansalud',
    'Comfenalco Valle',
    'Servicio Occidental de Salud (SOS)',
    'Emssanar',
    'Asmet Salud',
    'Cajacopi EPS',
    'Capresoca',
    'Salud Mía',
    'EPS Familiar de Colombia',
    'Anas Wayuu',
    'Asociación Indígena del Cauca - AIC',
    'Dusakawi',
    'Mallamas',
    'Pijaos Salud',
    'Dirección General de Sanidad Militar (DGSM)',
    'Dirección de Sanidad de la Policía Nacional (DISAN)',
    'Fondo Nacional de Prestaciones Sociales del Magisterio (FOMAG)',
    'Departamento de Salud de Ecopetrol',
    'Unidades de Salud de Universidades Públicas'
]) WITH ORDINALITY AS opciones(nombre, posicion);

-- Opciones que continúan con otro nombre.
INSERT INTO catalogo_equivalencia (tabla, nombre_anterior, nombre) VALUES
    ('sexo', 'Mujer', 'Femenino'),
    ('sexo', 'Hombre', 'Masculino'),
    ('regimen', 'Especial', 'Especial o de Excepción'),
    ('eps', 'Sura EPS', 'EPS Sura'),
    ('eps', 'Sanitas', 'EPS Sanitas'),
    ('eps', 'Salud Total', 'Salud Total EPS'),
    ('eps', 'Compensar', 'Compensar EPS'),
    ('eps', 'SOS EPS', 'Servicio Occidental de Salud (SOS)'),
    ('eps', 'EPS SAVIA SALUD', 'Savia Salud');

CREATE OR REPLACE FUNCTION pg_temp.reemplazar_catalogo(p_tabla text, p_respaldo text) RETURNS void AS $$
DECLARE
    con_codigo boolean;
    id_respaldo integer;
    secuencia text;
    fk record;
BEGIN
    EXECUTE format('LOCK TABLE %I IN SHARE ROW EXCLUSIVE MODE', p_tabla);
    SELECT posicion INTO STRICT id_respaldo FROM catalogo_objetivo
    WHERE tabla = p_tabla AND nombre = p_respaldo;
    SELECT bool_or(codigo IS NOT NULL) INTO con_codigo FROM catalogo_objetivo WHERE tabla = p_tabla;

    EXECUTE format($sql$
        INSERT INTO catalogo_mapa (tabla, id_anterior, id_nuevo)
        SELECT %1$L, actual.id, COALESCE(objetivo.posicion, %2$s)
        FROM %1$I actual
        LEFT JOIN catalogo_equivalencia equivalencia
            ON equivalencia.tabla = %1$L
           AND lower(btrim(equivalencia.nombre_anterior)) = lower(btrim(actual.nombre))
        LEFT JOIN catalogo_objetivo objetivo
            ON objetivo.tabla = %1$L
           AND lower(btrim(objetivo.nombre)) = lower(btrim(COALESCE(equivalencia.nombre, actual.nombre)))
        $sql$, p_tabla, id_respaldo);

    -- Nombres provisionales para no chocar con las restricciones de unicidad.
    EXECUTE format($sql$UPDATE %I SET nombre = '~' || id$sql$, p_tabla);
    IF con_codigo THEN
        EXECUTE format($sql$UPDATE %I SET codigo = '~' || id$sql$, p_tabla);
    END IF;

    -- Crear los ID definitivos que falten copiando las demás columnas de una fila existente.
    EXECUTE format($sql$
        INSERT INTO %1$I
        SELECT (jsonb_populate_record(NULL::%1$I,
                    COALESCE(plantilla.fila, '{}'::jsonb) || jsonb_build_object(
                        'id', objetivo.posicion,
                        'nombre', '~' || objetivo.posicion,
                        'codigo', '~' || objetivo.posicion))).*
        FROM catalogo_objetivo objetivo
        LEFT JOIN (SELECT to_jsonb(fila) AS fila FROM %1$I fila LIMIT 1) plantilla ON true
        WHERE objetivo.tabla = %1$L
          AND objetivo.posicion NOT IN (SELECT id FROM %1$I)
        $sql$, p_tabla);

    FOR fk IN
        SELECT restriccion.conrelid::regclass AS tabla_origen, columna.attname AS columna
        FROM pg_constraint restriccion
        JOIN pg_attribute columna
            ON columna.attrelid = restriccion.conrelid
           AND columna.attnum = restriccion.conkey[1]
        WHERE restriccion.contype = 'f'
          AND restriccion.confrelid = p_tabla::regclass
          AND cardinality(restriccion.conkey) = 1
    LOOP
        EXECUTE format($sql$
            UPDATE %1$s origen SET %2$I = mapa.id_nuevo
            FROM catalogo_mapa mapa
            WHERE mapa.tabla = %3$L
              AND origen.%2$I = mapa.id_anterior
              AND mapa.id_anterior <> mapa.id_nuevo
            $sql$, fk.tabla_origen, fk.columna, p_tabla);
    END LOOP;

    EXECUTE format($sql$
        UPDATE %1$I actual SET nombre = objetivo.nombre
        FROM catalogo_objetivo objetivo
        WHERE objetivo.tabla = %1$L AND objetivo.posicion = actual.id
        $sql$, p_tabla);
    IF con_codigo THEN
        EXECUTE format($sql$
            UPDATE %1$I actual SET codigo = objetivo.codigo
            FROM catalogo_objetivo objetivo
            WHERE objetivo.tabla = %1$L AND objetivo.posicion = actual.id
            $sql$, p_tabla);
    END IF;
    EXECUTE format($sql$
        DELETE FROM %1$I
        WHERE id NOT IN (SELECT posicion FROM catalogo_objetivo WHERE tabla = %1$L)
        $sql$, p_tabla);

    secuencia := pg_get_serial_sequence(p_tabla, 'id');
    IF secuencia IS NOT NULL THEN
        EXECUTE format('SELECT setval(%L, (SELECT max(id) FROM %I))', secuencia, p_tabla);
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS epsregimen (
    idregimen integer NOT NULL REFERENCES regimen(id),
    ideps integer NOT NULL REFERENCES eps(id),
    PRIMARY KEY (idregimen, ideps)
);

DELETE FROM epsregimen;

-- Régimen y EPS no tienen una opción «sin dato»: se usa la misma que el backend
-- asigna por defecto al crear un caso (ID 1).
DO $$
BEGIN
    PERFORM pg_temp.reemplazar_catalogo('sexo', 'Sin dato');
    PERFORM pg_temp.reemplazar_catalogo('orientacionsexual', 'Sin dato');
    PERFORM pg_temp.reemplazar_catalogo('identidadgenero', 'Sin dato');
    PERFORM pg_temp.reemplazar_catalogo('campus', 'Desconocido');
    PERFORM pg_temp.reemplazar_catalogo('regimen', 'Contributivo');
    PERFORM pg_temp.reemplazar_catalogo('eps', 'Nueva EPS');

    IF EXISTS (SELECT 1 FROM pg_attribute
               WHERE attrelid = 'campus'::regclass AND attname = 'activo' AND NOT attisdropped) THEN
        UPDATE campus SET activo = true WHERE activo IS NOT TRUE;
    END IF;
END $$;

INSERT INTO epsregimen (idregimen, ideps)
SELECT regimen.posicion, eps.posicion
FROM catalogo_objetivo regimen
JOIN catalogo_objetivo eps ON eps.tabla = 'eps'
WHERE regimen.tabla = 'regimen'
  AND (regimen.nombre = 'Especial o de Excepción') = (eps.posicion >= 25);

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
    ('rutas-externas', 'LINEA_122_FISCALIA', 'Línea 122 - Fiscalía', true),
    ('rutas-externas', 'LINEA_155_NACIONAL', 'Línea 155 - Nacional', true),
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
   OR (catalogo = 'rutas-externas' AND codigo IN ('POLICIA_NACIONAL', 'ICBF', 'CAIVAS'));

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
