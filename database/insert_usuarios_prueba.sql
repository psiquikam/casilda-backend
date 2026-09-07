-- ============================================================================
-- SCRIPT DE INSERCIÓN DE USUARIOS DE PRUEBA
-- Sistema CASILDA - FNSP
-- ============================================================================
-- NOTA: Todas las contraseñas están hasheadas con BCrypt
-- Contraseña por defecto para todos los usuarios: "Casilda2024!"
-- Hash BCrypt: $2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru
-- ============================================================================

-- ============================================================================
-- USUARIOS ADMINISTRADORES
-- ============================================================================
INSERT INTO usuario (email, password, nombre, idrol, activo) VALUES
('admin@udea.edu.co', '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru', 'Laura Estella Pineda Corcho', 1, true),
('admin2@udea.edu.co', '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru', 'Isabel Cristina Jaramillo González', 1, true);

-- ============================================================================
-- USUARIOS COORDINADORES
-- ============================================================================
INSERT INTO usuario (email, password, nombre, idrol, activo) VALUES
('coordinador.atencion@udea.edu.co', '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru', 'Laura Estella Pineda Corcho', 2, true),
('coordinador.prevencion@udea.edu.co', '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru', 'Isabel Cristina Jaramillo González', 2, true);

-- ============================================================================
-- USUARIOS PROFESIONALES
-- ============================================================================
-- Dupla 1
INSERT INTO usuario (email, password, nombre, idrol, activo) VALUES
('andrea.salazar@udea.edu.co', '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru', 'Andrea Salazar Morales', 3, true),
('carmen.sanchez@udea.edu.co', '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru', 'Carmen Andrea Sánchez Hernández', 3, true);

-- Dupla 2
INSERT INTO usuario (email, password, nombre, idrol, activo) VALUES
('diana.sanchez@udea.edu.co', '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru', 'Diana Cristina Sánchez Pérez', 3, true),
('manuela.morales@udea.edu.co', '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru', 'Manuela Morales Duque', 3, true);

-- Otros profesionales
INSERT INTO usuario (email, password, nombre, idrol, activo) VALUES
('laura.valencia@udea.edu.co', '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru', 'Laura Valencia Ruíz', 3, true),
('lina.rodas@udea.edu.co', '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru', 'Lina María Rodas', 3, true);

-- ============================================================================
-- USUARIOS REVISORES
-- ============================================================================
INSERT INTO usuario (email, password, nombre, idrol, activo) VALUES
('revisor1@udea.edu.co', '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru', 'Revisor Administrativo Uno', 4, true),
('revisor2@udea.edu.co', '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru', 'Revisor Administrativo Dos', 4, true);

-- ============================================================================
-- USUARIOS REGULARES (para pruebas)
-- ============================================================================
INSERT INTO usuario (email, password, nombre, idrol, activo) VALUES
('usuario.estudiante@udea.edu.co', '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru', 'María Camila Gómez Pérez', 5, true),
('usuario.docente@udea.edu.co', '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru', 'Carlos Alberto Ramírez López', 5, true),
('usuario.admin@udea.edu.co', '$2a$10$rjll9Epf8UWi7HeH5kmBaulRKTHMP7TZ8/zWEmpadDn7SFdXpjKru', 'Sofía Martínez Torres', 5, true);

INSERT INTO usuariorol (idusuario, idrol)
SELECT id, idrol
FROM usuario
WHERE email IN (
    'admin@udea.edu.co',
    'admin2@udea.edu.co',
    'coordinador.atencion@udea.edu.co',
    'coordinador.prevencion@udea.edu.co',
    'andrea.salazar@udea.edu.co',
    'carmen.sanchez@udea.edu.co',
    'diana.sanchez@udea.edu.co',
    'manuela.morales@udea.edu.co',
    'laura.valencia@udea.edu.co',
    'lina.rodas@udea.edu.co',
    'revisor1@udea.edu.co',
    'revisor2@udea.edu.co',
    'usuario.estudiante@udea.edu.co',
    'usuario.docente@udea.edu.co',
    'usuario.admin@udea.edu.co'
)
ON CONFLICT (idusuario, idrol) DO NOTHING;

-- Ejemplo de usuarios con múltiples roles.
INSERT INTO usuariorol (idusuario, idrol)
SELECT u.id, r.id
FROM usuario u
JOIN rol r ON r.codigo = 'COORDINADOR'
WHERE u.email = 'admin@udea.edu.co'
ON CONFLICT (idusuario, idrol) DO NOTHING;

INSERT INTO usuariorol (idusuario, idrol)
SELECT u.id, r.id
FROM usuario u
JOIN rol r ON r.codigo = 'REVISOR'
WHERE u.email = 'coordinador.atencion@udea.edu.co'
ON CONFLICT (idusuario, idrol) DO NOTHING;

-- ============================================================================
-- NOTAS IMPORTANTES
-- ============================================================================
-- 1. Cambiar las contraseñas en producción
-- 2. Los emails deben ser únicos en la tabla
-- 3. Las fechas se establecen automáticamente con DEFAULT CURRENT_TIMESTAMP
-- 4. Los usuarios están activos por defecto
-- 
-- ROLES:
-- 1 = Admin (Administración completa del sistema)
-- 2 = Coordinador (Gestión de casos, asignaciones y reportes)
-- 3 = Profesional (Atención de casos, registro de atenciones)
-- 4 = Revisor (Revisión y aprobación de casos)
-- 5 = Usuario (Usuario regular del sistema, puede crear solicitudes)
-- ============================================================================

-- ============================================================================
-- SCRIPT PARA GENERAR HASH DE CONTRASEÑAS (Node.js/Java)
-- ============================================================================
-- Node.js (usando bcryptjs):
-- const bcrypt = require('bcryptjs');
-- const hash = bcrypt.hashSync('Casilda2024!', 10);
-- console.log(hash);
--
-- Java (usando Spring Security BCryptPasswordEncoder):
-- BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
-- String hash = encoder.encode("Casilda2024!");
-- System.out.println(hash);
-- ============================================================================
