#!/usr/bin/env bash
# Sincroniza las imágenes de QA a S3. Aurora ya genera sus propios respaldos.
# Cron sugerido: 0 3 * * * /opt/casilda/backup-uploads.sh
set -euo pipefail

BUCKET="${QA_BACKUP_BUCKET:?Defina QA_BACKUP_BUCKET (ej: s3://casilda-qa-backups)}"
UPLOADS_DIR="${QA_UPLOADS_DIR:-/opt/casilda/uploads}"

aws s3 sync "$UPLOADS_DIR" "$BUCKET/uploads/" --only-show-errors
