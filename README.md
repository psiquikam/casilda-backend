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

### Correos de prueba al repartir

El envío está desactivado por defecto y utiliza dos destinatarios de prueba,
sin incluir información de la solicitud. Una opción es [Brevo SMTP](https://help.brevo.com/hc/en-us/articles/7924908994450-Send-transactional-emails-using-Brevo-SMTP):
crea una cuenta, verifica un remitente y obtiene el login y la clave SMTP.

En la carpeta raíz del backend crea `correo-reparto-local.properties`:

```properties
app.correo-reparto.habilitado=true
spring.mail.host=smtp-relay.brevo.com
spring.mail.port=587
spring.mail.username=LOGIN_SMTP
spring.mail.password=CLAVE_SMTP
app.correo-reparto.remitente=REMITENTE_VERIFICADO
app.correo-reparto.destinatarios=PRIMER_CORREO,SEGUNDO_CORREO
```

Inicia el backend desde su carpeta raíz y reinícialo después de editar el
archivo. El archivo está excluido de Git; no compartas las credenciales. El
reparto se guarda aunque falle el correo y la pantalla muestra el resultado.
Que el servidor SMTP acepte el mensaje no garantiza su entrega final.

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

### Migración consolidada del feature APH

Para desplegar el feature APH actual en una base existente, ejecutar únicamente
`database/migration_alma_aph.sql`, después de iniciar la versión que crea las tablas
JPA del módulo. El archivo reúne las migraciones de esquema y permisos del feature.
No incluye los hotfixes HF1/HF2 ya desplegados, la carga de catálogos ni cuentas o
datos sintéticos; esos recursos permanecen separados.

Los scripts granulares `update_*_aph.sql` se conservan como fixtures para las pruebas
de integración que validan cada permiso o cambio por separado. No ejecutarlos uno a
uno como migración de despliegue ni junto con el archivo consolidado.

La migración consolidada crea el rol `TECNICO_APH` y los permisos vigentes de lectura
de maestros para los roles que ya existan. El patrón `/maestros/**` cubre los GET
actuales y futuros bajo ese prefijo, incluidos catálogos paginados; no agrega permisos
de escritura ni de otras rutas.

Para pruebas exclusivamente locales, `database/insert_usuario_prueba_tecnico_aph.sql`
crea una cuenta sintética con el rol `TECNICO_APH`, copiando el hash de la cuenta
de prueba `revisor1@udea.edu.co`. Verificar que esa cuenta sea de prueba y que se
conozca su contraseña antes de ejecutarlo; el nuevo usuario tendrá la misma. El script
no cambia cuentas existentes y falla si la cuenta sintética ya existe. No usarlo en QA
ni producción, y no reutilizar la contraseña de prueba fuera del entorno local.

Para probar la auditoría con el rol dedicado, la migración consolidada crea
`ALMA_COORDINACION`; después puede ejecutarse
`database/insert_usuario_prueba_coordinacion_alma.sql`. La segunda crea
`coordinacion.alma@casilda.test` y copia el hash de `revisor1@udea.edu.co`, por lo que
usa la misma contraseña de prueba. El alta falla si la fuente no está activa o si la
cuenta de destino ya existe; no modifica usuarios existentes. Ejecutar este script de
cuenta solo en bases locales de prueba; la cuenta sintética no se incluye en la
migración consolidada.

El nuevo módulo expone `GET /alma/catalogos/{nombre}` a cualquier usuario
autenticado, sin modificar los permisos del módulo anterior. Después de iniciar
esta versión para crear `catalogoalmaitem`, ejecutar
`database/insert_catalogos_aph.sql`: carga y activa los catálogos de APH, actualizando
etiqueta y estado activo si el código ya existe.
`tipos-identificacion` reutiliza el catálogo vigente; `textos-consentimiento`
estará vacío hasta cargar texto. Se permite un texto de prueba, claramente marcado
como no oficial, solo en QA; producción requiere el texto aprobado. Los cambios futuros de códigos o etiquetas
deben aplicarse también mediante una modificación versionada de la base de datos.
El catálogo de tipos de identificación conserva los códigos maestros existentes.

Reglas confirmadas para los siguientes endpoints de APH:

- Un seguimiento psicológico está activo cuando la persona tiene un caso en curso con Psicología y una fecha para el próximo control.
- «Mis atenciones» lista las atenciones o casos asignados o tomados por el usuario autenticado.
- La auditoría de Línea Alma solo estará disponible para `ADMIN` y `ALMA_COORDINACION`.
  Por decisión explícita, ambos pueden consultar los contextos APH y `CASO`.
- La matriz técnica de roles remitentes se documenta más abajo; no constituye por sí misma
  aprobación de la política institucional de gobernanza de datos. Se conserva para esta
  entrega la matriz que ya aplica el backend.

El primer bloque de atenciones nuevas permite crear, consultar y editar borradores mediante
`POST /alma/atenciones-aph`, `GET /alma/atenciones-aph/{idAtencion}` y
`PATCH /alma/atenciones-aph/{idAtencion}`. Requiere iniciar esta versión para crear sus
tablas y aplicar `database/migration_alma_aph.sql`. El consecutivo es anual y usa el año UTC; las escrituras exigen la versión
vigente. Cada técnico puede consultar y editar únicamente sus atenciones; `REVISOR` tiene
acceso de lectura conforme al contrato. La creación y edición registran eventos sin el
contenido de la atención.

El triaje manual o su omisión justificada se registra con
`PUT /alma/atenciones-aph/{idAtencion}/triaje`. Es exclusivo del técnico asignado;
requiere motivo de consulta y versión vigente. Cambia la atención a `EN_GESTION_APH`.
Las columnas y permisos de este flujo quedan incluidos en
`database/migration_alma_aph.sql`.

La derivación a Psicología y la consulta de su bandeja usan
`POST /alma/atenciones-aph/{idAtencion}/derivaciones/psicologia` y
`GET /alma/bandeja/psicologia`. La derivación exige triaje y consentimiento con una versión
activa del catálogo `textos-consentimiento`; no se incluye texto oficial en este cambio.
En producción requiere el texto oficial; en QA se admite texto de prueba rotulado como
no oficial. El SLA se calcula desde la recepción en el servidor.
Para recontacto se usa la persona resuelta por el módulo vigente y los estados «Abierto activo»
y «Abierto en transición». El primer bloque de bandeja es de consulta: toma, intentos,
resolución de recontactos y primer contacto aún no están implementados.

Los permisos de estas rutas quedan incluidos en
`database/migration_alma_aph.sql`.
Se mantienen los permisos actuales durante esta entrega; podrán ajustarse después mediante
la base de datos. El permiso de `PROFESIONAL` es amplio y se conserva como condición vigente,
no como aprobación definitiva de gobernanza. El texto ficticio de consentimiento solo se permite
en QA, rotulado como no oficial; no usarlo en producción.

Las remisiones entrantes usan `POST /alma/remisiones-aph`; la bandeja y la toma usan
`GET /alma/bandeja/aph` y `POST /alma/bandeja/aph/{idAtencion}/tomar`. Una remisión
nace sin técnico asignado; la toma se bloquea por fila y solo un técnico puede ganarla.
«Mis atenciones» incluye únicamente las tomadas por el usuario autenticado. Se permiten
remisiones únicas mientras exista una del mismo origen y persona en `PENDIENTE_APH`
o `EN_GESTION_APH`; se compara tipo y número de documento sin distinguir mayúsculas;
el intento repetido responde `409 REMISION_DUPLICADA` con el `idAtencion` existente.
El teléfono de la persona es obligatorio y debe cumplir `^\+?\d{7,15}$`.
El consentimiento es obligatorio salvo peligro inminente declarado; se conserva la
evidencia de consentimiento o de la excepción. Para QA puede usarse texto de prueba
claramente rotulado como no oficial; en producción debe usarse el texto aprobado.
La bandeja limita a 60 consultas por minuto por usuario, sin distinguir la red de origen;
se puede configurar con `app.alma.bandeja.limite` y
`app.alma.bandeja.ventana-minutos`. Al superar el límite responde `429 LIMITE_CONSULTAS`.

Las columnas y permisos de remisiones quedan incluidos en
`database/migration_alma_aph.sql`. El script de despliegue no crea usuarios de prueba.
EA admite `EA_COORDINACION`, `PROFESIONAL`, `COORDINADOR` y `ADMIN`; SPyB admite
`SPB_ANALISTA` y `ADMIN`; UAD admite `UAD_EQUIPO_3`, `UAD_EQUIPO_4`,
`UAD_COORDINACION` y `ADMIN`. Esta es la matriz técnica vigente; la política de
gobernanza y su ratificación institucional siguen en discusión. Central de Monitoreo
queda pendiente de un código de rol y una regla de autorización para emergencias.

La creación y toma registran eventos; la auditoría JPA registra escrituras, no cada
lectura. No hay notificador en esta entrega. Una solución futura deberá considerar
creación (`PENDIENTE_APH`) y toma (`EN_GESTION_APH`), sin datos personales ni el motivo.
El límite de tasa sigue pendiente.

Las rutas externas de atenciones APH se activan y consultan en `/alma/rutas-externas`
y se actualizan con `PATCH /alma/rutas-externas/{idRuta}`. Solo pueden gestionarlas
el técnico propietario y `ADMIN`; los demás roles no reciben rutas en el detalle.
Las rutas sobre casos y el acceso de Psicología quedan pendientes. La excepción sin
consentimiento por peligro inminente aplica solo a `LINEA_123`. Por decisión de
negocio, la versión del texto es opcional en estas rutas; si se envía, debe estar
activa. Sin versión no se puede acreditar qué texto se aceptó.

Las columnas y permisos de rutas externas quedan incluidos en
`database/migration_alma_aph.sql`.

Las derivaciones internas de atenciones APH se registran y consultan en
`/alma/derivaciones/internas` por el técnico propietario o `ADMIN`. Son solo registros:
no se notifican ni entregan automáticamente a la dependencia. Se devuelve
`estado: REGISTRADA`, `fechaRegistro` y `fechaEnvio: null`; coordinar con el frontend
antes de habilitar el flujo, pues el contrato original esperaba `ENVIADA`.
Los demás roles no reciben derivaciones en el detalle. Los casos y el acceso de
Psicología quedan pendientes. Se requiere consentimiento salvo peligro inminente
hacia `SPYB`; la versión del texto es opcional, pero si se envía debe estar activa.
Sin versión no se puede acreditar qué texto se aceptó.
Los permisos de derivaciones internas quedan incluidos en
`database/migration_alma_aph.sql`.

El cierre APH (`POST /alma/atenciones-aph/{idAtencion}/cierre`) corresponde al técnico
propietario o `ADMIN`, solo en `EN_GESTION_APH` y con versión vigente. No se cierran
borradores, atenciones con Psicología pendiente o aceptada, ni con rutas externas
abiertas. El motivo debe estar activo en `motivos-cierre-aph`; la observación admite
hasta 1000 caracteres. El permiso queda incluido en
`database/migration_alma_aph.sql`.

En el módulo de casos vigente, la transición a `Cerrado` registra `fechacierre`;
reabrir el caso la limpia. Los casos históricos conservan fecha desconocida hasta
corregirla; la columna queda incluida en `database/migration_alma_aph.sql`.
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
quedan pendientes hasta definir esos flujos. El esquema y permisos quedan incluidos en
`database/migration_alma_aph.sql`; configurar
`app.alma.consulta-previa.habilitada=true`. Pueden acceder `ADMIN`, `TECNICO_APH`
y `REVISOR` a la consulta previa, sujetos también a los permisos dinámicos. Ningún
endpoint devuelve notas clínicas en la bitácora.

`POST /alma/contenciones-post-cierre` registra una nota en una atención APH nueva
para un caso cerrado de la misma persona, sin reabrirlo ni modificar el estado de
la atención. Las notas aparecen en el detalle de la atención y la auditoría registra
el evento sin incluir su contenido. Hibernate crea la tabla `contencionpostcierre`;
el permiso queda incluido en `database/migration_alma_aph.sql`.
Pueden registrar contenciones el técnico responsable y usuarios `PROFESIONAL` cuya
persona tenga un registro individual en `profesional` con cargo `Psicóloga`. No se
infiere la profesión por la pertenencia a una dupla o grupo profesional.
Para preparar identidades sintéticas de Psicología y Jurídico, con membresía tanto
en grupos especializados como en una dupla, ejecutar
`database/insert_usuarios_prueba_profesionales_aph.sql` únicamente en local o QA
autorizado, después de cumplir sus prerrequisitos. El script copia el hash de una
cuenta de prueba existente, no cambia credenciales de cuentas previas y valida el
cargo y tipo de servicio de los grupos antes de crear las relaciones.
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
confiable para calcularla; el frontend debe tolerar ese valor. No habilitar el flujo completo
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

### Catálogos de Equipo de Atención

Para desplegar los cambios de Equipo de Atención, coordinar con el equipo de BD
la ejecución de estos scripts sobre la base del entorno, después de que exista
la tabla `rutaequipoatencion` creada por Hibernate:

1. `database/insert_catalogos_equipo_atencion.sql`
2. `database/insert_discapacidades_equipo_atencion.sql`
3. `database/insert_lugares_entrevista_equipo_atencion.sql`
4. `database/insert_relaciones_misionales_equipo_atencion.sql`
5. `database/insert_modalidades_matriz_equipo_atencion.sql`

Los scripts agregan opciones y conservan los identificadores existentes; se
pueden volver a ejecutar. El primero clasifica rutas internas y externas y añade
la columna para describir «Otra». El cuarto añade las columnas del ámbito de
ocurrencia. Si el entorno no permite que Hibernate actualice el esquema, el
equipo de BD debe crear primero `rutaequipoatencion` según su entidad y aplicar
los scripts como migración autorizada.

`GET /maestros/rutas-internas` y `/maestros/rutas-externas` consultan las rutas
clasificadas; para los servicios se reutiliza `GET /maestros/tipos-servicio`.

El detalle `GET /solicitudes/acompanamiento/{id}` incluye las discapacidades
del solicitante para el equipo de atención. Al guardar la pestaña de persona,
omitir `discapacidades` conserva las existentes; enviar una lista las reemplaza.
Las opciones de discapacidad y lugares de entrevista se incorporan mediante
los scripts anteriores sin eliminar los valores históricos.

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
