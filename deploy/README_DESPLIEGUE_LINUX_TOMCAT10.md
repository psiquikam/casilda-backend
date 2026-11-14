# Manual de Despliegue CASILDA en Linux con Tomcat 10

Este documento describe los requerimientos y el paso a paso para desplegar:

- Backend Spring Boot (WAR)
- Frontend Angular (estático)

sobre un servidor Linux con Apache Tomcat 10.

## 1. Arquitectura objetivo

- Tomcat 10 ejecuta dos aplicaciones:
  - API backend: contexto /api-casilda (WAR)
  - Frontend Angular: contexto / (ROOT) o /casilda
- Base de datos PostgreSQL accesible desde el servidor.

## 2. Requerimientos

## 2.1 Sistema operativo

- Ubuntu 22.04/24.04, Debian 12 o RHEL/Rocky/Alma 8+
- Acceso sudo
- systemd habilitado

## 2.2 Software

- Java 21 (JDK)
- Apache Maven 3.9+
- Node.js 20.x y npm 10+
- PostgreSQL client (psql) opcional para validaciones
- Apache Tomcat 10.1+

## 2.3 Puertos y red

- 8080/tcp (Tomcat) interno
- 80/443 (si se publica por Nginx/Apache reverse proxy)
- 5432/tcp (PostgreSQL) según topología

## 2.4 Artefactos del proyecto

- Backend WAR esperado: target/api-casilda.war
- Frontend build esperado: dist/casilda-fnsp/browser/

## 2.5 Usuario de base de datos requerido

Para este despliegue se debe crear el usuario PostgreSQL:

- Usuario: admin
- Contraseña: admin

Comandos (ejecutar como usuario postgres en el servidor de base de datos):

```bash
sudo -u postgres psql -c "DO $$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'admin') THEN CREATE ROLE admin LOGIN PASSWORD 'admin'; ELSE ALTER ROLE admin WITH LOGIN PASSWORD 'admin'; END IF; END $$;"
sudo -u postgres psql -c "ALTER ROLE admin CREATEDB;"
```

## 3. Preparación del servidor Linux

Crear usuario de ejecución y carpetas:

```bash
sudo useradd --system --home /opt/tomcat --shell /bin/false tomcat || true
sudo mkdir -p /opt/tomcat /opt/casilda/{backend,frontend,backups,logs}
sudo chown -R tomcat:tomcat /opt/tomcat /opt/casilda
```

Instalar Java 21 y utilidades:

Ubuntu/Debian:

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk curl unzip rsync
```

RHEL/Rocky/Alma:

```bash
sudo dnf install -y java-21-openjdk java-21-openjdk-devel curl unzip rsync
```

Verificar:

```bash
java -version
```

## 4. Instalación de Tomcat 10

Descargar e instalar Tomcat 10.1.x:

```bash
cd /tmp
curl -fLO https://downloads.apache.org/tomcat/tomcat-10/v10.1.41/bin/apache-tomcat-10.1.41.tar.gz
sudo tar -xzf apache-tomcat-10.1.41.tar.gz -C /opt/tomcat --strip-components=1
sudo chown -R tomcat:tomcat /opt/tomcat
sudo chmod +x /opt/tomcat/bin/*.sh
```

Crear servicio systemd:

```bash
sudo tee /etc/systemd/system/tomcat.service > /dev/null <<'EOF'
[Unit]
Description=Apache Tomcat 10
After=network.target

[Service]
Type=forking
User=tomcat
Group=tomcat
Environment=JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
Environment=CATALINA_PID=/opt/tomcat/temp/tomcat.pid
Environment=CATALINA_HOME=/opt/tomcat
Environment=CATALINA_BASE=/opt/tomcat
Environment='CATALINA_OPTS=-Xms512M -Xmx1024M -Dfile.encoding=UTF-8'
ExecStart=/opt/tomcat/bin/startup.sh
ExecStop=/opt/tomcat/bin/shutdown.sh
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF
```

Iniciar:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now tomcat
sudo systemctl status tomcat --no-pager
```

## 5. Despliegue del Backend (Spring Boot WAR)

## 5.1 Construir WAR

En la máquina de build (o en el servidor):

```bash
cd /ruta/casilda-backend
mvn clean package -DskipTests
```

Debe generarse: target/api-casilda.war

## 5.2 Configurar variables de entorno para Spring

El archivo application.yml incluye valores base de conexión a base de datos y deben revisarse para cada ambiente.

Archivo de referencia:

- src/main/resources/application.yml

Propiedades a validar/cambiar según el servidor destino:

- spring.datasource.url
- spring.datasource.username
- spring.datasource.password
- server.port
- server.servlet.context-path

Nota: en Tomcat, las variables definidas en setenv.sh (SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD, etc.) sobrescriben los valores de application.yml en tiempo de ejecución.

### CORS en QA para desarrollo local

`APP_CORS_ALLOWED_ORIGINS` es una lista separada por comas. Para permitir que el
frontend local en `http://localhost:4200` consuma QA, conservar también el origen
del frontend desplegado:

```bash
export APP_CORS_ALLOWED_ORIGINS="https://<ORIGEN_FRONTEND>,http://localhost:4200"
```

Aplicar el cambio en `/opt/tomcat/bin/setenv.sh` y reiniciar Tomcat. No usar `*` ni
añadir `localhost` a producción. Verificar el preflight:

```bash
curl -i -X OPTIONS "https://casildaqa.duckdns.org/api-casilda/auth/login" \
  -H "Origin: http://localhost:4200" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: content-type"
```

Debe responder `200` e incluir `Access-Control-Allow-Origin: http://localhost:4200`.

Crear setenv.sh de Tomcat:

```bash
sudo tee /opt/tomcat/bin/setenv.sh > /dev/null <<'EOF'
#!/usr/bin/env bash
export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
export JAVA_OPTS="-Xms512m -Xmx1024m -Dfile.encoding=UTF-8"

export SPRING_PROFILES_ACTIVE="prod"
export SERVER_PORT="8080"

# Debe coincidir con la URL usada por el frontend
export SERVER_SERVLET_CONTEXT_PATH="/api-casilda"

export SPRING_DATASOURCE_URL="jdbc:postgresql://<DB_HOST>:5432/casilda"
export SPRING_DATASOURCE_USERNAME="admin"
export SPRING_DATASOURCE_PASSWORD="admin"

export SPRING_JPA_HIBERNATE_DDL_AUTO="update"
export SPRING_JPA_SHOW_SQL="false"

export JWT_SECRET="<JWT_SECRET_PRODUCCION>"
export JWT_EXPIRATION="86400000"
EOF

sudo chmod +x /opt/tomcat/bin/setenv.sh
sudo chown tomcat:tomcat /opt/tomcat/bin/setenv.sh
```

## 5.3 Publicar el WAR

```bash
sudo systemctl stop tomcat

# Backup por fecha
sudo cp -f /opt/tomcat/webapps/api-casilda.war /opt/casilda/backups/api-casilda.war.$(date +%F-%H%M%S) 2>/dev/null || true

# Limpieza de despliegue anterior
sudo rm -rf /opt/tomcat/webapps/api-casilda /opt/tomcat/webapps/api-casilda.war

# Copia del nuevo artefacto
sudo cp -f /ruta/casilda-backend/target/api-casilda.war /opt/tomcat/webapps/api-casilda.war
sudo chown tomcat:tomcat /opt/tomcat/webapps/api-casilda.war

sudo systemctl start tomcat
```

## 5.4 Validación backend

```bash
curl -I http://127.0.0.1:8080/api-casilda
curl http://127.0.0.1:8080/api-casilda/maestros/tipos-solicitud
```

Si está habilitado Swagger en prod:

- http://<HOST>:8080/api-casilda/swagger-ui.html
- http://<HOST>:8080/api-casilda/api-docs

## 6. Despliegue del Frontend (Angular)

## 6.1 Ajustar API de producción

Antes de compilar, validar valor en environment.prod.ts:

- apiBaseUrl debe apuntar a la URL pública del backend, por ejemplo:
  - http://<HOST>/api-casilda
  - o https://<DOMINIO>/api-casilda

## 6.2 Construir frontend

```bash
cd /ruta/sistema-casilda-fnsp
npm ci
npm run build
```

Salida esperada:

- dist/casilda-fnsp/browser/

## 6.3 Publicar frontend en Tomcat

Opción recomendada: publicar en contexto raíz / (ROOT).

```bash
sudo systemctl stop tomcat

# Backup ROOT anterior
sudo rsync -a --delete /opt/tomcat/webapps/ROOT/ /opt/casilda/backups/frontend-root.$(date +%F-%H%M%S)/ 2>/dev/null || true

# Reemplazar estáticos
sudo rm -rf /opt/tomcat/webapps/ROOT/*
sudo cp -a /ruta/sistema-casilda-fnsp/dist/casilda-fnsp/browser/. /opt/tomcat/webapps/ROOT/
sudo chown -R tomcat:tomcat /opt/tomcat/webapps/ROOT

sudo systemctl start tomcat
```

Opción alternativa: publicar bajo /casilda

1. Compilar con base href:

```bash
npm run build -- --base-href /casilda/
```

2. Copiar a /opt/tomcat/webapps/casilda/

## 6.4 Validación frontend

```bash
curl -I http://127.0.0.1:8080/
```

Abrir en navegador:

- http://<HOST>:8080/

Probar login y navegación verificando llamadas a /api-casilda.

## 7. Flujo de despliegue recomendado (orden)

1. Ejecutar backup de backend y frontend.
2. Desplegar backend y validar endpoints críticos.
3. Desplegar frontend y validar login + pantallas principales.
4. Revisar logs por 10 a 15 minutos.

## 8. Operación y logs

Comandos útiles:

```bash
sudo systemctl status tomcat --no-pager
sudo journalctl -u tomcat -f
sudo tail -f /opt/tomcat/logs/catalina.out
```

## 9. Rollback

## 9.1 Backend

```bash
sudo systemctl stop tomcat
sudo cp -f /opt/casilda/backups/api-casilda.war.<FECHA> /opt/tomcat/webapps/api-casilda.war
sudo chown tomcat:tomcat /opt/tomcat/webapps/api-casilda.war
sudo systemctl start tomcat
```

## 9.2 Frontend

```bash
sudo systemctl stop tomcat
sudo rm -rf /opt/tomcat/webapps/ROOT/*
sudo cp -a /opt/casilda/backups/frontend-root.<FECHA>/. /opt/tomcat/webapps/ROOT/
sudo chown -R tomcat:tomcat /opt/tomcat/webapps/ROOT
sudo systemctl start tomcat
```

## 10. Restaurar backup de base de datos Casilda (PostgreSQL)

Esta sección aplica cuando se requiere recuperar la base de datos en un servidor PostgreSQL Linux.

## 10.1 Requisitos previos

- Archivo de respaldo disponible en el servidor (ejemplo: `/opt/casilda/backups/db/`)
- Usuario con permisos para crear/restaurar base de datos (por ejemplo `postgres`)
- Servicio PostgreSQL activo
- Detener temporalmente la aplicación para evitar escrituras durante la restauración

```bash
sudo systemctl stop tomcat
sudo systemctl status postgresql --no-pager
```

Crear/actualizar el usuario requerido por la aplicación:

```bash
sudo -u postgres psql -c "DO $$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'admin') THEN CREATE ROLE admin LOGIN PASSWORD 'admin'; ELSE ALTER ROLE admin WITH LOGIN PASSWORD 'admin'; END IF; END $$;"
sudo -u postgres psql -c "ALTER ROLE admin CREATEDB;"
```

## 10.2 Identificar tipo de backup

1. Backup SQL plano (`.sql`): se restaura con `psql`.
2. Backup formato custom/tar (`.backup`, `.dump`, `.tar`): se restaura con `pg_restore`.

## 10.3 Restaurar backup SQL plano (.sql)

Ejemplo de archivo: `/opt/casilda/backups/db/casilda_2026-04-07.sql`

```bash
sudo -u postgres psql -c "DROP DATABASE IF EXISTS casilda;"
sudo -u postgres psql -c "CREATE DATABASE casilda WITH ENCODING 'UTF8';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE casilda TO admin;"
sudo -u postgres psql -d casilda -f /opt/casilda/backups/db/casilda_2026-04-07.sql
```

## 10.4 Restaurar backup custom (.backup/.dump/.tar)

Ejemplo de archivo: `/opt/casilda/backups/db/casilda_2026-04-07.backup`

```bash
sudo -u postgres psql -c "DROP DATABASE IF EXISTS casilda;"
sudo -u postgres psql -c "CREATE DATABASE casilda WITH ENCODING 'UTF8';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE casilda TO admin;"
sudo -u postgres pg_restore \
  --dbname=casilda \
  --clean \
  --if-exists \
  --no-owner \
  --no-privileges \
  /opt/casilda/backups/db/casilda_2026-04-07.backup
```

Si el respaldo fue tomado con `-Fc` y contiene muchos objetos, puedes acelerar con:

```bash
sudo -u postgres pg_restore --dbname=casilda --jobs=4 /opt/casilda/backups/db/casilda_2026-04-07.backup
```

## 10.5 Restauración a otra base (prueba)

```bash
sudo -u postgres psql -c "DROP DATABASE IF EXISTS casilda_restore_test;"
sudo -u postgres psql -c "CREATE DATABASE casilda_restore_test WITH ENCODING 'UTF8';"
sudo -u postgres psql -d casilda_restore_test -f /opt/casilda/backups/db/casilda_2026-04-07.sql
```

## 10.6 Verificación post-restauración

```bash
sudo -u postgres psql -d casilda -c "\dt"
sudo -u postgres psql -d casilda -c "SELECT COUNT(*) AS total_municipios FROM municipio;"
sudo -u postgres psql -d casilda -c "SELECT COUNT(*) AS total_usuarios FROM usuario;"
```

Si todo es correcto:

```bash
sudo systemctl start tomcat
```

## 10.7 Errores comunes

- `database "casilda" is being accessed by other users`:
  - Cerrar conexiones activas antes de `DROP DATABASE`.
- `role ... does not exist`:
  - Restaurar con `--no-owner --no-privileges` o crear roles faltantes.
- `permission denied` sobre el archivo:
  - Ajustar permisos y propietario del respaldo.

## 11. Checklist de salida

- Tomcat activo sin errores al arranque
- Backend responde en /api-casilda
- Frontend carga y consume backend correctamente
- CORS/HTTPS validados según ambiente
- Backups del despliegue anterior guardados
- Credenciales y secretos fuera del código fuente

## 12. Archivos de referencia en este proyecto

- Plantilla Linux de entorno backend: deploy/tomcat/setenv.sh.template
- Plantilla Windows de entorno backend: deploy/tomcat/setenv.bat.template
- Build backend WAR (packaging/finalName): pom.xml
- URL de API en frontend producción: src/environments/environment.prod.ts
- Build output Angular: angular.json
