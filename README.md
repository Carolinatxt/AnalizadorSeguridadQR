# AnalizadorSeguridadQR

Aplicacion Android para escanear codigos QR y analizar la seguridad de las URLs detectadas mediante un backend FastAPI. El proyecto combina validacion local, proveedores externos de reputacion y un historial privado almacenado solo en el dispositivo.

## Autora

Carolina De la Losa

TFC (Trabajo Fin de Ciclo)

## Vision general

La app esta dividida en dos piezas:

- `android/`: cliente Android nativo en Kotlin + Jetpack Compose.
- `backend/`: API FastAPI que valida URLs, consulta proveedores de seguridad y devuelve una clasificacion.

Flujo principal:

1. El usuario escanea un QR o pega una URL manualmente.
2. Android valida que sea una URL web compatible.
3. La app llama a `POST /api/v1/analyze`.
4. El backend valida la URL y consulta Google Web Risk, IPQualityScore y, opcionalmente, OpenPhish.
5. El motor de reglas devuelve `safe`, `suspicious` o `dangerous`, junto con `summary`, `reasons` y `analysis_status`.
6. Android muestra el resultado y guarda un resumen en historial local con Room.

## Frontend Android

Stack principal:

- Kotlin
- Jetpack Compose + Material 3
- ZXing (`journeyapps`) para escaneo QR
- Retrofit + OkHttp para red
- Room para historial local
- DataStore para preferencias de tema
- Coil 3 para cargar la vista previa remota

Arquitectura general del cliente:

- `MainActivity` coordina la navegacion principal entre `SCAN`, `HISTORY` y `SETTINGS`.
- `MainViewModel` concentra el estado del flujo de escaneo y analisis.
- `HistoryViewModel` observa Room y alimenta la pestaña de historial.
- `network/` encapsula Retrofit, OkHttp y los contratos con el backend.
- `data/` separa persistencia local, repositorios y preferencias.
- `security/` aplica validaciones y politicas del lado cliente antes de abrir enlaces o pedir vistas previas.
- `ui/` organiza pantallas, componentes reutilizables, navegacion, estado visual y tema.

Capacidades implementadas:

- Escaneo de QR con permiso de camara.
- Entrada manual de URL.
- Pantalla principal con estados `idle`, `loading`, `error`, `not-a-web-url` y `analysis-result`.
- Historial local de los ultimos 100 analisis.
- Filtros en historial por riesgo.
- Reanalisis desde historial.
- Ajustes de apariencia, privacidad e historial.
- Politica de apertura segura del enlace: los enlaces no siempre se abren directamente.
- Vista previa opcional de la pagina analizada mediante SnapRender.

Detalles funcionales del frontend:

- Validacion local previa de URLs: solo acepta `http` o `https`, rechaza `userinfo`, caracteres de control y cadenas demasiado largas.
- Modelo de riesgo visual separado del backend: `SAFE`, `SUSPICIOUS`, `DANGEROUS`, `UNKNOWN`.
- Estado tecnico del analisis separado del riesgo: `COMPLETE`, `PARTIAL`, `UNAVAILABLE`, `UNKNOWN`.
- Reintento del ultimo analisis valido sin necesidad de volver a escanear.
- Guardado automatico en historial local de resumen, razones, dominio visible, fecha, nivel de riesgo y estado del analisis.
- Limite de historial: se conservan los ultimos 100 registros en Room.
- Confirmacion adicional antes de abrir enlaces sospechosos o resultados no concluyentes.
- Bloqueo o confirmacion especifica para la vista previa avanzada segun riesgo y completitud del analisis.



### Estructura del frontend

```text
android/
|- app/
|  |- src/main/
|  |  |- java/com/carolina/analizadorseguridadqr/
|  |  |  |- MainActivity.kt           # Punto de entrada principal
|  |  |  |- QrCaptureActivity.kt      # Pantalla dedicada al escaner QR
|  |  |  |- data/
|  |  |  |  |- local/history/         # Room: entidad, DAO, base de datos, converters
|  |  |  |  |- preferences/           # DataStore para tema
|  |  |  |  `- repository/            # Repositorios del cliente
|  |  |  |- network/
|  |  |  |  |- model/                 # DTOs de peticion/respuesta
|  |  |  |  |- AnalysisApi*.kt        # Cliente y servicio de analisis
|  |  |  |  |- Screenshot*.kt         # Cliente y servicio de vista previa
|  |  |  |  `- BackendConfig.kt       # URL base del backend
|  |  |  |- security/                 # Validacion de URL y reglas de apertura
|  |  |  |- ui/
|  |  |  |  |- components/            # Componentes Compose reutilizables
|  |  |  |  |- history/               # Pantallas y mapeo del historial
|  |  |  |  |- navigation/            # Tabs y barra inferior
|  |  |  |  |- screen/                # Pantalla principal de analisis
|  |  |  |  |- security/              # Politicas UI de apertura y preview
|  |  |  |  |- settings/              # Ajustes y subpantallas
|  |  |  |  |- state/                 # Estados visuales del flujo principal
|  |  |  |  `- theme/                 # Colores, tipografia y tema Compose
|  |  |  `- viewmodel/                # MainViewModel y factories
|  |  `- res/                         # Recursos Android
|  `- build.gradle.kts
|- build.gradle.kts
`- settings.gradle.kts
```

## Backend FastAPI

Stack principal:

- Python 3.11
- FastAPI
- Pydantic
- httpx
- slowapi
- tldextract
- uvicorn

Endpoints expuestos:

- `GET /api/v1/health`
- `POST /api/v1/analyze`
- `POST /api/v1/screenshot`

Funciones del backend:

- Validacion comun de URLs HTTP/HTTPS publicas.
- Rechazo de URLs vacias, demasiado largas, con caracteres de control, credenciales embebidas o IPs privadas/locales/reservadas.
- Cliente HTTP global compartido con `follow_redirects=False`.
- `request_id` por peticion para trazabilidad.
- Manejo centralizado de errores 422, 429 y 500.
- `rate limiting` por IP.
- Timeout total del analisis.
- Logs estructurados de proveedor y de resultado.

### Proveedores y motor de decision

`POST /api/v1/analyze` combina cuatro fuentes:

- Google Web Risk
- IPQualityScore
- OpenPhish (opcional, via SQLite local)
- Heuristicas locales sin red

Las heuristicas locales buscan señales estructurales como:

- uso de IP publica en vez de dominio
- acortadores de URL
- marcas embebidas en dominios no oficiales
- dominios lookalike
- hostnames excesivamente complejos
- keywords tipicas de phishing en host o path

Semantica de respuesta:

- `risk_level`
  - `safe`: Web Risk e IPQS disponibles, sin amenazas conocidas y sin senales que bloqueen la clasificacion segura.
  - `suspicious`: fallback conservador cuando hay senales medias, coincidencias historicas o incertidumbre operativa.
  - `dangerous`: amenazas graves detectadas por Web Risk, IPQS o una coincidencia reciente exacta en OpenPhish.
- `analysis_status`
  - `complete`: Web Risk e IPQS disponibles.
  - `partial`: uno de los proveedores no estuvo disponible, o OpenPhish aporta una coincidencia util sin cobertura completa.
  - `unavailable`: no fue posible completar la evaluacion principal.

`POST /api/v1/screenshot` no participa en la decision de seguridad. Solo genera una vista previa visual bajo demanda usando SnapRender.

## OpenPhish

OpenPhish esta integrado solo en backend y se usa como fuente auxiliar de phishing conocido.

- No forma parte de Android.
- No cambia el contrato publico del endpoint de analisis.
- Si no esta disponible, el backend sigue funcionando con las APIs Web Risk e IPQS.
- Si esta activado y hay `openphish.csv`, el contenedor puede importarlo a SQLite automaticamente al arrancar.

Documentacion tecnica ampliada:

- [docs/README.md](docs/README.md)
- [backend/data/README.md](backend/data/README.md)

## Estructura del proyecto

```text
AnalizadorSeguridadQR/
|- android/                  # App Android nativa
|- backend/
|  |- app/
|  |  |- api/                # Rutas FastAPI
|  |  |- core/               # Configuracion, logging, http client, rate limiter
|  |  |- domain/             # Reglas de decision y heuristicas locales
|  |  |- models/             # Schemas Pydantic
|  |  |- services/           # Integraciones con proveedores
|  |  `- utils/              # Validacion y normalizacion de URLs
|  |- data/                  # Datos locales opcionales de OpenPhish
|  |- scripts/               # Utilidades de importacion
|  |- Dockerfile
|  `- entrypoint.sh
|- docs/
`- docker-compose.yml
```

## Puesta en marcha local

### 1. Backend con Python

Desde `backend/`:

```bash
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload
```

Variables de entorno soportadas:

```env
WEBRISK_API_KEY=
GOOGLE_WEB_RISK_API_KEY=
IPQS_API_KEY=
SNAPRENDER_API_KEY=
OPENPHISH_ENABLED=false
OPENPHISH_DB_PATH=/app/data/openphish.sqlite
ANALYSIS_TOTAL_TIMEOUT_SECONDS=12
SCREENSHOT_TIMEOUT_SECONDS=6
```

Notas:

- `WEBRISK_API_KEY` y `GOOGLE_WEB_RISK_API_KEY` son alias funcionales para la misma integracion.
- Si faltan las API keys, el backend arranca igual, pero degradara el resultado a estados parciales o no disponibles.
- Ahora mismo no existe `backend/.env.example` en el repo, asi que las variables deben crearse manualmente en `backend/.env` o exportarse en el entorno.

### 2. Backend con Docker

Desde la raiz:

```bash
docker compose up --build
```

El `docker-compose.yml` expone el backend en `localhost:8000` y monta:

- `backend/app`
- `backend/data`
- `backend/scripts`

Si `OPENPHISH_ENABLED=true` y existe `backend/data/openphish.csv`, el `entrypoint.sh` intentara generar `openphish.sqlite` antes de arrancar Uvicorn.

### 3. Android

Abrir `android/` en Android Studio y ejecutar la app.

Para desarrollo local contra el backend del PC:

1. Arrancar el backend en `localhost:8000`.
2. Conectar el dispositivo Android por USB.
3. Ejecutar `adb reverse tcp:8000 tcp:8000`.
4. Lanzar la variante `debug`.

## Seguridad y privacidad

- La app no abre automaticamente todos los enlaces analizados.
- Las URLs se envian al backend para ser evaluadas.
- Durante el analisis, el backend puede enviarlas a proveedores externos como Web Risk e IPQS.
- Si el usuario solicita vista previa, la URL tambien se envia a SnapRender.
- El backend no implementa autenticacion ni almacenamiento de historial de usuario.
- El historial visible para el usuario se guarda en Room dentro del dispositivo.

## Limitaciones actuales

- La build `release` no esta lista para distribucion sin configurar una URL real de backend.
- El `rate limiting` usa la IP directa de la conexion; detras de proxy inverso necesita endurecimiento.
- El backend no autentica clientes ni protege con cuentas de usuario.
- No hay almacenamiento persistente de analisis en servidor.
- La vista previa remota depende de un tercero y puede devolver imagen cacheada.


## Resumen tecnico rapido

El proyecto ya tiene una base funcional clara:

- frontend Android bien separado entre UI, ViewModels, red y persistencia local
- backend desacoplado en validacion, servicios y reglas de negocio
- clasificacion conservadora para evitar falsos `safe`
- integracion opcional de OpenPhish y SnapRender sin romper el flujo principal
