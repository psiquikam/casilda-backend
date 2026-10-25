# Ambiente QA en AWS

## Configuración del entorno

| Componente | Descripción |
|---|---|
| **Aurora PostgreSQL** | Base de datos del backend, accesible solo desde el Security Group de la EC2 (puerto 5432, sin acceso público). |
| **EC2** (Amazon Linux 2023) | Ejecuta nginx (HTTPS) y Tomcat 10 con `api-casilda.war`; guarda las imágenes de contenidos en disco (volumen EBS). |
| **EC2 como bastión** | La misma instancia es el único punto de acceso por SSH (puerto 22) y desde ella se administra la base de datos con `psql`. |
| **S3** | En trabajo: respaldo de las imágenes de contenidos. |

Sigue el esquema de producción de
[deploy/README_DESPLIEGUE_LINUX_TOMCAT10.md](../deploy/README_DESPLIEGUE_LINUX_TOMCAT10.md).

## Pasos

1. **Seguridad**: SG de la EC2 (80 y 443 públicos; 22 solo para los desarrolladores autorizados) y SG de
   Aurora (5432 solo desde el SG de la EC2, sin acceso público).
2. **Aurora**: crear el clúster PostgreSQL, la base `casildaqa` y el rol de aplicación `casildaqa`
   (con `GRANT casildaqa TO postgres;` antes de `CREATE DATABASE ... OWNER casildaqa;`).
3. **EC2**: volumen EBS gp3 de 10 GB, rol IAM con escritura solo al bucket de respaldos.
   ```bash
   sudo dnf install -y java-21-amazon-corretto-headless postgresql16 nginx
   ```
   Instalar Tomcat 10.1 (manual, sección 4) y copiar
   [setenv.sh.qa.template](../deploy/qa/setenv.sh.qa.template) a `/opt/tomcat/bin/setenv.sh`
   (propietario `tomcat`, permisos `750`).
4. **Datos iniciales** (una vez, desde la EC2, con `-v ON_ERROR_STOP=1`): `database/initial_data.sql`,
   `insert_datos_maestros.sql`, `insert_parametrosistema.sql`, `insert_catalogos_linea_alma.sql` y
   `insert_usuarios_prueba.sql`. Los roles ya los crea `initial_data.sql`, y las tablas `estadosolicitud`
   y `estadocita` tienen columnas de auditoría, por lo que sus inserts deben nombrar las columnas.
5. **Imágenes**: `sudo mkdir -p /opt/casilda/uploads/contenidos && sudo chown -R tomcat:tomcat /opt/casilda/uploads`.
6. **Nginx**: proxy de `/api-casilda/` a `127.0.0.1:8080`, `client_max_body_size 2m`; HTTPS con
   `certbot --nginx` (renovación en cron). Puerto 8080 cerrado en el SG.
7. **Despliegue**: `mvn clean package -DskipTests`, copiar `target/api-casilda.war` a la EC2 y a
   `/opt/tomcat/webapps/`; reiniciar Tomcat.
8. **Respaldos**: [backup-uploads.sh](../deploy/qa/backup-uploads.sh) en cron hacia S3 (en trabajo).

## Conexión a la base de datos con pgAdmin (túnel SSH)

La base de datos no es pública: se accede a través de la EC2, que actúa como bastión, con un túnel SSH.

### 1. Crear el par de llaves (.pem) en AWS

1. Consola de AWS, región **us-east-2**: EC2, **Network & Security**, **Key Pairs**, **Create key pair**.
2. Nombre `casildaqa`, tipo **RSA**, formato **.pem**. La descarga ocurre una sola vez.
3. Guardarla y protegerla (Linux/macOS):
   ```bash
   mv ~/Descargas/casildaqa.pem ~/.ssh/ && chmod 400 ~/.ssh/casildaqa.pem
   ```
   En Windows, dejar el archivo en una carpeta propia y quitar los permisos de otros usuarios.
4. La llave solo se puede asociar a una instancia al crearla. Para dar acceso a otros desarrolladores,
   no compartir el `.pem`: cada uno agrega su propia llave pública en `~/.ssh/authorized_keys` de la EC2.
5. Verificar el acceso: `ssh -i ~/.ssh/casildaqa.pem ec2-user@<DOMINIO_O_IP_EC2>`.

### 2. Datos de conexión

| Dato | Valor |
|---|---|
| Host SSH (bastión) | dominio o IP pública de la EC2, usuario `ec2-user` |
| Host de la base de datos | endpoint de Aurora/RDS (el mismo de `SPRING_DATASOURCE_URL`) |
| Puerto | `5432` |
| Base de datos / usuario | `casildaqa` / `casildaqa` |

La EC2 debe estar en el SG que Aurora permite en el puerto 5432, y el puerto 22 de la EC2 debe estar
abierto para la IP del desarrollador.

### 3. Configurar pgAdmin

1. **Register, Server**.
2. Pestaña **General**: Name `casilda-qa`.
3. Pestaña **Connection**:
   - Host name/address: el endpoint de la base de datos (no la IP de la EC2).
   - Port: `5432`
   - Maintenance database: `casildaqa`
   - Username: `casildaqa`, y la contraseña.
4. Pestaña **SSH Tunnel**:
   - Use SSH tunneling: **Yes**
   - Tunnel host: dominio o IP pública de la EC2
   - Tunnel port: `22`
   - Username: `ec2-user`
   - Authentication: **Identity file**, y seleccionar `casildaqa.pem`
5. Pestaña **Parameters**: `SSL mode` en `require`.
6. **Save**. pgAdmin abre el túnel y se conecta a la base por el bastión.

### 4. Alternativa por línea de comandos

```bash
ssh -i ~/.ssh/casildaqa.pem -N -L 5433:<ENDPOINT_BD>:5432 ec2-user@<DOMINIO_O_IP_EC2>
```
Con el túnel abierto, en otra terminal o en pgAdmin (host `localhost`, puerto `5433`, sin pestaña SSH Tunnel):
```bash
psql "host=localhost port=5433 dbname=casildaqa user=casildaqa sslmode=require"
```
Si se conecta por `localhost` el certificado no coincide con el host, así que no usar `sslmode=verify-full`.

### Problemas frecuentes

| Síntoma | Causa probable |
|---|---|
| `Permission denied (publickey)` | Llave incorrecta o permisos distintos de `400` en el `.pem`. |
| Se queda esperando al abrir el túnel | Puerto 22 cerrado para tu IP en el SG de la EC2. |
| `Connection timed out` al base de datos | El SG de Aurora no permite el 5432 desde el SG de la EC2. |
| `password authentication failed` | Usuario o contraseña incorrectos del rol `casildaqa`. |
| El túnel funciona y luego falla | La IP o el endpoint cambió; la EC2 sin IP elástica cambia de IP al reiniciarse. |

## Variables de `setenv.sh` relevantes

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`: conexión a Aurora (`?sslmode=require`).
- `JWT_SECRET`: propio de QA, generado con `openssl rand -hex 32`.
- `APP_CORS_ALLOWED_ORIGINS`: orígenes permitidos separados por comas (por defecto `*`).
- `SERVER_FORWARD_HEADERS_STRATEGY=framework`: respeta las cabeceras del proxy nginx.
- `APP_CONTENIDOS_IMAGENES_DIRECTORIO`: carpeta de imágenes en el volumen EBS.

## Notas

- Usar `JWT_SECRET` y contraseña de base de datos propios de QA; no reutilizar los de producción.
- Los usuarios de `insert_usuarios_prueba.sql` comparten una contraseña conocida: cambiarla o eliminarlos antes de compartir la URL.
- Si las imágenes pasan a S3, solo cambia `ContenidoImagenStorageService`.
