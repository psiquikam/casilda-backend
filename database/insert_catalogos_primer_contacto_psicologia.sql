-- Valores provisionales compatibles con el formulario de Psicología.
-- No sobrescribir cambios o desactivaciones posteriores de los catálogos.
INSERT INTO catalogoalmaitem (catalogo, codigo, etiqueta, activo)
SELECT 'dimensiones-salud-mental', 'DIM_' || lpad(n::text, 2, '0'), 'Dimensión ' || n, true
FROM generate_series(1, 15) AS n
ON CONFLICT (catalogo, codigo) DO NOTHING;

INSERT INTO catalogoalmaitem (catalogo, codigo, etiqueta, activo) VALUES
    ('vinculos-agresor', 'PAREJA', 'Pareja', true),
    ('vinculos-agresor', 'EXPAREJA', 'Expareja', true),
    ('vinculos-agresor', 'FAMILIAR', 'Familiar', true),
    ('vinculos-agresor', 'DOCENTE', 'Docente', true),
    ('vinculos-agresor', 'COMPANERO_ESTUDIO', 'Compañero o compañera de estudio', true),
    ('vinculos-agresor', 'COMPANERO_TRABAJO', 'Compañero o compañera de trabajo', true),
    ('vinculos-agresor', 'DESCONOCIDO', 'Persona desconocida', true),
    ('vinculos-agresor', 'OTRO', 'Otro', true)
ON CONFLICT (catalogo, codigo) DO NOTHING;
