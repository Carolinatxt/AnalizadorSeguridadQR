import logging
import uuid
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from slowapi.errors import RateLimitExceeded
from app.api.routes import router
from app.core.http_client import close_http_client, get_http_client, init_http_client
from app.core.logging_config import configure_logging
from app.core.rate_limiter import limiter

configure_logging()
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(_app: FastAPI):
    try:
        await init_http_client()
        get_http_client()
        logger.info("Cliente HTTP global inicializado correctamente")
    except Exception:
        logger.exception("Fallo al inicializar cliente HTTP global; se aborta el arranque")
        raise

    try:
        yield
    finally:
        await close_http_client()
        logger.info("Cliente HTTP global cerrado")


app = FastAPI(title="AnalizadorSeguridadQR API", lifespan=lifespan)
app.state.limiter = limiter

app.include_router(router)


@app.middleware("http")
async def request_id_middleware(request: Request, call_next):
    request_id = uuid.uuid4().hex[:8]
    request.state.request_id = request_id
    response = await call_next(request)
    response.headers["X-Request-ID"] = request_id
    return response


@app.exception_handler(RequestValidationError)
async def request_validation_exception_handler(
    request: Request,
    exc: RequestValidationError,
) -> JSONResponse:
    # No registramos el payload completo para evitar filtrar datos sensibles.
    sanitized_errors = [
        {
            "type": err.get("type"),
            "loc": err.get("loc"),
            "msg": err.get("msg"),
        }
        for err in exc.errors()
    ]
    logger.warning(
        "Solicitud rechazada por validacion | request_id=%s | method=%s | path=%s | errors=%s",
        getattr(request.state, "request_id", None),
        request.method,
        request.url.path,
        sanitized_errors,
    )

    # Contrato estable hacia cliente: respuesta minima; detalle tecnico solo en logs.
    return JSONResponse(
        status_code=422,
        content={"detail": "Datos de entrada no válidos"},
    )


@app.exception_handler(RateLimitExceeded)
async def rate_limit_exceeded_handler(
    request: Request,
    _exc: RateLimitExceeded,
) -> JSONResponse:
    logger.warning(
        "Rate limit excedido | request_id=%s | method=%s | path=%s | client=%s",
        getattr(request.state, "request_id", None),
        request.method,
        request.url.path,
        request.client.host if request.client else "unknown",
    )
    return JSONResponse(
        status_code=429,
        content={"detail": "Demasiadas solicitudes. Inténtalo de nuevo más tarde."},
    )


@app.exception_handler(Exception)
async def unhandled_exception_handler(
    request: Request,
    exc: Exception,
) -> JSONResponse:
    logger.exception(
        "Excepcion no controlada | request_id=%s | method=%s | path=%s",
        getattr(request.state, "request_id", None),
        request.method,
        request.url.path,
        exc_info=exc,
    )
    return JSONResponse(
        status_code=500,
        content={"detail": "Error interno del servidor"},
    )


@app.get("/api/v1/health")
def health():
    return {"status": "ok", "service": "backend"}
