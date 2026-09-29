# Contrato para la creación y autorización de nuevos roles

## 1. Objetivo

Este documento define el contrato que debe cumplirse antes de crear un nuevo rol
en CASILDA. Su propósito es dejar explícito el nivel de acceso del rol para cada
endpoint y evitar permisos implícitos, ambiguos o excesivos.

Ningún rol nuevo debe implementarse únicamente con base en su nombre. La solicitud
debe incluir la matriz completa de permisos y contar con aprobación funcional y
técnica.

## 2. Alcance

Este contrato aplica a:

- La creación de roles de negocio.
- La modificación de permisos de roles existentes.
- La asociación de roles con endpoints.
- La desactivación de roles o permisos.
- Las pruebas de autorización asociadas al cambio.

La ruta base de la API es `/api/v1`. En la base de datos, la columna `endpoint.path`
se registra sin el prefijo `/api/v1`.

## 3. Niveles de acceso

Cada combinación de rol, ruta y método HTTP debe tener exactamente uno de estos
niveles:

| Nivel | Significado |
|---|---|
| **PÚBLICO** | El endpoint no requiere autenticación. Solo debe usarse para operaciones explícitamente públicas, como autenticación o documentación. |
| **LECTURA** | Permite consultar información mediante métodos `GET`. |
| **CREACIÓN** | Permite crear información mediante `POST`. |
| **ACTUALIZACIÓN** | Permite modificar información mediante `PUT` o `PATCH`. |
| **ELIMINACIÓN** | Permite eliminar información mediante `DELETE`. |
| **GESTIÓN TOTAL** | Permite todas las operaciones definidas para el recurso. Debe justificarse expresamente. |
| **SIN ACCESO** | El endpoint no está asociado al rol. Una solicitud autenticada debe responder `403 Forbidden`. |

Los niveles se asignan por método HTTP. Por ejemplo, tener **LECTURA** sobre
`/solicitudes/**` no concede automáticamente permiso para `POST`, `PUT` o `DELETE`.

## 4. Reglas generales de autorización

1. Todo endpoint no público requiere un token JWT válido.
2. Un usuario puede acceder si al menos uno de sus roles está asociado al endpoint.
3. La ausencia de asociación equivale a **SIN ACCESO**.
4. No se deben crear permisos globales para todas las rutas salvo que exista una
   justificación de seguridad y aprobación explícita.
5. Los roles deben identificarse mediante un código único en mayúsculas, por
   ejemplo `COORDINADOR`.
6. Un rol inactivo no puede autorizar solicitudes.
7. Los endpoints nuevos deben incluir su método HTTP, patrón de ruta y estado
   (`activo`) en la matriz de permisos.
8. Las rutas con comodines deben usarse solo cuando todos los endpoints incluidos
   tengan el mismo nivel de sensibilidad y autorización.
9. El acceso a datos debe respetar también las reglas de negocio y pertenencia de
   los recursos; tener permiso sobre un endpoint no elimina esas validaciones.

## 5. Matriz vigente de referencia

La siguiente matriz refleja la configuración de roles y endpoints de
`database/initial_data.sql`. El bloque final de ese archivo inserta datos semilla
para el Home y no forma parte de esta configuración de permisos.

| Recurso o patrón | Métodos | ADMIN | COORDINADOR | PROFESIONAL | REVISOR | USUARIO | GESTOR_CONTENIDO |
|---|---|---:|---:|---:|---:|---:|---:|
| `/auth/**` | Cualquier método | PÚBLICO | PÚBLICO | PÚBLICO | PÚBLICO | PÚBLICO | PÚBLICO |
| `/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` | Cualquier método | PÚBLICO | PÚBLICO | PÚBLICO | PÚBLICO | PÚBLICO | PÚBLICO |
| `/maestros/**` | `GET` | LECTURA | LECTURA | LECTURA | LECTURA | LECTURA | SIN ACCESO |
| `/maestros/**` | `POST`, `PUT`, `DELETE` | GESTIÓN TOTAL | SIN ACCESO | SIN ACCESO | SIN ACCESO | SIN ACCESO | SIN ACCESO |
| `/personas/**` | `GET` | LECTURA | LECTURA | LECTURA | LECTURA | LECTURA | SIN ACCESO |
| `/casos/**` | `GET`, `POST` | GESTIÓN TOTAL | GESTIÓN TOTAL | GESTIÓN TOTAL | GESTIÓN TOTAL | SIN ACCESO | SIN ACCESO |
| `/atenciones/**` | `GET`, `POST` | GESTIÓN TOTAL | GESTIÓN TOTAL | GESTIÓN TOTAL | GESTIÓN TOTAL | SIN ACCESO | SIN ACCESO |
| `/citas/**` | `GET`, `PUT` | GESTIÓN TOTAL | GESTIÓN TOTAL | GESTIÓN TOTAL | GESTIÓN TOTAL | SIN ACCESO | SIN ACCESO |
| `/compromisos/**` | `GET`, `POST`, `DELETE` | GESTIÓN TOTAL | GESTIÓN TOTAL | GESTIÓN TOTAL | GESTIÓN TOTAL | SIN ACCESO | SIN ACCESO |
| `/linea-alma/**` | `GET`, `POST` | GESTIÓN TOTAL | GESTIÓN TOTAL | GESTIÓN TOTAL | GESTIÓN TOTAL | SIN ACCESO | SIN ACCESO |
| `/solicitudes/**` | `GET`, `POST` | GESTIÓN TOTAL | GESTIÓN TOTAL | SIN ACCESO | SIN ACCESO | CREACIÓN y LECTURA | SIN ACCESO |
| `/usuarios` | `GET`, `POST` | GESTIÓN TOTAL | SIN ACCESO | SIN ACCESO | SIN ACCESO | SIN ACCESO | SIN ACCESO |
| `/usuarios/paginado` | `GET` | LECTURA | SIN ACCESO | SIN ACCESO | SIN ACCESO | SIN ACCESO | SIN ACCESO |
| `/usuarios/{id}` | `GET`, `PUT`, `DELETE` | GESTIÓN TOTAL | SIN ACCESO | SIN ACCESO | SIN ACCESO | SIN ACCESO | SIN ACCESO |
| `/usuarios/{id}/estado` | `PATCH` | ACTUALIZACIÓN | SIN ACCESO | SIN ACCESO | SIN ACCESO | SIN ACCESO | SIN ACCESO |
| `/parametros/**` | `GET`, `PUT` | GESTIÓN TOTAL | SIN ACCESO | SIN ACCESO | SIN ACCESO | SIN ACCESO | SIN ACCESO |
| `/contenidos/home` | `GET` | PÚBLICO | PÚBLICO | PÚBLICO | PÚBLICO | PÚBLICO | PÚBLICO |
| `/contenidos` | `GET` | LECTURA | SIN ACCESO | SIN ACCESO | SIN ACCESO | SIN ACCESO | LECTURA |
| `/contenidos/{id}` | `GET` | LECTURA | SIN ACCESO | SIN ACCESO | SIN ACCESO | SIN ACCESO | LECTURA |
| `/contenidos` | `POST` | CREACIÓN | SIN ACCESO | SIN ACCESO | SIN ACCESO | SIN ACCESO | CREACIÓN |
| `/contenidos/{id}` | `PUT` | ACTUALIZACIÓN | SIN ACCESO | SIN ACCESO | SIN ACCESO | SIN ACCESO | ACTUALIZACIÓN |
| `/contenidos/{id}` | `DELETE` | ELIMINACIÓN | SIN ACCESO | SIN ACCESO | SIN ACCESO | SIN ACCESO | ELIMINACIÓN |
| `/contenidos/imagenes/**` | `GET` | PÚBLICO | PÚBLICO | PÚBLICO | PÚBLICO | PÚBLICO | PÚBLICO |

Los patrones no listados en esta matriz no deben considerarse autorizados por
defecto. Deben agregarse al contrato antes de asignarles permisos.

## 6. Solicitud de un nuevo rol

### 6.1 Información general

- **Nombre funcional del rol:**
- **Código del rol:**
- **Descripción:**
- **Área o proceso responsable:**
- **Responsable funcional:**
- **Responsable técnico:**
- **Fecha de solicitud:**
- **Versión del contrato:**
- **Motivo de creación:**

### 6.2 Alcance del rol

- **Responsabilidades principales:**
- **Información que puede consultar:**
- **Información que puede crear:**
- **Información que puede modificar:**
- **Información que puede eliminar:**
- **Información que nunca debe consultar o modificar:**
- **Restricciones por estado, dependencia, programa o usuario:**

### 6.3 Matriz de permisos solicitada

Debe completarse una fila por cada endpoint protegido que el rol pueda utilizar.
También deben registrarse explícitamente los grupos relevantes en los que el rol
no tendrá acceso.

| Endpoint o patrón | Método | Nivel de acceso | Justificación funcional | Datos restringidos | Aprobado |
|---|---|---|---|---|---|
| `/recurso/**` | `GET` | LECTURA |  |  |  |
| `/recurso/**` | `POST` | CREACIÓN |  |  |  |
| `/recurso/{id}` | `PUT` | ACTUALIZACIÓN |  |  |  |
| `/recurso/{id}` | `DELETE` | ELIMINACIÓN |  |  |  |

### 6.4 Casos que deben definirse

La solicitud debe indicar el comportamiento esperado para:

- Usuario autenticado con el nuevo rol y permiso concedido.
- Usuario autenticado con el nuevo rol sin permiso.
- Usuario con otro rol autorizado.
- Usuario con varios roles, cuando aplique.
- Usuario sin token.
- Token inválido o expirado.
- Usuario inactivo.
- Endpoint o método no registrado en la matriz.

## 7. Implementación técnica obligatoria

La implementación de un nuevo rol debe incluir:

1. Registro del rol con código, nombre y estado activo.
2. Registro de los endpoints y métodos que utilizará.
3. Asociación del rol con cada endpoint permitido.
4. Actualización de la documentación de permisos.
5. Pruebas de acceso permitido y denegado.
6. Pruebas de acceso anónimo a endpoints protegidos.
7. Verificación de que los roles existentes no recibieron permisos nuevos por
   accidente.
8. Revisión de los datos de prueba y de la matriz de `database/initial_data.sql`.

Las asociaciones deben ser explícitas. No se debe reutilizar una asociación
existente solo porque el nombre del recurso parezca equivalente.

## 8. Códigos de respuesta esperados

| Situación | Respuesta esperada |
|---|---:|
| Acceso público permitido | `200`, `201` o el código definido por el endpoint |
| Token ausente en endpoint protegido | `401 Unauthorized` o el comportamiento documentado por la configuración vigente |
| Token inválido o expirado | `401 Unauthorized` |
| Usuario autenticado sin permiso | `403 Forbidden` |
| Endpoint o método no autorizado | `403 Forbidden` |
| Solicitud autorizada pero recurso inexistente | `404 Not Found` |

Las pruebas deben validar el comportamiento real de la configuración de seguridad
vigente y documentar cualquier diferencia respecto de esta tabla.

## 9. Aprobaciones

El cambio no debe pasar a implementación hasta contar con:

- **Aprobación funcional:** confirma que el rol y sus permisos corresponden al
  proceso de negocio.
- **Aprobación de seguridad:** confirma que no existe sobreasignación de permisos.
- **Aprobación técnica:** confirma que la matriz, los endpoints y las pruebas son
  consistentes.

| Aprobación | Nombre | Fecha | Estado |
|---|---|---|---|
| Funcional |  |  | Pendiente |
| Seguridad |  |  | Pendiente |
| Técnica |  |  | Pendiente |

## 10. Control de cambios

Toda modificación posterior debe registrar:

- Rol afectado.
- Endpoint y método HTTP afectados.
- Permiso anterior y permiso nuevo.
- Justificación.
- Riesgo identificado.
- Pruebas ejecutadas.
- Aprobaciones obtenidas.

