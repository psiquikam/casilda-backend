-- Fixture UAD para pruebas de integración. Reproduce los permisos de database/uad_modulo_entrega1.sql y entrega2.sql con
-- ids explícitos, porque integration-seed.sql inserta rutas con id fijo sin ajustar la secuencia.
-- Se ejecuta después de integration-seed.sql (ambas se declaran en el @Sql de UadIntegrationTest).

INSERT INTO rol (id, codigo, nombre, activo) VALUES
    (7, 'UAD_COORDINACION', 'Coordinación UAD', true),
    (8, 'UAD_EQUIPO_3', 'Profesional UAD Equipo 3', true),
    (9, 'UAD_EQUIPO_4', 'Profesional UAD Equipo 4', true)
ON CONFLICT (id) DO UPDATE SET codigo = EXCLUDED.codigo, nombre = EXCLUDED.nombre, activo = true;

-- Casilda2024! (mismo hash que integration-seed.sql). Datos ficticios. Ids altos: otras pruebas crean usuarios con
-- la secuencia (7, 8...) en el mismo contenedor, y un id fijo bajo chocaría con ellos.
INSERT INTO usuario (id, email, password, nombre, activo, fechacreacion, fechaactualizacion, idrol) VALUES
    (9007, 'uad.coord@udea.edu.co', '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru', 'Coordinación UAD de prueba', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 7),
    (9008, 'uad.e3@udea.edu.co', '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru', 'Profesional E3 de prueba', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 8),
    (9009, 'uad.e4@udea.edu.co', '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru', 'Profesional E4 de prueba', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 9)
ON CONFLICT (id) DO UPDATE SET email = EXCLUDED.email, password = EXCLUDED.password, nombre = EXCLUDED.nombre,
    idrol = EXCLUDED.idrol, activo = true;

INSERT INTO usuariorol (idusuario, idrol) VALUES (9007, 7), (9008, 8), (9009, 9)
ON CONFLICT (idusuario, idrol) DO NOTHING;

INSERT INTO endpoint (id, path, http_method, activo, publico) VALUES
    (900, '/uad/contexto', 'GET', true, false),
    (901, '/uad/noticias', 'GET', true, false),
    (902, '/uad/noticias/*', 'GET', true, false),
    (903, '/uad/noticias/*/clasificacion', 'POST', true, false),
    (904, '/uad/noticias/*/casos-asociados', 'GET', true, false),
    (905, '/uad/noticias/*/decisiones', 'POST', true, false),
    (906, '/uad/noticias/*/historial', 'GET', true, false),
    (907, '/uad/evidencias/*/descarga', 'GET', true, false),
    (908, '/maestros/uad-causales-inhibitorio', 'GET', true, false),
    (909, '/maestros/uad-entidades-remision', 'GET', true, false),
    (910, '/maestros/uad-tipos-solicitud-informacion', 'GET', true, false),
    (911, '/uad/noticias-anonimas', 'POST', true, true),
    (912, '/uad/casos', 'GET', true, false),
    (913, '/uad/casos/*', 'GET', true, false),
    (914, '/uad/casos/*/expediente', 'PATCH', true, false),
    (915, '/uad/casos/*/actuaciones/catalogo', 'GET', true, false),
    (916, '/uad/casos/*/actuaciones', 'POST', true, false),
    (917, '/uad/casos/*/historial', 'GET', true, false),
    (918, '/uad/casos/*/traslado-decision', 'POST', true, false),
    (919, '/uad/casos/*/remisiones-competencia', 'POST', true, false),
    (920, '/uad/casos/*/evidencias', 'GET', true, false)
ON CONFLICT (id) DO UPDATE SET path = EXCLUDED.path, http_method = EXCLUDED.http_method, activo = true, publico = EXCLUDED.publico;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id FROM endpoint e JOIN rol r ON r.codigo IN ('UAD_EQUIPO_3', 'UAD_EQUIPO_4')
WHERE e.id BETWEEN 900 AND 910 OR e.id BETWEEN 912 AND 920
ON CONFLICT (idendpoint, idrol) DO NOTHING;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT 900, r.id FROM rol r WHERE r.codigo IN ('UAD_COORDINACION', 'ADMIN')
ON CONFLICT (idendpoint, idrol) DO NOTHING;

INSERT INTO rutaactivacion (id, codigo, nombre, activo) VALUES (2, 'UAD_3_4', 'UAD 3 y 4 (Unidad de asuntos disciplinarios)', true)
ON CONFLICT (id) DO UPDATE SET codigo = EXCLUDED.codigo, activo = true;

INSERT INTO vinculoudea (id, nombre) VALUES (6, 'Docente Vinculado') ON CONFLICT (id) DO NOTHING;

INSERT INTO uad_catalogo (catalogo, codigo, nombre, orden, activo) VALUES
    ('CAUSAL_INHIBITORIO', 'DUPLICIDAD', 'Duplicidad de actuación en curso', 1, true),
    ('ENTIDAD_REMISION', 'FISCALIA', 'Fiscalía General de la Nación', 1, true),
    ('TIPO_SOLICITUD_INFORMACION', 'ACLARACION_HECHOS', 'Aclaración o ampliación de información', 1, true)
ON CONFLICT (catalogo, codigo) DO NOTHING;
