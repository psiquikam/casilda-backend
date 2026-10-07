-- Datos de prueba para los contenidos destacados del Home.
-- Ejecutar después de que Hibernate cree la tabla contenido.
-- Traducción literal del mock del frontend (ver contrato de API
-- "Contenido dinámico del Home"). Con estos 7 registros, el Home se ve igual
-- que el mock. Idempotente: no reinsertar títulos ya existentes.
INSERT INTO contenido (imagen, titulo, contenido, vigencia_inicio, vigencia_fin, enlace, seccion, orden, eliminado)
SELECT * FROM (VALUES
    ('assets/uad_equipo_3_y_4.svg', 'Registrar queja disciplinaria (UAD 3 y 4)',
     'Registra formalmente una queja ante la Unidad de Asuntos Disciplinarios. Tu relato se maneja bajo reserva.',
     TIMESTAMPTZ '2026-01-01T00:00:00Z', NULL::timestamptz, '/formulario-anonimo', 'ACCIONES', 0, false),
    ('assets/equipo_atencion.svg', 'Solicitud al equipo de atención VBG',
     'Contacta al equipo especializado para recibir orientación y acompañamiento psicosocial y jurídico.',
     TIMESTAMPTZ '2026-01-01T00:00:00Z', NULL::timestamptz, '/formulario-anonimo', 'ACCIONES', 1, false),
    ('assets/linea_alma.svg', 'Atención por Línea Alma',
     'Línea de escucha y apoyo psicológico inmediato de la Universidad de Antioquia.',
     TIMESTAMPTZ '2026-01-01T00:00:00Z', NULL::timestamptz, NULL, 'ACCIONES', 2, false),
    ('assets/seguridad_bienes_y_servicios.svg', 'Atención por seguridad a personas y bienes',
     'Reporta incidentes que requieran respuesta de seguridad inmediata dentro del campus.',
     TIMESTAMPTZ '2026-01-01T00:00:00Z', NULL::timestamptz, NULL, 'ACCIONES', 3, false),
    ('assets/reportes_informes_indicadores.svg', 'Indicadores internos',
     'Consulta los datos y métricas que el sistema CASILDA consolida sobre la atención institucional.',
     TIMESTAMPTZ '2026-01-01T00:00:00Z', NULL::timestamptz, NULL, 'INFORMACION', 0, false),
    ('assets/estadisticas-vbg.svg', 'Estadísticas en VBG',
     'Informes sobre la situación de las violencias basadas en género en la Universidad.',
     TIMESTAMPTZ '2026-01-01T00:00:00Z', NULL::timestamptz, NULL, 'INFORMACION', 1, false),
    ('assets/distintivo_casilda_morado.svg', '¿Quién es CASILDA?',
     'Es el sistema de vigilancia en salud pública de la UdeA para el abordaje de las discriminaciones y violencias basadas en género. Centraliza la información para prevenir, atender y proteger.',
     TIMESTAMPTZ '2026-01-01T00:00:00Z', NULL::timestamptz, NULL, 'INFORMACION', 2, false)
) AS semilla(imagen, titulo, contenido, vigencia_inicio, vigencia_fin, enlace, seccion, orden, eliminado)
WHERE NOT EXISTS (SELECT 1 FROM contenido c WHERE c.titulo = semilla.titulo);