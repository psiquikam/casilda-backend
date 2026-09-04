```md
# Contrato para la creación de endpoints

## 1. Objetivo

Todo endpoint solicitado por el frontend debe definirse mediante este contrato antes de iniciar su implementación.

El contrato debe especificar claramente:

- Ruta y verbo HTTP.
- Autenticación requerida.
- Roles permitidos.
- Campos de entrada.
- Campos de salida.
- Validaciones.
- Errores esperados.
- Ejemplos de uso.

---

## 2. Información general

- **Nombre funcional:**
- **Módulo:**
- **Descripción:**
- **Responsable frontend:**
- **Responsable backend:**
- **Fecha de solicitud:**
- **Versión del contrato:**

---

## 3. Definición del endpoint

- **Método HTTP:** `GET` / `POST` / `PUT` / `PATCH` / `DELETE`
- **Ruta:**
- **Descripción funcional:**
- **Requiere autenticación:** Sí / No
- **Content-Type de entrada:** `application/json`
- **Content-Type de salida:** `application/json`

---

## 4. Autenticación y permisos

### 4.1 Tipo de acceso

Seleccionar una opción:

- **PÚBLICO:** no requiere autenticación.
- **AUTENTICADO:** requiere un token JWT válido.
- **ROLES_ESPECÍFICOS:** requiere un token JWT válido y uno de los roles autorizados.

### 4.2 Roles permitidos

- **Regla de acceso:** `PÚBLICO` / `AUTENTICADO` / `UNO_DE_LOS_ROLES`
- **Roles permitidos:**

```text
- 
- 
- 
```

Los roles son valores configurables almacenados en la tabla `rol`. No deben tratarse como una lista cerrada, ya que pueden agregarse nuevos roles en el futuro.

Ejemplos válidos:

```text
Roles permitidos:
- ADMIN
```

```text
Roles permitidos:
- ADMIN
- COORDINADOR
- PROFESIONAL
```

Cuando se indiquen varios roles, la condición será **OR**: el usuario podrá acceder si tiene cualquiera de los roles definidos.

Actualmente, cada usuario tiene un único rol mediante la relación entre `usuario.idrol` y `rol.id`. Por esta razón, no se deben definir permisos que requieran simultáneamente varios roles mediante una condición AND.

### 4.3 Equivalencia en Spring Security

Un único rol:

```java
@PreAuthorize("hasRole('ADMIN')")
```

Varios roles alternativos:

```java
@PreAuthorize("hasAnyRole('ADMIN', 'COORDINADOR')")
```

La implementación debe usar los roles definidos en el contrato. No se deben inventar roles durante la implementación.

### 4.4 Respuestas de seguridad

| Situación | Código HTTP |
|---|---:|
| Token ausente | 401 Unauthorized |
| Token inválido | 401 Unauthorized |
| Token expirado | 401 Unauthorized |
| Usuario inactivo | 401 Unauthorized |
| Usuario autenticado sin rol permitido | 403 Forbidden |

---

## 5. Parámetros de entrada

### 5.1 Parámetros de ruta

| Nombre | Tipo | Obligatorio | Descripción | Validaciones |
|---|---|---:|---|---|
|  |  |  |  |  |

Ejemplo:

| Nombre | Tipo | Obligatorio | Descripción | Validaciones |
|---|---|---:|---|---|
| id | Long | Sí | Identificador del recurso | Debe ser positivo |

### 5.2 Parámetros de consulta

| Nombre | Tipo | Obligatorio | Valor por defecto | Descripción | Validaciones |
|---|---|---:|---|---|---|
|  |  |  |  |  |  |

Ejemplo:

| Nombre | Tipo | Obligatorio | Valor por defecto | Descripción | Validaciones |
|---|---|---:|---|---|---|
| page | Integer | No | 0 | Número de página | Mayor o igual a 0 |
| size | Integer | No | 10 | Registros por página | Mayor que 0 |

### 5.3 Headers

| Header | Obligatorio | Descripción | Ejemplo |
|---|---:|---|---|
| Authorization | Según autenticación | Token JWT | `Bearer <token>` |
| Content-Type | Si existe body | Formato del request | `application/json` |

---

## 6. Body de entrada

### 6.1 DTO de request

- **Nombre del DTO:**
- **Body requerido:** Sí / No

### 6.2 Ejemplo de request

```json
{
  "campo": "valor"
}
```

### 6.3 Definición de campos

| Campo | Tipo | Obligatorio | Acepta null | Descripción | Validaciones | Valor por defecto |
|---|---|---:|---:|---|---|---|
|  |  |  |  |  |  |  |

Cada campo debe especificar:

- Tipo de dato.
- Si es obligatorio.
- Si acepta `null`.
- Longitud máxima y mínima.
- Formato esperado.
- Valor por defecto.
- Si debe ser único.
- Si referencia otra entidad.
- Qué ocurre si la referencia no existe.

### 6.4 Reglas de validación

- 
- 
- 
- 

---

## 7. Respuesta exitosa

### 7.1 Código HTTP

Seleccionar una opción:

- `200 OK`: consulta, actualización o acción exitosa.
- `201 Created`: creación exitosa.
- `204 No Content`: eliminación exitosa sin contenido en la respuesta.

### 7.2 DTO de response

- **Nombre del DTO:**

### 7.3 Ejemplo de response

```json
{
  "id": 1,
  "campo": "valor"
}
```

### 7.4 Definición de campos de respuesta

| Campo | Tipo | Obligatorio | Acepta null | Descripción | Formato |
|---|---|---:|---:|---|---|
|  |  |  |  |  |  |

Las respuestas no deben exponer directamente entidades JPA. Siempre deben utilizar DTOs de respuesta.

---

## 8. Paginación

Completar esta sección si el endpoint devuelve una lista.

- **Tiene paginación:** Sí / No
- **Parámetro de página:**
- **Parámetro de tamaño:**
- **Tamaño máximo permitido:**
- **Ordenamiento por defecto:**
- **Dirección:** Ascendente / Descendente

Ejemplo de response paginada:

```json
{
  "content": [
    {
      "id": 1,
      "campo": "valor"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

## 9. Errores esperados

| Código HTTP | Situación | Mensaje esperado |
|---|---|---|
| 400 | Body inválido o campos faltantes |  |
| 401 | Token ausente, inválido o expirado |  |
| 403 | Usuario sin el rol requerido |  |
| 404 | Recurso no encontrado |  |
| 409 | Registro duplicado o conflicto de negocio |  |
| 500 | Error inesperado del servidor |  |

### 9.1 Ejemplo de error

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "El campo email es obligatorio",
  "timestamp": "2026-09-03T10:00:00"
}
```

---

## 10. Reglas de negocio

Describir aquí las reglas específicas del endpoint:

1. 
2. 
3. 
4. 

Ejemplos:

- El email debe ser único.
- El rol indicado debe existir en la tabla `rol`.
- Un usuario inactivo no puede iniciar sesión.
- Sólo se puede actualizar la contraseña cuando se envía un nuevo valor.
- El usuario autenticado sólo puede consultar sus propios registros.

---

## 11. Ejemplo completo

### Crear usuario

- **Método:** `POST`
- **Ruta:** `/api/v1/usuarios`
- **Descripción:** Crea un usuario del sistema.
- **Autenticación:** Sí
- **Regla de autorización:** `UNO_DE_LOS_ROLES`
- **Roles permitidos:** `ADMIN`

### Request

```json
{
  "nombre": "Juan Pérez",
  "email": "juan.perez@udea.edu.co",
  "password": "ClaveSegura123!",
  "idRol": 2,
  "activo": true
}
```

### Campos del request

| Campo | Tipo | Obligatorio | Acepta null | Validaciones |
|---|---|---:|---:|---|
| nombre | String | Sí | No | No vacío, máximo 100 caracteres |
| email | String | Sí | No | Formato email, debe ser único |
| password | String | Sí | No | No vacío, mínimo 8 caracteres |
| idRol | Integer | Sí | No | Debe existir en la tabla `rol` |
| activo | Boolean | No | Sí | Por defecto `true` |

### Response `201 Created`

```json
{
  "id": 15,
  "nombre": "Juan Pérez",
  "email": "juan.perez@udea.edu.co",
  "idRol": 2,
  "nombreRol": "PROFESIONAL",
  "activo": true,
  "fechaCreacion": "2026-09-03T09:30:00",
  "fechaActualizacion": "2026-09-03T09:30:00"
}
```

### Errores

- `400`: datos inválidos.
- `401`: usuario no autenticado.
- `403`: el usuario no tiene el rol `ADMIN`.
- `404`: el rol indicado no existe.
- `409`: el email ya está registrado.

---

## 12. Reglas obligatorias de implementación

- Cada endpoint debe tener definida su ruta y verbo HTTP.
- Cada endpoint protegido debe indicar sus roles permitidos.
- Los roles deben considerarse valores dinámicos y extensibles.
- No se debe usar una condición AND entre roles.
- Si hay varios roles permitidos, la condición será OR.
- Cada request con body debe utilizar un DTO.
- Cada respuesta debe utilizar un DTO.
- No se deben retornar entidades JPA directamente.
- Los campos obligatorios deben validarse en el backend.
- Las referencias a otras entidades deben validarse.
- Los errores `400`, `401`, `403`, `404` y `409` deben documentarse cuando apliquen.
- Los endpoints protegidos deben recibir `Authorization: Bearer <token>`.
- La documentación OpenAPI/Swagger debe actualizarse.
- Deben probarse los casos exitosos, errores de validación y permisos.
- El frontend no debe asumir que sólo existen los roles actuales.
- El frontend debe tratar los roles como valores recibidos dinámicamente desde el backend.

---

## 13. Criterios de aceptación

- [ ] La ruta y el verbo HTTP están definidos.
- [ ] La autenticación está definida.
- [ ] Los roles permitidos están definidos.
- [ ] Se especificó si los roles funcionan como OR.
- [ ] Los parámetros de ruta están documentados.
- [ ] Los parámetros de consulta están documentados.
- [ ] El DTO de request está definido.
- [ ] El DTO de response está definido.
- [ ] Los campos obligatorios están identificados.
- [ ] Las validaciones están documentadas.
- [ ] Los códigos HTTP están definidos.
- [ ] Los errores esperados están documentados.
- [ ] Existen ejemplos de request y response.
- [ ] Las reglas de negocio están descritas.
- [ ] OpenAPI/Swagger fue actualizado.
- [ ] Se probaron autenticación y autorización.
- [ ] Se probaron las validaciones.
- [ ] Se verificó que el frontend pueda consumir el endpoint sin ambigüedades.
```
