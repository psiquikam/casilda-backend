# Pruebas manuales: reporte de caso v2

Requisitos: servidor local en marcha, `curl` y `jq`.

```bash
cd tests-manuales/reportes
./probar.sh
```

El endpoint de creación es público y el script no requiere autenticación. Por defecto
usa `http://localhost:8080/api/v1` y los IDs `1` para la ruta, identidad de género,
cargo y vínculos. Se pueden cambiar con estas variables:

```bash
BASE_URL=http://localhost:8080/api/v1 \
RUTA_ID=1 GENERO_ID=1 CARGO_ID=1 \
VINCULO_UDEA_ID=1 VINCULO_VICTIMA_ID=1 \
./probar.sh
```

Si la base local todavía no tiene cargados los permisos públicos de catálogos,
se puede probar únicamente el endpoint de reportes:

```bash
OMITIR_CATALOGOS=true ./probar.sh
```

El script consulta los catálogos públicos, valida un caso inválido y crea un reporte
con los bloques de víctima, contacto y presunto agresor. La creación persiste un
registro real y no existe todavía un endpoint público para eliminarlo; usarlo
preferiblemente en una base local de pruebas.
