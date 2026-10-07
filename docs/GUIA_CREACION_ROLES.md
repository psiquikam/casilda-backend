# Guía de creación de roles y asignación de usuarios

Esta guía realiza todos los cambios directamente en PostgreSQL. No se usan endpoints HTTP.

## Modelo de autorización

- `rol`: catálogo de roles. `id` es entero y se asigna manualmente; código y nombre son únicos.
- `endpoint`: catálogo de rutas y métodos. La combinación `(path, http_method)` es única; su ID lo genera la base de datos.
- `endpointrole`: asociación entre un rol y un endpoint.
- `usuario.idrol`: rol principal obligatorio del usuario.
- `usuariorol`: roles efectivos del usuario. Mantener esta tabla sincronizada con `usuario.idrol`; para un usuario con varios roles, registrar todas las asociaciones aquí.

En `initial_data.sql` ya están ocupados los IDs de rol del 1 al 6, incluido `GESTOR_CONTENIDO`. No reutilizar el ID 6. Los paths se guardan sin el contexto `/api/v1`.

## Antes de empezar

1. Ejecutar los scripts después de que Hibernate haya creado las tablas.
2. Respaldar la base de datos y usar una transacción para los cambios.
3. Definir el código, nombre, rutas y métodos que realmente requiere el rol. No conceder permisos por comodines sin validar el alcance.
4. La contraseña debe estar almacenada como hash BCrypt. Nunca insertar texto plano. Generar el hash antes del SQL con `BCryptPasswordEncoder` o una herramienta equivalente.

Los ejemplos usan `AUDITOR` con permiso de lectura en dos rutas existentes. Sustituirlo por el rol y los permisos aprobados.

## Opción A: procedimiento almacenado

El procedimiento valida los datos, crea el rol, registra o reactiva los endpoints protegidos y crea las asociaciones en una sola transacción. No crea usuarios. No permite asociar endpoints marcados como públicos.

Desde `psql`, en la raíz del proyecto, instalarlo una vez:

```text
\set ON_ERROR_STOP on
\i database/crear_rol_con_permisos.sql
```

Crear el rol y los permisos. El bloqueo evita que dos altas calculen el mismo ID; el procedimiento toma el mismo bloqueo durante su ejecución:

```text
BEGIN;
SELECT pg_advisory_xact_lock(74839201);
SELECT COALESCE(MAX(id), 0) + 1 AS nuevo_id FROM rol
\gset

CALL crear_rol_con_permisos(
    :'nuevo_id',
    'AUDITOR',
    'Auditor',
    '[
      {"path":"/personas/**","http_method":"GET"},
      {"path":"/casos/**","http_method":"GET"}
    ]'::jsonb,
    true
);
COMMIT;
```

El procedimiento solo acepta `GET`, `POST`, `PUT`, `PATCH` y `DELETE`. Para endpoints públicos no se crea una fila en `endpointrole`; estos deben seguir configurados con `publico = true`.

## Opción B: SQL manual

Usar esta alternativa solo cuando se necesite controlar explícitamente cada inserción. El ejemplo usa comandos de `psql` para guardar el siguiente ID libre en una variable y ejecuta la creación del rol, los endpoints y sus permisos en la misma transacción.

```text
\set ON_ERROR_STOP on
BEGIN;
SELECT pg_advisory_xact_lock(74839201);
SELECT COALESCE(MAX(id), 0) + 1 AS nuevo_id FROM rol
\gset

INSERT INTO rol (id, codigo, nombre, activo)
VALUES (:'nuevo_id', 'AUDITOR', 'Auditor', true);

INSERT INTO endpoint (path, http_method, activo, publico) VALUES
    ('/personas/**', 'GET', true, false),
    ('/casos/**', 'GET', true, false)
ON CONFLICT (path, http_method) DO UPDATE
SET activo = EXCLUDED.activo;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, :'nuevo_id'
FROM endpoint e
WHERE (e.path, e.http_method) IN (
    ('/personas/**', 'GET'),
    ('/casos/**', 'GET')
)
  AND e.activo = true
  AND e.publico = false
ON CONFLICT (idendpoint, idrol) DO NOTHING;

COMMIT;
```

Si una ruta ya existe como pública, no se debe asociar al rol. Revisar `endpoint.publico` antes de crear una asociación. El `ON CONFLICT` del catálogo reactiva el endpoint, pero conserva su estado público actual.

## Crear un usuario y asignarle el rol

Este bloque funciona después de crear el rol, tanto con el procedimiento como manualmente. Reemplazar el correo, nombre y marcador de contraseña por un hash BCrypt real. El ID del usuario lo genera la base de datos; el bloque añade el rol principal en `usuario.idrol` y también su asociación en `usuariorol`.

```sql
DO $$
DECLARE
    id_rol_nuevo integer;
    id_usuario_nuevo bigint;
BEGIN
    SELECT id
    INTO id_rol_nuevo
    FROM rol
    WHERE codigo = 'AUDITOR'
      AND activo = true;

    IF id_rol_nuevo IS NULL THEN
        RAISE EXCEPTION 'No existe un rol AUDITOR activo';
    END IF;

    INSERT INTO usuario (
        email, password, nombre, activo,
        fechacreacion, fechaactualizacion, idrol
    ) VALUES (
        'auditor@udea.edu.co',
        '<REEMPLAZAR_POR_HASH_BCRYPT_REAL>',
        'Usuario Auditor',
        true,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        id_rol_nuevo
    )
    RETURNING id INTO id_usuario_nuevo;

    INSERT INTO usuariorol (idusuario, idrol)
    VALUES (id_usuario_nuevo, id_rol_nuevo)
    ON CONFLICT (idusuario, idrol) DO NOTHING;
END;
$$;
```

El marcador no es un hash válido: debe reemplazarse antes de ejecutar el bloque. No reutilizar una contraseña de prueba en producción. Si se agrega un rol secundario a un usuario existente, insertar la asociación en `usuariorol`; cambiar `usuario.idrol` solo si también se quiere cambiar su rol principal.

## Verificación

```sql
SELECT id, codigo, nombre, activo
FROM rol
WHERE codigo = 'AUDITOR';

SELECT e.path, e.http_method, e.activo, e.publico
FROM endpointrole er
JOIN endpoint e ON e.id = er.idendpoint
JOIN rol r ON r.id = er.idrol
WHERE r.codigo = 'AUDITOR'
ORDER BY e.path, e.http_method;

SELECT u.email, u.activo, rp.codigo AS rol_principal, r.codigo AS rol_asociado
FROM usuario u
JOIN rol rp ON rp.id = u.idrol
LEFT JOIN usuariorol ur ON ur.idusuario = u.id
LEFT JOIN rol r ON r.id = ur.idrol
WHERE u.email = 'auditor@udea.edu.co';
```

Comprobar que el usuario está activo, que el rol asociado está activo y que cada permiso apunta a un endpoint activo no público. Registrar el SQL usado en control de versiones para repetir la configuración en otros ambientes.