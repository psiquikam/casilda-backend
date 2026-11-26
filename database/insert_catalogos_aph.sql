-- Ejecutar después de que Hibernate cree catalogoalmaitem.
-- Estos catálogos se mantienen como datos de base de datos: versionar aquí
-- cualquier cambio futuro de códigos o etiquetas.
-- No se carga texto de consentimiento hasta recibir una versión oficial.
INSERT INTO catalogoalmaitem (catalogo, codigo, etiqueta, activo) VALUES
    ('estratos', '1', 'Estrato 1', true),
    ('estratos', '2', 'Estrato 2', true),
    ('estratos', '3', 'Estrato 3', true),
    ('estratos', '4', 'Estrato 4', true),
    ('estratos', '5', 'Estrato 5', true),
    ('estratos', '6', 'Estrato 6', true),
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
    ('destinos-internos', 'SPYB', 'Seguridad a personas y bienes', true),
    ('destinos-internos', 'EA', 'Equipo de atención', true),
    ('destinos-internos', 'UAD', 'Unidad de Asuntos Disciplinarios 3-4', true),
    ('destinos-internos', 'PSICOORIENTACION_GENERO', 'Psicoorientación con perspectiva de género', true),
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
    ('resultados-ruta-externa', 'ATENDIDA', 'Atendida', true),
    ('resultados-ruta-externa', 'SIN_RESPUESTA', 'Sin respuesta', true),
    ('resultados-ruta-externa', 'NO_ACEPTA', 'No acepta', true),
    ('motivos-cierre-aph', 'NO_ACEPTA_PSICOLOGIA', 'No acepta psicología', true),
    ('motivos-cierre-aph', 'ORIENTACION_BRINDADA', 'Orientación brindada', true),
    ('motivos-cierre-aph', 'REMITIDA_RUTA_EXTERNA', 'Remitida a ruta externa', true),
    ('motivos-cierre-aph', 'SIN_RESPUESTA', 'Sin respuesta', true)
ON CONFLICT (catalogo, codigo) DO UPDATE
SET etiqueta = EXCLUDED.etiqueta,
    activo = EXCLUDED.activo;

UPDATE catalogoalmaitem
SET activo = false
WHERE (catalogo = 'destinos-internos' AND codigo = 'DUPLA_PSICOJURIDICA')
   OR (catalogo = 'rutas-externas' AND codigo IN ('LINEA_123', 'POLICIA_NACIONAL', 'ICBF', 'CAIVAS'));
