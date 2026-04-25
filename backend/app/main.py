import logging

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from app.api.routes import router
from app.core.logging_config import configure_logging

configure_logging()
logger = logging.getLogger(__name__)

app = FastAPI(title="AnalizadorSeguridadQR API")

app.include_router(router)


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
        "Solicitud rechazada por validacion | method=%s | path=%s | errors=%s",
        request.method,
        request.url.path,
        sanitized_errors,
    )

    return JSONResponse(
        status_code=422,
        content={"detail": "Datos de entrada no válidos"},
    )


@app.get("/api/v1/health")
def health():
    return {"status": "ok", "service": "backend"}
