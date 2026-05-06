# Datos Locales De OpenPhish

Esta carpeta se usa para los datos locales de OpenPhish del backend.

- `openphish.csv` es el CSV real descargado del Academic Use Program.
- `openphish.csv` NO se sube al repositorio.
- `openphish.sqlite` se genera automaticamente a partir del CSV.
- `openphish.sqlite` NO se sube al repositorio.

Para activar OpenPhish con Docker:

1. Copia el CSV real como `backend/data/openphish.csv`.
2. Configura `OPENPHISH_ENABLED=true`.
3. Configura `OPENPHISH_DB_PATH=/app/data/openphish.sqlite`.
4. Ejecuta `docker compose up --build`.

Si no existe `openphish.sqlite`, Docker intentara generarla automaticamente
desde `openphish.csv` al arrancar el contenedor.

Notas importantes:

- OpenPhish no se descarga automaticamente desde internet.
- OpenPhish solo cubre phishing conocido y documentado.
- La cobertura de OpenPhish no es universal.
- Google Web Risk e IPQualityScore siguen siendo las fuentes principales.
