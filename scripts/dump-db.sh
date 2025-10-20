#!/usr/bin/env bash
set -euo pipefail

PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5432}"
PGDATABASE="${PGDATABASE:-minurl}"
PGUSER="${PGUSER:-minurl}"
PGPASSWORD="${PGPASSWORD:-minurl}"
BACKUP_DIR="${1:-backups}"

export PGHOST PGPORT PGDATABASE PGUSER PGPASSWORD

timestamp="$(date -u +"%Y%m%d-%H%M%SZ")"
mkdir -p "${BACKUP_DIR}"
dump_path="${BACKUP_DIR}/${PGDATABASE}-${timestamp}.dump"

pg_dump --clean --if-exists --format=custom --file="${dump_path}"

find "${BACKUP_DIR}" -type f -name "*.dump" -mtime +7 -delete

echo "Wrote ${dump_path}"
