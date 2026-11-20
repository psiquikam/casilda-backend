# Módulo UAD Equipos 3 y 4: backend, Entrega 2

**Rama:** `feature/06/20/casilda/modulo-uad-back`.

**Contrato:** `docs/contratos/uad/openapi-uad.yaml` del frontend (v0.3.0), operaciones con `x-entrega: 2`. Con esto, la bandeja de casos y «Gestionar caso» del frontend pueden usar `uad.adaptador: 'http'`. La base es la Entrega 1 (`docs/UAD_BACKEND_ENTREGA1.md`).

## Qué se implementó

| Operación (operationId) | Ruta | Permiso |
|---|---|---|
| `listarCasosUad` | `GET /uad/casos?fase&page&size` | Equipos 3 y 4 |
| `obtenerCasoUad` | `GET /uad/casos/{casoUadId}` | Equipos 3 y 4 |
| `registrarExpedienteUad` | `PATCH /uad/casos/{casoUadId}/expediente` | Equipos 3 y 4 |
| `catalogoActuaciones` | `GET /uad/casos/{casoUadId}/actuaciones/catalogo` | Equipos 3 y 4 |
| `registrarActuaciones` | `POST /uad/casos/{casoUadId}/actuaciones` | Equipos 3 y 4 |
| `historialCaso` | `GET /uad/casos/{casoUadId}/historial` | Equipos 3 y 4 |
| `finalizarInstruccion` | `POST /uad/casos/{casoUadId}/traslado-decision` | Equipos 3 y 4 |
| `remitirCasoPorCompetencia` | `POST /uad/casos/{casoUadId}/remisiones-competencia` | Equipos 3 y 4 |
| `evidenciasCaso` | `GET /uad/casos/{casoUadId}/evidencias` | Equipos 3 y 4 |

Coordinación UAD y ADMIN no tienen estas rutas (D-UAD-03). El contexto (`GET /uad/contexto`) ahora incluye la capacidad `GESTIONAR_CASO` para los equipos.

## Reglas

- **Visibilidad:** un caso es del equipo responsable en la fase actual. En instrucción lo ve el instructor (personal → Equipo 3, estudiantes → Equipo 4). En decisión lo ve el decisor (personal → Equipo 4, estudiantes → Equipo 3). Si el caso no existe o no es visible, responde 404 (`UadCasoService.casoVisible`).
- **Bandeja:** solo casos con gestión `ACTIVA`, ordenados por fecha de admisión descendente. Un caso remitido sale de la bandeja, pero quien lo tiene asignado puede seguir abriéndolo.
- **Catálogo de actuaciones** (`UadCatalogoActuaciones`): es el mismo de `dominio/catalogo-actuaciones.ts` del frontend (bitácora §7 y §8).
  - Las ordinarias dependen de la fase y el equipo.
  - Las permanentes se muestran siempre.
  - Mediación, inhibitorio, terminación y remisión a la Ruta salen deshabilitadas, con su motivo, por estar pendientes de validación.
- **Actuaciones ordinarias:** se pueden registrar varias a la vez, pero solo de la fase y el equipo actuales y nunca junto con una permanente (D-UAD-20). Si no cumplen, responde 400 `ACTUACION_NO_DISPONIBLE`. No cambian la fase, pero sí suben la `version` del caso (`PESSIMISTIC_FORCE_INCREMENT`), para que otra persona con el caso abierto recargue antes de actuar.
- **Traslado:**
  - **Requisitos:** fase `INSTRUCCION`, gestión activa y `confirmaTraslado = true`.
  - **Qué cambia:** el caso pasa a `DECISION` con el equipo decisor y conserva `casoId`, expediente, admisión, evidencias e historial.
  - **Resultado:** la respuesta trae `visibleParaUsuario: false`, porque el instructor deja de verlo.
  - **Requisitos jurídicos adicionales:** no se inventan (D-UAD-21).
- **Remisión por competencia:** valida que la entidad esté en el catálogo y que la fecha no sea futura. La gestión UAD queda `FINALIZADA` con motivo `REMITIDA_POR_COMPETENCIA`. El caso CASILDA no se cierra y no se envía nada a la entidad.
- **Expediente:** puede quedar vacío. Si llega el mismo valor que ya está registrado, responde 400. El historial guarda el valor anterior y el nuevo, y las noticias del caso se actualizan al mismo valor.
- **Errores:** `CONFLICTO_VERSION`, `CASO_NO_ACTIVO` y `FASE_NO_PERMITE` responden 409. `ACTUACION_NO_DISPONIBLE` y `VALIDACION` responden 400.
- **Historial:** cada acción crea un `UadEvento` con profesional, equipo, fase y observaciones. El traslado queda firmado por el equipo instructor y la fase de instrucción.
- **Evidencias:** son las de las noticias admitidas en el caso. `GET /uad/evidencias/{id}/descarga` ahora autoriza también al equipo que tiene el caso. Así el decisor puede descargar las pruebas aunque, después del traslado, ya no vea la noticia de origen.

## Diferencias con el contrato

- **Notificación del traslado:** queda `PENDIENTE` si hay víctima identificada y `NO_APLICA` si no la hay. No hay servicio de correo (D-UAD-10).
- **`evidenciaId`:** igual que en la Entrega 1, es el id numérico enviado como texto.

## Despliegue

1. Desplegar el WAR. No hay tablas nuevas; las rutas usan las de la Entrega 1.
2. Ejecutar `database/uad_modulo_entrega2.sql`. Es idempotente y requiere haber ejecutado antes `uad_modulo_entrega1.sql`.
3. En el frontend, `uad.adaptador: 'http'` cubre noticias y casos.

## Pruebas

```powershell
mvn test "-Dtest=Uad*Test" "-Dsurefire.failIfNoSpecifiedTests=false"
```

- `UadCatalogoActuacionesTest`: ordinarias por fase y equipo, permanentes, pendientes de validación y validación del registro.
- `UadCasoServiceTest`: 404 para el otro equipo, versión y 409, actuaciones no disponibles, traslado (visibilidad, evento, notificación), remisión y expediente.
- `UadIntegrationTest.gestionDelCasoDeInstruccionADecisionYRemision`: el flujo completo con la base real. El Equipo 3 admite y actúa, registra el expediente y traslada. El Equipo 4 ve el caso con su historial y lo remite. ADMIN, coordinación y peticiones sin sesión reciben 403 en `/uad/casos`.

## Fuera de esta entrega

- Acceso histórico a casos ya trasladados o finalizados (D-UAD-03).
- Inhibitorio, terminación y mediación como acciones ejecutables (validación jurídica).
- Carga de evidencias (D-UAD-09).
- Remisión a la Ruta, «Mis reportes» y subsanación (Entrega 3, D-UAD-16).
