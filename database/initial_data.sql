-- Datos iniciales para ejecutar después de que Hibernate cree las tablas.
-- Este archivo no crea tablas.

INSERT INTO rol (id, codigo, nombre, activo) VALUES
    (1, 'ADMIN', 'Admin', true),
    (2, 'COORDINADOR', 'Coordinador', true),
    (3, 'PROFESIONAL', 'Profesional', true),
    (4, 'REVISOR', 'Revisor', true),
    (5, 'USUARIO', 'Usuario', true),
    (6, 'GESTOR_CONTENIDO', 'Gestor de contenidos', true)
ON CONFLICT (id) DO UPDATE
SET codigo = EXCLUDED.codigo,
    nombre = EXCLUDED.nombre,
    activo = EXCLUDED.activo;

INSERT INTO endpoint (path, http_method, activo, publico) VALUES
    ('/auth/**', 'ANY', true, true),
    ('/api-docs/**', 'ANY', true, true),
    ('/swagger-ui/**', 'ANY', true, true),
    ('/swagger-ui.html', 'ANY', true, true),
    ('/usuarios', 'GET', true, false),
    ('/usuarios/paginado', 'GET', true, false),
    ('/usuarios/{id}', 'GET', true, false),
    ('/usuarios', 'POST', true, false),
    ('/usuarios/{id}', 'PUT', true, false),
    ('/usuarios/{id}', 'DELETE', true, false),
    ('/usuarios/{id}/estado', 'PATCH', true, false),
    ('/casos/**', 'GET', true, false),
    ('/casos/**', 'POST', true, false),
    ('/solicitudes/**', 'GET', true, false),
    ('/solicitudes/**', 'POST', true, false),
    ('/solicitudes/**', 'PUT', true, false),
    ('/solicitudes/**', 'DELETE', true, false),
    ('/atenciones/**', 'GET', true, false),
    ('/atenciones/**', 'POST', true, false),
    ('/citas/**', 'GET', true, false),
    ('/citas/**', 'PUT', true, false),
    ('/compromisos/**', 'GET', true, false),
    ('/compromisos/**', 'POST', true, false),
    ('/compromisos/**', 'DELETE', true, false),
    ('/linea-alma/**', 'GET', true, false),
    ('/linea-alma/**', 'POST', true, false),
    ('/personas/**', 'GET', true, false),
    ('/parametros/**', 'GET', true, false),
    ('/parametros/**', 'PUT', true, false),
    ('/maestros/**', 'GET', true, false),
    ('/maestros/**', 'POST', true, false),
    ('/maestros/**', 'PUT', true, false),
    ('/maestros/**', 'DELETE', true, false),
    ('/contenidos/home', 'GET', true, true),
    ('/contenidos', 'GET', true, false),
    ('/contenidos/{id}', 'GET', true, false),
    ('/contenidos', 'POST', true, false),
    ('/contenidos/{id}', 'PUT', true, false),
    ('/contenidos/{id}', 'DELETE', true, false),
    ('/contenidos/imagenes/**', 'GET', true, true)
ON CONFLICT (path, http_method) DO UPDATE
SET activo = EXCLUDED.activo,
    publico = EXCLUDED.publico;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo IN ('ADMIN', 'COORDINADOR', 'PROFESIONAL', 'REVISOR', 'USUARIO')
WHERE e.http_method = 'GET'
  AND e.path IN ('/maestros/**', '/personas/**')
ON CONFLICT (idendpoint, idrol) DO NOTHING;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo IN ('ADMIN', 'COORDINADOR', 'PROFESIONAL', 'REVISOR')
WHERE e.path IN ('/casos/**', '/atenciones/**', '/citas/**', '/compromisos/**', '/linea-alma/**')
  AND e.http_method IN ('GET', 'POST', 'PUT', 'DELETE')
ON CONFLICT (idendpoint, idrol) DO NOTHING;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo IN ('ADMIN', 'COORDINADOR')
WHERE e.path LIKE '/solicitudes/%'
  AND e.http_method IN ('GET', 'POST', 'PUT', 'DELETE')
ON CONFLICT (idendpoint, idrol) DO NOTHING;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo = 'ADMIN'
WHERE e.path IN ('/usuarios', '/usuarios/paginado', '/usuarios/{id}', '/usuarios/{id}/estado')
   OR e.path = '/parametros/**'
   OR (e.path = '/maestros/**' AND e.http_method IN ('POST', 'PUT', 'DELETE'))
ON CONFLICT (idendpoint, idrol) DO NOTHING;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo = 'USUARIO'
WHERE e.path = '/solicitudes/**'
  AND e.http_method IN ('GET', 'POST')
ON CONFLICT (idendpoint, idrol) DO NOTHING;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id
FROM endpoint e
JOIN rol r ON r.codigo IN ('ADMIN', 'GESTOR_CONTENIDO')
WHERE e.path IN ('/contenidos', '/contenidos/{id}')
  AND e.http_method IN ('GET', 'POST', 'PUT', 'DELETE')
ON CONFLICT (idendpoint, idrol) DO NOTHING;

-- Datos semilla del Home: traducción literal del mock que hoy usa el frontend
-- (ver contrato de API "Contenido dinámico del Home"). Si el backend arranca
-- con exactamente estos 7 registros, el Home se ve igual que hoy. Idempotente:
-- no se reinsertan si ya existen (desempate por título, único dato estable
-- disponible antes de que la tabla tenga datos).
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
