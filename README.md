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

- `GET /maestros/sexos`
- `GET /maestros/tipos-identificacion`
- `GET /maestros/etnias`
- `GET /maestros/campus`
- `GET /maestros/facultades`
- `GET /maestros/dependencias`
- `GET /maestros/modalidades-violencia`
- `GET /maestros/programas`

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
