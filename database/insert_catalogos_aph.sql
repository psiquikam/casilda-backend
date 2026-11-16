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
    ('parentescos-solicitante', 'MADRE_PADRE', 'Madre o padre', true),
    ('parentescos-solicitante', 'PAREJA', 'Pareja', true),
    ('parentescos-solicitante', 'AMISTAD', 'Amistad', true),
    ('parentescos-solicitante', 'COMPANERIA_ACADEMICA', 'Compañía académica', true),
    ('parentescos-solicitante', 'DOCENTE', 'Docente', true),
    ('parentescos-solicitante', 'OTRO', 'Otro', true),
    ('destinos-internos', 'SPYB', 'Seguridad a personas y bienes', true),
    ('destinos-internos', 'EA', 'Equipo de atención', true),
    ('destinos-internos', 'DUPLA_PSICOJURIDICA', 'Dupla psicojurídica', true),
    ('destinos-internos', 'MASCULINIDADES', 'Masculinidades', true),
    ('destinos-internos', 'UAD', 'Unidad de asuntos disciplinarios', true),
    ('rutas-externas', 'LINEA_123', 'Línea 123', true),
    ('rutas-externas', 'CODIGO_FUCSIA', 'Código fucsia', true),
    ('rutas-externas', 'FISCALIA', 'Fiscalía', true),
    ('rutas-externas', 'COMISARIA_FAMILIA', 'Comisaría de familia', true),
    ('rutas-externas', 'POLICIA_NACIONAL', 'Policía nacional', true),
    ('rutas-externas', 'ICBF', 'Instituto Colombiano de Bienestar Familiar', true),
    ('rutas-externas', 'CAIVAS', 'Centro de atención a víctimas de violencia sexual', true),
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
