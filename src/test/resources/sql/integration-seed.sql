-- Seed data is deliberately limited to rows used by integration tests.
INSERT INTO rol (id, nombre, codigo, activo) VALUES
    (1, 'Administrador', 'ADMIN', true),
    (2, 'Coordinador', 'COORDINADOR', true),
    (5, 'Usuario', 'USUARIO', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO usuario (id, email, password, nombre, activo, fechacreacion, fechaactualizacion, idrol)
VALUES
    (1, 'admin@udea.edu.co',
     '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru',
     'Administrador de pruebas', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (2, 'user@udea.edu.co',
     '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru',
     'Usuario de pruebas', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 5)
ON CONFLICT (id) DO NOTHING;

INSERT INTO usuariorol (idusuario, idrol) VALUES
    (1, 1), (1, 2), (2, 5)
ON CONFLICT (idusuario, idrol) DO NOTHING;

INSERT INTO pais (id, codigo, nombre)
VALUES (1, 'CO', 'Colombia')
ON CONFLICT (id) DO NOTHING;

INSERT INTO endpoint (id, path, http_method, activo, publico) VALUES
    (1, '/maestros/paises', 'GET', true, false),
    (2, '/usuarios', 'GET', true, false)
ON CONFLICT (id) DO NOTHING;

INSERT INTO endpointrole (id, idendpoint, idrol) VALUES
    (1, 1, 1),
    (2, 1, 5),
    (3, 2, 1)
ON CONFLICT (id) DO NOTHING;
