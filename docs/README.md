# Documentacion tecnica y notas del proyecto

## Backend y OpenPhish

OpenPhish se integra solo en el backend como fuente auxiliar especializada en
phishing conocido. No sustituye Google Web Risk ni IPQualityScore, que siguen
siendo las fuentes primarias del analisis en tiempo real.

Resumen de diseno:
- OpenPhish vive solo en backend.
- No se incluye dentro de Android.
- No cambia el contrato publico de `POST /api/v1/analyze`.
- Si OpenPhish no esta disponible, el backend sigue funcionando con Web Risk e IPQS.
- Si OpenPhish encuentra una coincidencia relevante, aporta evidencia adicional
  para bloquear `safe` o elevar el riesgo segun las reglas actuales.

## Variables de entorno

Las variables de OpenPhish del backend son:
- `OPENPHISH_ENABLED`
- `OPENPHISH_DB_PATH`

Ejemplo en `backend/.env.example`:

```env
OPENPHISH_ENABLED=false
OPENPHISH_DB_PATH=/app/data/openphish.sqlite
```

Comportamiento esperado:
- `OPENPHISH_ENABLED=false`: OpenPhish queda deshabilitado de forma explicita.
- `OPENPHISH_ENABLED=true`: el backend intenta usar la SQLite local.
- Si la base no existe o no esta configurada, el backend no falla en el arranque.

## Importacion del CSV a SQLite

Con Docker, la importacion puede automatizarse al arrancar el contenedor.
Si `OPENPHISH_ENABLED=true`, existe `backend/data/openphish.csv` y todavia no
existe `backend/data/openphish.sqlite`, el contenedor generara la SQLite antes
de arrancar Uvicorn.

Flujo recomendado con Docker:

1. Guardar el CSV real como `backend/data/openphish.csv`.
2. Configurar `OPENPHISH_ENABLED=true`.
3. Configurar `OPENPHISH_DB_PATH=/app/data/openphish.sqlite`.
4. Ejecutar `docker compose up --build`.

Si `openphish.csv` no existe, el backend sigue arrancando y OpenPhish se
tratara como no disponible.

La importacion manual sigue disponible. El script actual es:

```powershell
cd backend
python scripts/import_openphish_csv.py .\ruta\openphish.csv .\data\openphish.sqlite
```

El script:
- crea la tabla `openphish_entries` si no existe;
- normaliza URL y host con la misma logica que usa el backend en runtime;
- inserta con `INSERT OR IGNORE`;
- evita duplicados por `normalized_url`;
- crea indices para consulta por URL, host, familia y fecha de descubrimiento;
- muestra solo un resumen final de filas leidas, importadas, duplicadas e invalidas.

Ni el CSV ni la SQLite deben subirse al repositorio. El repo solo incluye
`backend/data/openphish.example.csv` con la cabecera esperada y
`backend/data/README.md` con instrucciones de uso local.

## Limitaciones actuales

- OpenPhish solo cubre phishing conocido. No cubre malware general, software no
  deseado ni reputacion contextual de la misma forma que Web Risk e IPQS.
- La cobertura de OpenPhish no es uniforme entre sectores ni regiones. Por eso
  se mantiene como fuente auxiliar y no como reemplazo de las consultas en
  tiempo real.
- El `exact URL match` solo detecta la URL exacta indexada por OpenPhish. Si
  una campana usa variantes con tokens distintos en `path` o `query`, puede no
  haber coincidencia aunque pertenezca a la misma campana.
- En el MVP no hay actualizacion automatica del feed ni de la SQLite.
- El `host match` solo usa host exacto. No se hace `root domain match`, ni
  `registered domain match`, ni comparacion parcial por plataforma.
- Que OpenPhish no encuentre coincidencia no significa que la URL sea segura.

## Privacidad y manejo de datos

- No redistribuir el feed de OpenPhish dentro del repositorio.
- No incluir el CSV ni la SQLite en Git.
- No meter datos de OpenPhish en Android ni en la APK.
- No exponer datos del feed al cliente.
- No registrar URLs completas del usuario en logs de produccion.

## Nota academica

Si OpenPhish se usa en la memoria del TFG o en material de evaluacion,
debe citarse segun las condiciones del Academic Use Program e incluir la
fecha de acceso o de obtencion del feed utilizado.
