# Objetivos de pruebas — Reportar caso

Este documento define los objetivos verificables para la implementación del contrato de
creación de reportes de caso descrito en
[`contrato-reportar-caso.md`](../../Descargas/contrato-reportar-caso.md).

## Alcance

La primera versión de las pruebas cubre el backend del flujo compartido por:

- `POST /reportes` sin autenticación, para el canal anónimo.
- `POST /reportes` con un JWT válido, para el canal autenticado.
- Los maestros consumidos por el formulario:
  - `GET /maestros/tipos-identificacion`
  - `GET /maestros/campus`
  - `GET /maestros/rutas-activacion`

La prueba debe verificar el comportamiento HTTP, la validación del contrato, la seguridad y
la persistencia real en PostgreSQL. No se debe considerar suficiente comprobar solamente que
el controlador devuelve una respuesta con forma válida.

## Objetivos principales

### OBJ-01 — Crear un reporte anónimo

Verificar que una petición válida sin `Authorization`:

1. Es aceptada por `POST /reportes`.
2. Devuelve `201 Created`.
3. Devuelve un `codigo` generado por el servidor con prefijo `CAS-`.
4. Devuelve `id`, `fechaRegistro` y el estado de la ruta solicitada.
5. Persiste el reporte antes de responder.
6. No asocia el reporte con una persona autenticada.

### OBJ-02 — Crear un reporte autenticado

Verificar que la misma operación con un JWT válido:

1. Devuelve `201 Created`.
2. Persiste los mismos datos funcionales del reporte.
3. Asocia el reporte con la identidad del token, según la decisión aprobada para D3.
4. No requiere un campo `modo` en el cuerpo.
5. No expone datos de autenticación en la respuesta.

### OBJ-03 — Garantizar unicidad y no adivinabilidad del código CAS

Verificar que:

1. Cada reporte recibe un código distinto.
2. El código se persiste con una restricción `UNIQUE`.
3. Dos creaciones concurrentes no producen el mismo código.
4. El código no es un consecutivo corto fácilmente enumerable.
5. Una colisión de código se reintenta o falla explícitamente; nunca responde éxito con un
   reporte no persistido.

### OBJ-04 — Validar el DTO completo en el servidor

Verificar que el backend repite las reglas del frontend y devuelve todos los errores de una
petición en `validationErrors`, usando exactamente las claves del DTO:

- Documento parcial: `tipoDocumentoId` sin `numeroDocumento` o viceversa.
- Longitudes y caracteres inválidos para CC, TI, CE, PAS y tipos genéricos.
- `fechaNacimiento` igual o posterior a la fecha actual de `America/Bogota`.
- TI con una persona de 18 años cumplidos o más.
- `descripcionHechos` ausente, vacía, con espacios o mayor a 4.000 caracteres.
- `rutaActivacionId` inexistente o inactiva.
- `tipoDocumentoId` inexistente o inactivo.
- `tipoLugar` distinto de `interno`, `externo` o `null`.
- Lugar interno sin `campusId`.
- Lugar externo sin `lugarDetalleExterno`.
- Campos de lugar incompatibles con el tipo de lugar.

El objetivo incluye comprobar que los mensajes son aptos para mostrarse junto al control
correspondiente y que no se devuelve un error genérico sin clave de campo.

### OBJ-05 — Proteger el canal anónimo y autenticado

Verificar que:

1. La ausencia de token no bloquea el canal anónimo.
2. Un JWT válido permite crear el reporte autenticado.
3. Un JWT inválido o vencido devuelve `401`.
4. Un token inválido nunca se interpreta silenciosamente como una petición anónima.
5. El endpoint aplica HTTPS en los entornos donde corresponda.
6. Las peticiones repetitivas al canal público pueden recibir `429` por límite de tasa.
7. La respuesta incluye `Cache-Control: no-store`.

### OBJ-06 — Proteger la privacidad del reporte

Verificar que:

1. El cuerpo del reporte no aparece en los logs de aplicación.
2. No se persisten IP, user agent ni cabeceras que permitan reidentificar el canal anónimo,
   salvo una decisión legal explícita que modifique el contrato.
3. Los campos de texto se almacenan como texto plano y no se interpretan como HTML.
4. La respuesta no devuelve información adicional de la persona reportada.

### OBJ-07 — Exponer correctamente los maestros

Verificar que cada endpoint de maestro:

1. Devuelve un arreglo JSON plano.
2. Incluye `id`, `codigo` y `nombre` cuando corresponda.
3. Devuelve solamente opciones activas para `rutas-activacion`.
4. Mantiene los nombres en *sentence case*.
5. Conserva el orden esperado por el formulario.
6. Responde con un error explícito ante una falla; no convierte la falla en un arreglo vacío
   que deje el formulario inutilizable.

### OBJ-08 — Mantener un contrato de respuesta estable

Verificar que una creación exitosa:

- Usa `201`, no `200`.
- Incluye `codigo` en todas las respuestas exitosas.
- Incluye una fecha serializada con zona horaria válida.
- Incluye el estado de `rutaActivacion` como `SOLICITADA` si se adopta D1-A.
- No devuelve entidades JPA directamente.

Verificar que los errores usan:

- `400` para validación.
- `401` para token presente inválido o vencido.
- `429` para límite de tasa.
- `500`/`503` para fallas del servidor, sin respuesta de éxito simulada.

### OBJ-09 — Cubrir la integración que consumirá el frontend

Verificar, mediante una prueba de integración, el flujo completo:

1. Sembrar los maestros activos y los datos de autenticación necesarios.
2. Enviar el JSON con los nombres exactos del contrato.
3. Crear el reporte.
4. Leerlo nuevamente desde la base de datos.
5. Comparar campos, código, relación de ruta y asociación de identidad.
6. Limpiar los datos creados sin depender de datos preexistentes.

## Casos prioritarios

| Prioridad | Caso | Resultado esperado |
|---|---|---|
| P0 | Reporte anónimo válido | `201`, CAS persistido y ruta `SOLICITADA` |
| P0 | Reporte autenticado válido | `201`, CAS persistido y asociación correcta |
| P0 | Token inválido | `401`, nunca anónimo |
| P0 | Errores múltiples | `400` con todas las claves en `validationErrors` |
| P0 | Código CAS simultáneo | códigos distintos y persistidos |
| P0 | Lugar interno sin campus | `400` en `campusId` |
| P0 | Lugar externo sin detalle | `400` en `lugarDetalleExterno` |
| P1 | TI de persona adulta | `400` en `tipoDocumentoId` |
| P1 | Documento fuera de límite | `400` en `numeroDocumento` |
| P1 | Maestro de rutas | solo rutas activas, forma y nombres correctos |
| P1 | Descripción de 4.001 caracteres | `400` en `descripcionHechos` |
| P1 | Respuesta no cacheable | `Cache-Control: no-store` |
| P2 | Límite de tasa | `429` después del umbral configurado |

## Estrategia de ejecución

- **Unitarias:** validadores, generador de código CAS y mapeos DTO-entidad.
- **Integración:** `@SpringBootTest`/`MockMvc` con PostgreSQL real mediante Testcontainers,
  siguiendo la infraestructura existente en `src/test/java/.../integration`.
- **Concurrencia:** dos o más peticiones válidas en paralelo y lectura de los códigos
  persistidos.
- **Contrato HTTP:** aserciones de status, headers, JSON y claves de error.
- **Prueba de frontend posterior:** el repositorio Angular deberá comprobar el mapeo del
  formulario, el envío deshabilitado, la presentación del código recibido y el tratamiento
  accesible de `validationErrors`.

## Criterio de finalización

Los objetivos P0 deben estar cubiertos y verdes antes de integrar la funcionalidad. No se
acepta como PASS:

- una respuesta `201` cuyo reporte no exista en la base de datos;
- un código generado solamente en el cliente;
- un token inválido tratado como anónimo;
- un `400` que no permita asociar los errores a los controles;
- un maestro fallido convertido silenciosamente en una lista vacía;
- una prueba que dependa de registros preexistentes o de una base de datos local manual.

Las decisiones D1–D7 del contrato deben quedar confirmadas antes de convertir los casos
dependientes en pruebas definitivas.
