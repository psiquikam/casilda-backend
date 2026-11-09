#!/usr/bin/env bash
# Pruebas manuales del contrato v2 de reportes contra un servidor local.
# Uso:
#   ./probar.sh
# Variables opcionales: BASE_URL, RUTA_ID, GENERO_ID, CARGO_ID,
# VINCULO_UDEA_ID, VINCULO_VICTIMA_ID, OMITIR_CATALOGOS
set -u

BASE_URL="${BASE_URL:-http://localhost:8080/api/v1}"
RUTA_ID="${RUTA_ID:-1}"
GENERO_ID="${GENERO_ID:-1}"
CARGO_ID="${CARGO_ID:-1}"
VINCULO_UDEA_ID="${VINCULO_UDEA_ID:-1}"
VINCULO_VICTIMA_ID="${VINCULO_VICTIMA_ID:-1}"
OMITIR_CATALOGOS="${OMITIR_CATALOGOS:-false}"

RESPUESTA=$(mktemp)
EVIDENCIA=$(mktemp --suffix=.pdf)
printf '%%PDF-1.7\nprueba manual\n' > "$EVIDENCIA"
trap 'rm -f "$RESPUESTA" "$EVIDENCIA"' EXIT

PASS=0
FAIL=0

# llamar METODO RUTA [CUERPO] -> deja el cuerpo en $BODY y el estado en $STATUS
llamar() {
  local metodo="$1" ruta="$2" cuerpo="${3:-}"
  local args=(-sS -o "$RESPUESTA" -w '%{http_code}' -X "$metodo"
    "$BASE_URL$ruta" -H 'Content-Type: application/json')
  [ -n "$cuerpo" ] && args+=(-d "$cuerpo")
  STATUS=$(curl "${args[@]}")
  BODY=$(cat "$RESPUESTA")
}

# verificar DESCRIPCION ESTADO_ESPERADO [FILTRO_JQ_QUE_DEBE_SER_TRUE]
verificar() {
  local desc="$1" esperado="$2" filtro="${3:-}" ok=1
  [ "$STATUS" = "$esperado" ] || ok=0
  if [ "$ok" = 1 ] && [ -n "$filtro" ]; then
    [ "$(echo "$BODY" | jq -r "$filtro" 2>/dev/null)" = "true" ] || ok=0
  fi
  if [ "$ok" = 1 ]; then
    PASS=$((PASS + 1))
    echo "  OK    $desc"
  else
    FAIL=$((FAIL + 1))
    echo "  FALLA $desc (estado $STATUS, esperado $esperado)"
    echo "$BODY" | head -c 500
    echo
  fi
}

if [ "$OMITIR_CATALOGOS" = "true" ]; then
  echo "== Catálogos públicos (omitidos) =="
else
  echo "== Catálogos públicos =="
  llamar GET /maestros/rutas-activacion
  verificar "rutas de activación disponibles" 200 \
    "type == \"array\" and length > 0"
  if [ "$STATUS" != "200" ]; then
    echo "No se pudieron consultar los catálogos."
    echo "Use OMITIR_CATALOGOS=true si la base local aún no tiene esos permisos."
    exit 1
  fi

  llamar GET /maestros/identidades-genero
  verificar "identidades de género disponibles" 200 "type == \"array\""
  llamar GET /maestros/cargos
  verificar "cargos disponibles" 200 "type == \"array\""
fi

echo "== Validaciones =="
llamar POST /reportes '{
  "descripcionHechos": "Prueba manual de validación.",
  "rutaActivacionId": '"$RUTA_ID"',
  "contacto": {
    "deseaContacto": true
  }
}'
verificar "contacto solicitado sin medio devuelve 400" 400 \
  '.validationErrors.contacto != null'

echo "== Crear reporte v2 =="
CUERPO=$(cat <<EOF
{
  "descripcionHechos": "Reporte creado durante la prueba manual del contrato v2.",
  "rutaActivacionId": $RUTA_ID,
  "tipoReportante": "TERCERO",
  "victima": {
    "primerNombre": "Ana",
    "primerApellido": "Prueba",
    "correo": "ana.prueba@example.com",
    "identidadGeneroId": $GENERO_ID,
    "cargoId": $CARGO_ID
  },
  "contacto": {
    "deseaContacto": true,
    "correo": "contacto.prueba@example.com",
    "whatsapp": "3001234567"
  },
  "presuntoAgresor": {
    "numeroDocumento": "98765432",
    "primerNombre": "Carlos",
    "primerApellido": "Prueba",
    "vinculoUniversidadId": $VINCULO_UDEA_ID,
    "vinculoVictimaId": $VINCULO_VICTIMA_ID,
    "informacionAdicional": "Registro generado para pruebas manuales.",
    "remitirUad": true
  }
}
EOF
)

llamar POST /reportes "$CUERPO"
verificar "crear reporte v2" 201 \
  '.codigo | startswith("CAS-")'
if [ "$STATUS" = "201" ]; then
  echo "  Código generado: $(echo "$BODY" | jq -r '.codigo')"
  verificar "respuesta inicia la ruta como SOLICITADA" 201 \
    '.rutaActivacion.estado == "SOLICITADA"'
fi

echo "== Crear reporte multipart con evidencia =="
CUERPO_MULTIPART=$(printf '%s' "$CUERPO")
STATUS=$(curl -sS -o "$RESPUESTA" -w '%{http_code}' \
  -X POST "$BASE_URL/reportes" \
  -F "reporte=$CUERPO_MULTIPART;type=application/json" \
  -F "evidencias=@$EVIDENCIA;type=application/pdf")
BODY=$(cat "$RESPUESTA")
verificar "crear reporte multipart" 201 \
  '.codigo | startswith("CAS-")'

echo
echo "Resultado: $PASS correctas, $FAIL con falla"
[ "$FAIL" = 0 ]
