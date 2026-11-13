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

Para habilitar el rol `PROFESIONAL_APH` en una base existente, ejecutar
`database/update_add_rol_profesional_aph.sql` después de crear las tablas de seguridad.
El script es idempotente y solo concede acceso a `GET /maestros/roles`; los permisos
de las nuevas rutas APH se asignarán de forma específica al implementarlas.

Para pruebas exclusivamente locales, `database/insert_usuario_prueba_profesional_aph.sql`
crea una cuenta sintética con el rol `PROFESIONAL_APH`, copiando el hash de la cuenta
de prueba `revisor1@udea.edu.co`. Verificar que esa cuenta sea de prueba y que se
conozca su contraseña antes de ejecutarlo; el nuevo usuario tendrá la misma. El script
no cambia cuentas existentes y falla si la cuenta sintética ya existe. No usarlo en QA
ni producción, y no reutilizar la contraseña de prueba fuera del entorno local.

El nuevo módulo expone `GET /alma/catalogos/{nombre}` a cualquier usuario
autenticado, sin modificar los permisos del módulo anterior. Después de iniciar
esta versión para crear `catalogoalmaitem`, ejecutar
`database/insert_catalogos_aph_provisionales.sql`: los valores propuestos quedan
inactivos hasta su aprobación y el script no sobrescribe valores existentes.
`tipos-identificacion` reutiliza el catálogo vigente; `textos-consentimiento`
estará vacío hasta cargar el texto oficial. Antes de integrar con Front se debe
homologar el código de pasaporte y comunicar el código `PROFESIONAL_APH`.

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
