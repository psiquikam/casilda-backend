-- Dimensiones provisionales compatibles con el formulario de Psicología.
-- No sobrescribir cambios o desactivaciones posteriores de los catálogos.
INSERT INTO catalogoalmaitem (catalogo, codigo, etiqueta, activo)
SELECT 'dimensiones-salud-mental', 'DIM_' || lpad(n::text, 2, '0'), 'Dimensión ' || n, true
FROM generate_series(1, 15) AS n
ON CONFLICT (catalogo, codigo) DO NOTHING;

INSERT INTO catalogoalmaitem (catalogo, codigo, etiqueta, activo) VALUES
    ('vinculos-agresor', 'AMIGO_AMIGA', 'Amigo/amiga', true),
    ('vinculos-agresor', 'PAREJA', 'Pareja', true),
    ('vinculos-agresor', 'EXPAREJA', 'Expareja', true),
    ('vinculos-agresor', 'CONOCIDO', 'Conocido', true),
    ('vinculos-agresor', 'FAMILIAR', 'Familiar', true),
    ('vinculos-agresor', 'DOCENTE', 'Docente', true),
    ('vinculos-agresor', 'EMPLEADO_UNIVERSIDAD', 'Empleado de la universidad', true),
    ('vinculos-agresor', 'COMPANERO_ESTUDIO', 'Compañero o compañera de estudio', true),
    ('vinculos-agresor', 'COMPANERO_TRABAJO', 'Compañero o compañera de trabajo', true),
    ('vinculos-agresor', 'COMPARTEN_LUGAR_ESTUDIO', 'Comparten lugar de estudio', true),
    ('vinculos-agresor', 'COMPARTEN_LUGAR_TRABAJO', 'Comparten lugar de trabajo', true),
    ('vinculos-agresor', 'JEFE', 'Jefe', true),
    ('vinculos-agresor', 'DESCONOCIDO', 'Persona desconocida', true),
    ('vinculos-agresor', 'OTRO', 'Otro', true),
    ('vinculos-agresor', 'NO_TIENE_VINCULO', 'No tiene vínculo', true)
ON CONFLICT (catalogo, codigo) DO NOTHING;
