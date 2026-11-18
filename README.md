# CASILDA Backend

Backend del sistema CASILDA desarrollado con Spring Boot, Spring Security (JWT) y PostgreSQL.

## URLs principales

- Base API: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/api/v1/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api/v1/api-docs`

## Requisitos

- Java 21
- Maven 3.9+
- PostgreSQL 12+

## Configuración

Archivo: `src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/casilda
    username: postgres
    password: postgres

server:
  port: 8080
  servlet:
    context-path: /api/v1
```

## Compilar y ejecutar

```bash
mvn clean compile
mvn spring-boot:run
```

Si `mvn` no está en `PATH`, usa la ruta instalada localmente (ejemplo en Windows):

```powershell
& "C:\Users\William Ortiz\.maven\maven-3.9.12(1)\bin\mvn.cmd" spring-boot:run
```

## Autenticación JWT

### Login

`POST /auth/login`

Ejemplo:

```bash
curl -X POST "http://localhost:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@udea.edu.co",
    "password": "Casilda2024!"
  }'
```

Con el token devuelto, autoriza en Swagger con:

`Bearer <token>`

## Endpoints principales

### Maestros

- `GET /maestros/tipos-servicio` (tipos de servicio activos)
- `GET /solicitudes/grupos-profesionales` (grupos activos; cada elemento incluye `idTipoServicio` para filtrar)
- `GET /maestros/sexos`
- `GET /maestros/tipos-identificacion`
- `GET /maestros/etnias`
- `GET /maestros/campus`
- `GET /maestros/facultades`
- `GET /maestros/dependencias`
- `GET /maestros/modalidades-violencia`
- `GET /maestros/programas`

El registro de la pestaña 0 de Línea ALMA (`POST /linea-alma/registros/pestana/0`)
requiere `idTipoServicio` y guarda el servicio seleccionado; ya no se asigna
automáticamente Atención APH.

La nota de la pestaña APH (`POST /linea-alma/registros/pestana/3`) admite hasta
2000 caracteres. Antes de desplegar esta versión en una base existente, respaldarla
y ejecutar `database/update_schema_atencion_aph_nota.sql`. El cambio falla sin
truncar datos si hay notas que superan el límite; revisarlas antes de reintentarlo.
Los valores que superan el límite reciben `400` con
`validationErrors["atencionAph.notaAph"]`.

Para habilitar el selector de instancias de remisión, ejecutar
`database/update_schema_instancia_remision.sql` en la base correspondiente después
de respaldarla. Carga una lista inicial limitada (una instancia interna y cuatro
externas), pendiente de confirmación como catálogo oficial. El script valida los
tipos 1: Interna y 2: Externa y falla si los IDs de instancia ya tienen datos
diferentes; no modifica ni elimina otras instancias.

Para habilitar el rol `TECNICO_APH` en una base existente, ejecutar
`database/update_add_rol_tecnico_aph.sql` después de crear las tablas de seguridad.
El script es idempotente y solo concede acceso a `GET /maestros/roles`; los permisos
de las nuevas rutas APH se asignarán de forma específica al implementarlas.

Para ampliar la lectura de maestros en bases existentes, ejecutar
`database/update_permisos_get_maestros_equipo.sql`. Concede `GET /maestros/**` a los
roles de atención existentes (`ADMIN`, `COORDINADOR`, `PROFESIONAL`, `REVISOR`,
`TECNICO_APH`), las coordinaciones `EA_COORDINACION`, `ALMA_COORDINACION`,
`UAD_COORDINACION`, `SPB_COORDINACION` y los remitentes `SPB_ANALISTA`,
`UAD_EQUIPO_3`, `UAD_EQUIPO_4` que ya existan. El script es idempotente y no crea
roles; debe ejecutarse de nuevo después de crear cualquiera de esos roles. Central de
Monitoreo no se incluye porque todavía no hay un código de rol confirmado.
El patrón incluye todos los GET actuales y futuros bajo `/maestros/`, incluso
catálogos paginados; no agrega permisos de escritura ni de otras rutas. Los GET de
maestros ya configurados como públicos seguirán siendo públicos.

Para pruebas exclusivamente locales, `database/insert_usuario_prueba_tecnico_aph.sql`
crea una cuenta sintética con el rol `TECNICO_APH`, copiando el hash de la cuenta
de prueba `revisor1@udea.edu.co`. Verificar que esa cuenta sea de prueba y que se
conozca su contraseña antes de ejecutarlo; el nuevo usuario tendrá la misma. El script
no cambia cuentas existentes y falla si la cuenta sintética ya existe. No usarlo en QA
ni producción, y no reutilizar la contraseña de prueba fuera del entorno local.

Para probar la auditoría con el rol dedicado, ejecutar primero
`database/update_add_rol_coordinacion_alma.sql` y luego
`database/insert_usuario_prueba_coordinacion_alma.sql`. La segunda crea
`coordinacion.alma@casilda.test` y copia el hash de `revisor1@udea.edu.co`, por lo que
usa la misma contraseña de prueba. El alta falla si la fuente no está activa o si la
cuenta de destino ya existe; no modifica usuarios existentes. Limitar ambos scripts
a bases locales de prueba.

El nuevo módulo expone `GET /alma/catalogos/{nombre}` a cualquier usuario
autenticado, sin modificar los permisos del módulo anterior. Después de iniciar
esta versión para crear `catalogoalmaitem`, ejecutar
`database/insert_catalogos_aph.sql`: carga y activa los catálogos de APH, actualizando
etiqueta y estado activo si el código ya existe.
`tipos-identificacion` reutiliza el catálogo vigente; `textos-consentimiento`
estará vacío hasta cargar el texto oficial. Los cambios futuros de códigos o etiquetas
deben aplicarse también mediante una modificación versionada de la base de datos.
El catálogo de tipos de identificación conserva los códigos maestros existentes.

Reglas confirmadas para los siguientes endpoints de APH:

- Un seguimiento psicológico está activo cuando la persona tiene un caso en curso con Psicología y una fecha para el próximo control.
- «Mis atenciones» lista las atenciones o casos asignados o tomados por el usuario autenticado.
- La auditoría de Línea Alma solo estará disponible para `ADMIN` y `ALMA_COORDINACION`.
  Por decisión explícita, ambos pueden consultar los contextos APH y `CASO`.
- Las remisiones entrantes podrán originarse en `SPB_ANALISTA` (y traslados puntuales de emergencia
  desde Central de Monitoreo), `UAD_EQUIPO_3`, `UAD_EQUIPO_4` y la coordinación UAD. Estos roles
  son adicionales; no reemplazan ni modifican roles existentes.

El primer bloque de atenciones nuevas permite crear, consultar y editar borradores mediante
`POST /alma/atenciones-aph`, `GET /alma/atenciones-aph/{idAtencion}` y
`PATCH /alma/atenciones-aph/{idAtencion}`. Requiere iniciar esta versión para crear sus
tablas y ejecutar `database/update_permisos_borrador_alma_aph.sql` después de crear el rol
`TECNICO_APH`. El consecutivo es anual y usa el año UTC; las escrituras exigen la versión
vigente. Cada técnico puede consultar y editar únicamente sus atenciones; `REVISOR` tiene
acceso de lectura conforme al contrato. La creación y edición registran eventos sin el
contenido de la atención.

El triaje manual o su omisión justificada se registra con
`PUT /alma/atenciones-aph/{idAtencion}/triaje`. Es exclusivo del técnico asignado;
requiere motivo de consulta y versión vigente. Cambia la atención a `EN_GESTION_APH`.
Tras reiniciar esta versión para añadir las columnas, ejecutar
`database/update_permisos_triaje_alma_aph.sql` en la base correspondiente.

La derivación a Psicología y la consulta de su bandeja usan
`POST /alma/atenciones-aph/{idAtencion}/derivaciones/psicologia` y
`GET /alma/bandeja/psicologia`. La derivación exige triaje y consentimiento con una versión
activa del catálogo `textos-consentimiento`; no se incluye texto oficial en este cambio.
Sin ese texto no se puede derivar. El SLA se calcula desde la recepción en el servidor.
Para recontacto se usa la persona resuelta por el módulo vigente y los estados «Abierto activo»
y «Abierto en transición». El primer bloque de bandeja es de consulta: toma, intentos,
resolución de recontactos y primer contacto aún no están implementados.

Tras iniciar esta versión para crear las columnas, ejecutar
`database/update_permisos_derivacion_psicologia.sql` solo en un entorno autorizado.
Los permisos de la bandeja son **provisionales**: `PROFESIONAL` puede incluir usuarios de otras
áreas y permite ver datos personales. Antes de usarlos fuera de pruebas se debe restringir
el acceso a los profesionales de Psicología y validar el alcance de los demás roles.
No insertar textos ficticios de consentimiento en bases compartidas o reales.

Las remisiones entrantes usan `POST /alma/remisiones-aph`; la bandeja y la toma usan
`GET /alma/bandeja/aph` y `POST /alma/bandeja/aph/{idAtencion}/tomar`. Una remisión
nace sin técnico asignado; la toma se bloquea por fila y solo un técnico puede ganarla.
«Mis atenciones» incluye únicamente las tomadas por el usuario autenticado. Se permiten
remisiones repetidas. El consentimiento con texto oficial activo es obligatorio salvo
peligro inminente; se conserva la evidencia de consentimiento o de la excepción.

Para bases existentes, iniciar esta versión para crear `datosremision` y ejecutar
`database/update_permisos_bandeja_aph.sql` antes de usar las rutas. El script permite
que el técnico asignado sea nulo en remisiones pendientes y concede permisos a los
roles existentes, sin crearlos; debe repetirse al crear nuevos roles autorizados.
EA admite `EA_COORDINACION`, `PROFESIONAL`, `COORDINADOR` y `ADMIN`; SPyB admite
`SPB_ANALISTA` y `ADMIN`; UAD admite `UAD_EQUIPO_3`, `UAD_EQUIPO_4`,
`UAD_COORDINACION` y `ADMIN`. `PROFESIONAL` y `COORDINADOR` no distinguen área:
se autorizó su uso para EA, pero deben restringirse por dependencia antes de
compartirlos con otras áreas. Central de Monitoreo queda pendiente de un código de rol
y una regla de autorización para emergencias.

La creación y toma registran eventos; la auditoría JPA registra escrituras, no cada
lectura. La limitación de tasa y la notificación de remisiones están pendientes;
no exponer la bandeja fuera de un entorno autorizado sin resolver esos controles.

Las rutas externas de atenciones APH se activan y consultan en `/alma/rutas-externas`
y se actualizan con `PATCH /alma/rutas-externas/{idRuta}`. Solo pueden gestionarlas
el técnico propietario y `ADMIN`; los demás roles no reciben rutas en el detalle.
Las rutas sobre casos y el acceso de Psicología quedan pendientes. La excepción sin
consentimiento por peligro inminente aplica solo a `LINEA_123`. Por decisión de
negocio, la versión del texto es opcional en estas rutas; si se envía, debe estar
activa. Sin versión no se puede acreditar qué texto se aceptó.

Para una base existente, iniciar esta versión para crear `rutaexternaalma` y ejecutar
`database/update_permisos_rutas_externas_aph.sql` en el entorno autorizado.

Las derivaciones internas de atenciones APH se registran y consultan en
`/alma/derivaciones/internas` por el técnico propietario o `ADMIN`. Son solo registros:
no se notifican ni entregan automáticamente a la dependencia. Se devuelve
`estado: REGISTRADA`, `fechaRegistro` y `fechaEnvio: null`; coordinar con el frontend
antes de habilitar el flujo, pues el contrato original esperaba `ENVIADA`.
Los demás roles no reciben derivaciones en el detalle. Los casos y el acceso de
Psicología quedan pendientes. Se requiere consentimiento salvo peligro inminente
hacia `SPYB`; la versión del texto es opcional, pero si se envía debe estar activa.
Sin versión no se puede acreditar qué texto se aceptó.
Para una base existente, iniciar esta versión para crear `derivacioninternaalma` y
ejecutar `database/update_permisos_derivaciones_internas_aph.sql`.

El cierre APH (`POST /alma/atenciones-aph/{idAtencion}/cierre`) corresponde al técnico
propietario o `ADMIN`, solo en `EN_GESTION_APH` y con versión vigente. No se cierran
borradores, atenciones con Psicología pendiente o aceptada, ni con rutas externas
abiertas. El motivo debe estar activo en `motivos-cierre-aph`; la observación admite
hasta 1000 caracteres. Para una base existente ejecutar
`database/update_permisos_cierre_alma_aph.sql`.

En el módulo de casos vigente, la transición a `Cerrado` registra `fechacierre`;
reabrir el caso la limpia. Los casos históricos conservan fecha desconocida hasta
corregirla: ejecutar `database/update_fecha_cierre_caso.sql` antes de usar la versión.
La corrección se realiza con
`PATCH /solicitudes/acompanamiento/{solicitudId}/casos/{casoId}/fecha-cierre` y
`{"fechaCierre":"2026-01-01T10:00:00Z"}`. Solo se permite en casos cerrados de la
solicitud indicada, a usuarios autorizados a asignar esa solicitud; la autorización
se evalúa contra la configuración vigente de `POST /solicitudes/acompanamiento/{id}/asignar`.

La consulta previa `GET /alma/personas/consulta` está **deshabilitada por defecto** y
es exclusivamente para pruebas autorizadas. Registra cada consulta válida en
`auditoriarecursos`, sin guardar el documento consultado; los campos que cambien
en futuras auditorías de escritura se limitarán a sus nombres, nunca sus valores.
El historial inmutable se consulta con
`GET /alma/auditoria?tipoContexto=ATENCION_APH&idContexto=ALMA-AAAA-NNNNNN` o
`GET /alma/auditoria?tipoContexto=CASO&idContexto={id|codigo}`. Registra creación,
actualizaciones y cambios de estado en ambos contextos; las derivaciones APH también
quedan registradas. Los eventos se devuelven en orden cronológico y las descripciones no
incluyen contenido clínico ni notas. Acceso restringido a `ADMIN` y
`ALMA_COORDINACION`; aplica límite de 30 consultas por usuario cada 10 minutos.
No existe actualmente una operación de derivación sobre `CASO` ni de vinculación
manual que permita generar los eventos `DERIVACION` y `VINCULACION` para ese contexto;
quedan pendientes hasta definir esos flujos. Para probar estos endpoints, ejecutar
`database/update_auditoria_recursos.sql`, `database/update_consulta_previa_aph.sql`,
`database/update_historial_auditoria_aph.sql` y configurar
`app.alma.consulta-previa.habilitada=true`. Pueden acceder `ADMIN`, `TECNICO_APH`
y `REVISOR` a la consulta previa, sujetos también a los permisos dinámicos. Ningún
endpoint devuelve notas clínicas en la bitácora.
El límite predeterminado es de 30 solicitudes por usuario en una ventana fija de
10 minutos (`app.alma.consulta-previa.limite` y
`app.alma.consulta-previa.ventana-minutos`); al cambiar de ventana pueden admitirse
más de 30 solicitudes en un intervalo móvil de 10 minutos. Se guarda únicamente
el contador y el inicio de la ventana por usuario, sin documento consultado.
Evitar registrar la cadena de consulta en logs del servidor y los proxies.

La consulta selecciona el seguimiento psicológico abierto futuro más cercano; si
todos están vencidos, el vencido más reciente. El responsable es el grupo de la
asignación vigente, si existe. Para casos cerrados prioriza la fecha de cierre
conocida más reciente; si todas son desconocidas, el caso creado más recientemente,
con fecha de cierre nula. En ambos casos puede no ser el último cierre real. No
envía notificaciones a Psicología ni vincula una atención nueva.

En este bloque `gestionSimultanea` se devuelve como `null` mientras no exista una fuente
confiable para calcularla; el frontend debe tolerar ese valor. La consulta de auditoría
todavía no está implementada en el nuevo módulo: no habilitar el flujo completo
ni desactivar los mocks de `atencionesAph` y `bandejaPsicologia` hasta implementar sus acciones.

Para una base existente, coordinar una ventana de mantenimiento con el frontend:

1. Desplegar esta versión e iniciar la aplicación para que Hibernate cree la columna
   nullable `grupoprofesional.idtiposervicio` y su clave foránea. No habilitar tráfico todavía:
   los grupos existentes no tendrán servicio hasta ejecutar el siguiente paso.
2. Ejecutar `database/update_catalogos_servicio_grupo.sql` y verificar que finalice
   correctamente. Actualiza los datos y establece `NOT NULL` después de completar la relación.
3. Habilitar el tráfico una vez comprobados los catálogos y actualizado el frontend.

Respaldar y revisar los datos antes de ejecutar el script. El script
reemplaza los nombres asociados a los IDs existentes: las referencias históricas
conservarán sus IDs pero mostrarán el nuevo significado, incluido el antiguo ID 5
de Atención APH. Eliminará otros IDs de estos catálogos solo si no tienen referencias;
si existen referencias, falla y revierte la transacción sin borrar registros. En ese caso,
mantener la ventana de mantenimiento y resolver las referencias antes de reintentarlo.
En una base nueva, ejecutar también el script después del primer inicio de Hibernate
para cargar los catálogos y establecer la restricción `NOT NULL`.

### Formularios

- `POST /quejas`
- `POST /acompanamientos`
- `POST /atenciones`

### Gestión

- `GET /usuarios`
- `POST /usuarios`
- `PUT /usuarios/{id}`
- `PATCH /usuarios/{id}/estado`
- `DELETE /usuarios/{id}`
- `GET /listas`

### Contenidos

Endpoints públicos:

- `GET /contenidos/home` (devuelve los contenidos vigentes del Home como arreglo JSON)
- `GET /contenidos/imagenes/**` (consulta de imágenes)

Endpoints protegidos, disponibles para los roles `ADMIN` y `GESTOR_CONTENIDO`:

- `GET /contenidos?page=0&size=10` (listado paginado)
- `GET /contenidos/{id}`
- `POST /contenidos` (recibe `multipart/form-data`, con la parte `contenido` en JSON y una parte `imagen` opcional)
- `PUT /contenidos/{id}` (recibe `multipart/form-data`; permite reemplazar o eliminar la imagen)
- `DELETE /contenidos/{id}` (eliminación lógica)

### Casos y seguimiento

- `GET /casos`
- `GET /casos/sin-asignar`
- `PUT /casos/{codigo}`
- `POST /repartos`
- `GET /asignaciones/mis-casos`
- `GET /acompanamientos/{codigoCaso}/contactos`
- `POST /acompanamientos/{codigoCaso}/contactos`
- `GET /seguimiento/{codigoCaso}`
- `GET /dashboard/revisor`

## Scripts SQL útiles

- `docs/GUIA_CREACION_ROLES.md` (tutorial DB-only para crear roles, permisos y usuarios)
- `database/crear_rol_con_permisos.sql` (procedimiento almacenado para crear un rol con permisos)
- `database/initial_data.sql` (configuración inicial de roles, endpoints y permisos)
- `database/insert_usuarios_prueba.sql` (seed de usuarios)
- `database/update_passwords_usuarios_prueba.sql` (actualización de hashes BCrypt)

## Instalador para servidor Apache

Se agregó un instalador listo para Linux con Apache HTTP Server en:

- `deploy/apache/install-backend-apache.sh`
- `deploy/apache/README_APACHE_INSTALLER.md`

Este instalador:

- Instala Java 21 + Apache (apt/dnf/yum)
- Configura el backend como servicio `systemd`
- Publica `/api/v1` detrás de Apache con `mod_proxy`

Uso rápido:

```bash
mvn clean package -DskipTests
sudo bash deploy/apache/install-backend-apache.sh
```

Opcional (dominio):

```bash
export SERVER_NAME=api.casilda.midominio.com
sudo bash deploy/apache/install-backend-apache.sh
```

## Notas importantes

- El proyecto usa `context-path=/api/v1`; no dupliques prefijos (`/api/v1/api/v1/...`).
- Para ejemplos completos de consumo, consultar `EJEMPLOS_ENDPOINTS.md`.
