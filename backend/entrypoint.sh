#!/bin/sh
set -e

OPENPHISH_CSV_PATH="/app/data/openphish.csv"
OPENPHISH_SQLITE_PATH="/app/data/openphish.sqlite"

if [ "$OPENPHISH_ENABLED" = "true" ]; then
  if [ -f "$OPENPHISH_CSV_PATH" ] && [ ! -f "$OPENPHISH_SQLITE_PATH" ]; then
    echo "Importando OpenPhish CSV a SQLite..."
    python /app/scripts/import_openphish_csv.py "$OPENPHISH_CSV_PATH" "$OPENPHISH_SQLITE_PATH"
  elif [ -f "$OPENPHISH_SQLITE_PATH" ]; then
    echo "Base SQLite de OpenPhish encontrada. No se reimporta."
  else
    echo "OpenPhish activado, pero no existe /app/data/openphish.csv. El backend arrancara igualmente."
  fi
else
  echo "OpenPhish desactivado. No se importan datos locales."
fi

exec uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
