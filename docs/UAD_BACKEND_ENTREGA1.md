# Módulo UAD Equipos 3 y 4: backend, Entrega 1

**Rama:** `feature/06/20/casilda/modulo-uad-back`.

**Contrato:** `docs/contratos/uad/openapi-uad.yaml` del frontend (v0.3.0). Esta entrega implementa las operaciones con `x-entrega: 1` y la radicación pública de la NDA. Con esto, el frontend puede pasar `uad.adaptador` a `'http'` para las bandejas, la evaluación y la queja anónima.

## Qué se implementó

| Operación (operationId) | Ruta | Permiso |
|---|---|---|
| `obtenerContextoUad` | `GET /uad/contexto` | UAD_EQUIPO_3, UAD_EQUIPO_4, UAD_COORDINACION, ADMIN |
| `listarNoticias` | `GET /uad/noticias?tipo&estado&fechaRecepcionDesde&fechaRecepcionHasta&consecutivo&page&size` | Equipos 3 y 4 |
| `obtenerNoticia` | `GET /uad/noticias/{id}` | Equipos 3 y 4 |
| `clasificarVinculacion` | `POST /uad/noticias/{id}/clasificacion` | Equipos 3 y 4 |
| `consultarCasosAsociados` | `GET /uad/noticias/{id}/casos-asociados` | Equipos 3 y 4 |
| `registrarDecisionEvaluacion` | `POST /uad/noticias/{id}/decisiones` | Equipos 3 y 4 |
| `historialNoticia` | `GET /uad/noticias/{id}/historial` | Equipos 3 y 4 |
| `descargarEvidencia` | `GET /uad/evidencias/{id}/descarga` | Equipos 3 y 4 |
| Catálogos | `GET /maestros/uad-causales-inhibitorio`, `uad-entidades-remision`, `uad-tipos-solicitud-informacion` | Equipos 3 y 4. Además, los roles que ya tenían `/maestros/**` (ADMIN, COORDINADOR, PROFESIONAL, REVISOR, USUARIO). Son listas no sensibles. |
| `radicarNoticiaAnonima` | `POST /uad/noticias-anonimas` | **Pública** |

**Creación de la NDI:** al guardar un reporte, `ReporteCasoService` publica `ReporteCasoRegistradoEvent`. Después del commit, `UadRadicacionService` crea la NDI si la ruta es `UAD_3_4` o si la persona autorizó `remitirUad`. Si la creación de la NDI falla, el reporte ya quedó guardado y el error se registra en el log.

## Diseño

- **Paquetes:** subpaquetes `uad` en cada capa: `controller`, `service`, `repository`, `model/entity`, `model/enums`, `dto/request` y `dto/response`.
- **Tablas** (las crea Hibernate):
  - `uad_noticia` y `uad_caso`, con `@Version`.
  - `uad_evento` (historial append-only, `@Immutable`).
  - `uad_solicitud_informacion`.
  - `uad_consecutivo` (contador por tipo y año, con bloqueo pesimista).
  - `uad_catalogo`.
- **Auditoría:** todas las entidades UAD extienden `Auditable` (`created_at`, `created_by`, `modified_at`, `modified_by`).
- **Privacidad de la NDA:** `Auditable` guarda el email de la sesión, y una NDA no debe asociar identidad aunque llegue un token. Por eso el controlador radica la NDA dentro de `UadSinIdentidad`, que pone una sesión anónima mientras dura la transacción. La noticia, su evento y el contador de consecutivos quedan con `ANONYMOUS`. El evento de historial tampoco tiene profesional.
- **Autorización en dos capas:**
  - La tabla `endpoint` autoriza el rol.
  - `UadNoticiaService` autoriza cada registro: equipo del JWT (`ROLE_UAD_EQUIPO_3/4`), visibilidad de la noticia y estado. Ver `UadCompetencia`.
  - Una noticia de otro equipo responde **404**, igual que una inexistente.
- **Errores:** `UadExceptionHandler`, limitado a `controller.uad`, devuelve los campos de `ErrorResponse` más `codigo`: `CONFLICTO_VERSION`, `NOTICIA_YA_DECIDIDA`, `NOTICIA_NO_EVALUABLE`, `DECISION_INCOMPATIBLE`, `SIN_EQUIPO_UAD`, `VALIDACION`, `NO_ENCONTRADO`. Validación = 400 con `validationErrors` en rutas con punto. El formato global no cambia.
- **Concurrencia:** cada acción exige la `version` leída; si cambió, responde 409. Las transacciones simultáneas las resuelve `@Version`.

## Decisiones provisionales en el código

| Decisión | Dónde | Qué se hizo |
|---|---|---|
| D-UAD-03 Coordinación y ADMIN | `UadUsuarioActual`, SQL | Sin rol de equipo no hay bandejas. ADMIN y coordinación solo ven `/uad/contexto` (vacío). |
| D-UAD-05 Vinculación | `UadMapeoVinculacion` | `vinculoudea` (14) → lista UAD (10). Egresados, contratistas, jubilados y externos → `NO_TIENE_VINCULO`; "Otro" o sin dato → `NO_SE`. Trabajador oficial y directivo solo se obtienen al clasificar. |
| D-UAD-12 "No tiene vínculo" | `UadCompetencia` | Visible a ambos equipos; "Continuar trámite" exige un estamento concreto. |
| D-UAD-13 Inhibitoria | `UadDecisionValidator` | Justificación obligatoria; causales sembradas pendientes de validación. |
| D-UAD-15 ID de caso | `UadNoticiaService.nuevoCasoId` | `CAS-` + 10 caracteres, único entre gestiones UAD. Falta un ID transversal. |
| D-UAD-17 Nacimiento de la NDI | `UadRadicacionService` | Ruta `UAD_3_4` **o** `remitirUad = true`. |
| D-UAD-18 Casos asociados | `UadNoticiaService.buscarCasosAsociados` | Por documento de la persona afectada, solo en solicitud directa. Busca casos del Equipo de Atención (`solicitante.numeroDocumento`) y otras gestiones UAD. |

## Diferencias con el contrato

- **`evidenciaId`:** el contrato lo define como UUID; aquí es el id numérico de `evidenciareporte`, enviado como texto. El frontend lo trata como cadena opaca.
- **`revisionConcurrente`:** siempre `null`. Detectar quién tiene abierta una evaluación necesita presencia o bloqueo blando: D-UAD-31, nuevo.
- **Notificaciones:** no hay servicio de correo. Se devuelven en estado `PENDIENTE` con el motivo, nunca `ENVIADA`.
- **429 en la NDA:** no implementado. Limitar por origen implica procesar la IP en la aplicación, y no hay política aprobada (D-UAD-29). Puede hacerse en el proxy.
- **Descarga de evidencias:** queda registrada en el log del servidor (quién, qué evidencia, qué noticia), no en el historial visible.

## Despliegue

1. Desplegar el WAR. Hibernate (`ddl-auto: update`) crea las tablas `uad_*`.
2. Ejecutar `database/uad_modulo_entrega1.sql`. Es idempotente y hace tres cosas:
   - crea los roles UAD con `MAX(id)+1` bajo el advisory lock de la guía;
   - registra las rutas y sus permisos;
   - siembra los catálogos.
3. Asignar roles a usuarios reales con `usuariorol`, según `docs/GUIA_CREACION_ROLES.md`. **El script no asigna ninguno.**
4. En el frontend, poner `uad.adaptador: 'http'` en el entorno que corresponda.

## Pruebas

```powershell
# Unitarias UAD (sin Docker)
mvn test "-Dtest=Uad*Test" "-Dsurefire.failIfNoSpecifiedTests=false"
# Integración UAD (requiere Docker Desktop encendido)
mvn test "-Dtest=UadIntegrationTest"
```

- **Unitarias:**
  - `UadReglasTest`: competencia, mapeo y validación.
  - `UadNoticiaServiceTest`: autorización por equipo, 404, sin `casoId` al abrir, admisión, 409, NDA sin identidad.
  - `UadRadicacionServiceTest`: NDA sin sesión asociada, NDI por ruta y por `remitirUad`, idempotencia, y que un fallo de la NDI no afecte el reporte.
  - `UadControllersTest`: rutas, `no-store`, `codigo` y validación.
- **Integración** (`UadIntegrationTest`, semilla `sql/uad-integration-seed.sql`): NDA pública, NDA con sesión sin identidad, NDI desde `POST /reportes` visible solo al Equipo 3, 404 para el Equipo 4, admisión y 409, autorización por rol (Equipo de Atención, ADMIN, coordinación, sin sesión).

## Fuera de esta entrega

- **Entrega 2:** bandeja de casos, Gestionar caso, actuaciones, traslado a decisión y remisión desde el caso. `uad_caso` ya existe y la admisión lo crea.
- **Entrega 3:** "Mis reportes" y respuesta a la subsanación (bloqueados por D-UAD-16) y remisión a la Ruta.
