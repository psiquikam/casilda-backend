-- Integration fixture mirrors database/initial_data.sql's roles and endpoint
-- matrix.  The application context creates the schema before this script runs.
INSERT INTO rol (id, codigo, nombre, activo) VALUES
    (1, 'ADMIN', 'Administrador', true),
    (2, 'COORDINADOR', 'Coordinador', true),
    (3, 'PROFESIONAL', 'Profesional', true),
    (4, 'REVISOR', 'Revisor', true),
    (5, 'USUARIO', 'Usuario', true),
    (6, 'GESTOR_CONTENIDO', 'Gestor de contenidos', true)
ON CONFLICT (id) DO UPDATE SET codigo = EXCLUDED.codigo, nombre = EXCLUDED.nombre, activo = true;

-- Casilda2024!
INSERT INTO usuario (id, email, password, nombre, activo, fechacreacion, fechaactualizacion, idrol)
VALUES
    (1, 'admin@udea.edu.co', '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru', 'Admin', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (2, 'coordinador@udea.edu.co', '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru', 'Coordinador', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2),
    (3, 'profesional@udea.edu.co', '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru', 'Profesional', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 3),
    (4, 'revisor@udea.edu.co', '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru', 'Revisor', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 4),
    (5, 'user@udea.edu.co', '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru', 'Usuario', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 5),
    (6, 'gestor@udea.edu.co', '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru', 'Gestor de contenidos', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 6)
ON CONFLICT (id) DO UPDATE SET idrol = EXCLUDED.idrol, activo = true;

INSERT INTO usuariorol (idusuario, idrol) VALUES
    (1, 1), (2, 2), (3, 3), (4, 4), (5, 5), (6, 6)
ON CONFLICT (idusuario, idrol) DO NOTHING;

INSERT INTO pais (id, codigo, nombre) VALUES (1, 'CO', 'Colombia')
ON CONFLICT (id) DO NOTHING;

-- Every path/method group from initial_data.sql is represented here.
INSERT INTO endpoint (id, path, http_method, activo, publico) VALUES
    (1, '/usuarios', 'GET', true, false), (2, '/usuarios/paginado', 'GET', true, false),
    (3, '/usuarios/{id}', 'GET', true, false), (4, '/usuarios', 'POST', true, false),
    (5, '/usuarios/{id}', 'PUT', true, false), (6, '/usuarios/{id}', 'DELETE', true, false),
    (7, '/usuarios/{id}/estado', 'PATCH', true, false),
    (8, '/casos/**', 'GET', true, false), (9, '/casos/**', 'POST', true, false),
    (10, '/solicitudes/**', 'GET', true, false), (11, '/solicitudes/**', 'POST', true, false),
    (12, '/solicitudes/**', 'PUT', true, false), (13, '/solicitudes/**', 'DELETE', true, false),
    (14, '/atenciones/**', 'GET', true, false), (15, '/atenciones/**', 'POST', true, false),
    (16, '/citas/**', 'GET', true, false), (17, '/citas/**', 'PUT', true, false),
    (18, '/compromisos/**', 'GET', true, false), (19, '/compromisos/**', 'POST', true, false),
    (20, '/compromisos/**', 'DELETE', true, false),
    (21, '/linea-alma/**', 'GET', true, false), (22, '/linea-alma/**', 'POST', true, false),
    (23, '/personas/**', 'GET', true, false),
    (24, '/parametros/**', 'GET', true, false), (25, '/parametros/**', 'PUT', true, false),
    (26, '/maestros/**', 'GET', true, false), (27, '/maestros/**', 'POST', true, false),
    (28, '/maestros/**', 'PUT', true, false), (29, '/maestros/**', 'DELETE', true, false),
    (30, '/contenidos/home', 'GET', true, true),
    (31, '/contenidos', 'GET', true, false), (32, '/contenidos/{id}', 'GET', true, false),
    (33, '/contenidos', 'POST', true, false), (34, '/contenidos/{id}', 'PUT', true, false),
    (35, '/contenidos/{id}', 'DELETE', true, false),
    (36, '/contenidos/imagenes/**', 'GET', true, true)
ON CONFLICT (id) DO UPDATE SET path = EXCLUDED.path, http_method = EXCLUDED.http_method,
    activo = true, publico = EXCLUDED.publico;

-- Read-only maestros/personas are available to every role.
INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id FROM endpoint e CROSS JOIN rol r
WHERE e.id = 26 OR e.id = 23
ON CONFLICT (idendpoint, idrol) DO NOTHING;

-- Operational groups are available to every staff role (not USUARIO).
INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id FROM endpoint e JOIN rol r ON r.codigo IN ('ADMIN','COORDINADOR','PROFESIONAL','REVISOR')
WHERE e.id IN (8, 9, 14, 15, 16, 17, 18, 19, 20, 21, 22)
ON CONFLICT (idendpoint, idrol) DO NOTHING;

-- Solicitudes GET/POST are also available to USUARIO.
INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id FROM endpoint e JOIN rol r ON r.codigo IN ('ADMIN','COORDINADOR','USUARIO')
WHERE e.id IN (10, 11)
ON CONFLICT (idendpoint, idrol) DO NOTHING;
INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id FROM endpoint e JOIN rol r ON r.codigo IN ('ADMIN','COORDINADOR')
WHERE e.id IN (12, 13)
ON CONFLICT (idendpoint, idrol) DO NOTHING;

-- User administration and parameter/catalog writes are ADMIN-only.
INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, 1 FROM endpoint e WHERE e.id IN (1,2,3,4,5,6,7,24,25,27,28,29)
ON CONFLICT (idendpoint, idrol) DO NOTHING;

-- Administración de contenidos: ADMIN y GESTOR_CONTENIDO (endpoint /contenidos/home queda público, sin roles).
INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id FROM endpoint e JOIN rol r ON r.codigo IN ('ADMIN','GESTOR_CONTENIDO')
WHERE e.id IN (31, 32, 33, 34, 35)
ON CONFLICT (idendpoint, idrol) DO NOTHING;

-- Minimal request graph shared by authorization and update integration tests.
INSERT INTO departamento (id, codigo, nombre) VALUES (1, '05', 'Antioquia')
ON CONFLICT (id) DO NOTHING;
INSERT INTO municipio (id, codigo, nombre, iddepartamento) VALUES (1, '05001', 'Medellín', 1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO tipoidentificacion (id, codigo, nombre) VALUES (1, 'CC', 'Cédula')
ON CONFLICT (id) DO NOTHING;
INSERT INTO tipocorreo (id, nombre) VALUES (1, 'Personal') ON CONFLICT (id) DO NOTHING;
INSERT INTO tipotelefono (id, nombre) VALUES (1, 'Celular') ON CONFLICT (id) DO NOTHING;
INSERT INTO identidadgenero (id, nombre) VALUES (1, 'Mujer') ON CONFLICT (id) DO NOTHING;
INSERT INTO tiposolicitud (id, nombre) VALUES (1, 'Acompañamiento') ON CONFLICT (id) DO NOTHING;
INSERT INTO estadosolicitud (id, nombre) VALUES (1, 'Pendiente') ON CONFLICT (id) DO NOTHING;
INSERT INTO mediosolicitud (id, nombre) VALUES (1, 'Web') ON CONFLICT (id) DO NOTHING;
INSERT INTO cargo (id, nombre) VALUES (1, 'Docente') ON CONFLICT (id) DO NOTHING;
INSERT INTO campus (id, nombre) VALUES (1, 'Medellín') ON CONFLICT (id) DO NOTHING;
INSERT INTO unidadadministrativa (id, nombre) VALUES (1, 'Bienestar') ON CONFLICT (id) DO NOTHING;
INSERT INTO unidadacademica (id, nombre) VALUES (1, 'Derecho') ON CONFLICT (id) DO NOTHING;
INSERT INTO persona (id, primernombre, primerapellido, idtipoidentificacion, numerodocumento, idciudadnacimiento)
VALUES (100, 'Ana', 'Gómez', 1, '100000001', 1), (101, 'Luis', 'Pérez', 1, '100000002', 1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO remision (id, idremitente, idcargo, idunidadadministrativa, idunidadacademica, idcampus, idusuariocreacion)
VALUES (100, 101, 1, 1, 1, 1, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO correopersona (id, idpersona, idtipo, correo)
VALUES (100, 100, 1, 'ana@example.edu.co') ON CONFLICT (id) DO NOTHING;
INSERT INTO telefonopersona (id, idpersona, idtipo, telefono)
VALUES (100, 100, 1, '3000000000') ON CONFLICT (id) DO NOTHING;
INSERT INTO solicitudatencion (id, idsolicitante, idremision, ididentidadgenero, idtiposolicitud,
    idestadosolicitud, idmediosolicitud, idusuariocreacion, observacionestelefono, observacionescorreo)
VALUES (100, 100, 100, 1, 1, 1, 1, 1, 'Antes', 'Antes')
ON CONFLICT (id) DO NOTHING;
