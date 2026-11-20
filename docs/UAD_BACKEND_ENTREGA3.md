# Módulo UAD Equipos 3 y 4: backend, Entrega 3 (parte 1)

**Rama:** `feature/06/20/casilda/modulo-uad-back`. Las Entregas 1 y 2 ya están en `main`.

**Contrato:** `docs/contratos/uad/openapi-uad.yaml` del frontend, operaciones `listarMisReportes`, `obtenerMiReporte` y `responderSolicitudInformacion`. El frontend todavía no tiene la pantalla «Mis reportes».

Esta entrega tiene dos partes independientes: la corrección de privacidad de `/solicitudes` (D-UAD-16), que no es del módulo UAD, y «Mis reportes» con la respuesta a la subsanación.

## 1. Privacidad de `/solicitudes` (D-UAD-16)

**Problema:** el rol `USUARIO` tiene `GET /solicitudes/**` y el servicio devolvía todas las solicitudes. Cualquier persona con cuenta podía listar las solicitudes de acompañamiento de las demás.

**Corrección** (`SolicitudAcceso`, `SolicitudAcompanamientoService`):

- Quien no tiene un rol del equipo de atención (`ADMIN`, `COORDINADOR`, `PROFESIONAL`, `REVISOR`) solo lee las solicitudes que radicó (`SolicitudAtencion.usuarioCreacion`).
- Aplica a `GET /solicitudes/acompanamiento`, `/acompanamiento/paginado`, `/acompanamiento/{id}` y `/acompanamiento/{id}/contactos`.
- Una solicitud ajena responde 404, igual que una inexistente.
- El equipo de atención no cambia: sigue viendo todo.

**Efectos a revisar con el equipo de atención:**

- Una solicitud que el equipo radicó a nombre de una persona no le aparece a esa persona, porque no la creó ella.
- La persona sigue viendo los contactos telefónicos de su propia solicitud (resultado y observación). Si esas observaciones son internas, hay que quitarle esa ruta.
- No requiere SQL: los permisos de `endpoint` no cambian.

## 2. «Mis reportes» y respuesta a la subsanación

| Operación (operationId) | Ruta | Permiso |
|---|---|---|
| `listarMisReportes` | `GET /mis-reportes?page&size` | USUARIO |
| `obtenerMiReporte` | `GET /mis-reportes/{codigo}` | USUARIO |
| `responderSolicitudInformacion` | `POST /reportes/{codigo}/solicitudes-informacion/{solicitudId}/respuesta` (multipart) | USUARIO |

### Reglas

- **Dueño:** el filtro va en la consulta, por `ReporteCaso.usuario`. Un reporte ajeno responde 404 (`UadMisReportesService`).
- **Qué no aparece:** las NDA y los reportes hechos sin iniciar sesión, porque no tienen dueño.
- **Alcance:** solo reportes que llegaron a la UAD (`actor: UAD`). Los demás actores de la Ruta entran cuando definan qué estado puede ver la persona.
- **`codigo`:** el del reporte (`CAS-…`), que la persona recibió al radicar. La respuesta también trae el consecutivo NDI.
- **Estado para la persona (provisional, D-UAD-32):**

| Estado de la noticia | Texto |
|---|---|
| `RECIBIDA`, `PENDIENTE_CLASIFICACION` | Recibido |
| `EN_EVALUACION_PRELIMINAR`, `SUBSANACION_RECIBIDA`, `EN_REEVALUACION` | En revisión |
| `PENDIENTE_SUBSANACION` | Información adicional solicitada |
| `PROCEDE_TRAMITE` | En trámite |
| `INHIBITORIA`, `REMITIDA_POR_COMPETENCIA` | Revisión finalizada |

  No se entrega historial, equipo, expediente ni el tipo de decisión.
- **Responder:**
  - Texto obligatorio, máximo 4000 caracteres.
  - Guarda `respuesta` y `fecha_respuesta` en `uad_solicitud_informacion`, pasa la noticia a `SUBSANACION_RECIBIDA` y deja un evento `SUBSANACION_RECIBIDA` sin profesional ni equipo.
  - Se puede responder después de la fecha límite: el vencimiento no cierra la noticia (UAD-EVA-05).
  - La UAD ve la respuesta en `solicitudInformacion.respuesta` de `GET /uad/noticias/{id}`.
- **Evidencias:** deshabilitadas (D-UAD-09). Si llega un archivo, responde 400 `EVIDENCIAS_NO_HABILITADAS`.
- **Errores:** 409 `SOLICITUD_YA_RESPONDIDA`, 409 `SOLICITUD_NO_VIGENTE` (la UAD ya decidió), 409 `CONFLICTO_VERSION`, 400 `VALIDACION`, 404 `NO_ENCONTRADO`.

### Hallazgo sin resolver

`UadNoticiaService.acciones` permite «Solicitar información» si la noticia tiene reporte, aunque el reporte se haya hecho sin sesión. En ese caso nadie puede responder desde «Mis reportes». La noticia no se bloquea, porque la UAD puede decidir igual, pero la solicitud queda sin destinatario. Falta decidir si se deshabilita la acción cuando `ReporteCaso.usuario` es nulo.

## Despliegue

1. Desplegar el WAR. Hibernate (`ddl-auto: update`) agrega las columnas `respuesta` y `fecha_respuesta`.
2. Ejecutar `database/uad_modulo_entrega3.sql`. Es idempotente y requiere `uad_modulo_entrega1.sql`.

## Pruebas

```powershell
mvn test "-Dtest=!*IntegrationTest,!*MigrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"
```

- `SolicitudAccesoTest`: quién ve todo y quién solo lo propio; listados filtrados; 404 ante una solicitud ajena.
- `UadMisReportesServiceTest`: filtro por dueño, 404, 401, respuesta, validaciones, doble respuesta y solicitud de otra noticia.
- `UadMisReportesControllerTest`: rutas, multipart, `no-store` y `codigo` de error.

**Sin prueba de integración.** Requieren Docker y no se ejecutaron en esta entrega. Faltan dos casos en `UadIntegrationTest` y `SolicitudAuthorizationIntegrationTest`: un `USUARIO` no ve el reporte ni la solicitud de otro, y el flujo completo solicitar → responder → la UAD ve la respuesta.

## Fuera de esta entrega

- Evidencias en la respuesta (D-UAD-09).
- Reportes de otros actores de la Ruta en «Mis reportes».
- Remisión a la Ruta, acceso histórico y notificaciones.
- Registrar la descarga de evidencias en el historial visible: hoy queda en el log del servidor.
