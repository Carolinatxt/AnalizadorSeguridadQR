import logging
from urllib.parse import urlparse

from fastapi import APIRouter, HTTPException

from app.models.schemas import AnalyzeUrlRequest, AnalyzeUrlResponse
from app.services.web_risk_service import check_url_with_web_risk

router = APIRouter()
logger = logging.getLogger(__name__)


@router.post("/api/v1/analyze", response_model=AnalyzeUrlResponse)
async def analyze_url(payload: AnalyzeUrlRequest) -> AnalyzeUrlResponse:
    url = payload.url.strip()
    logger.info("Nueva peticion de analisis recibida en /api/v1/analyze")

    # Logs de rechazo para trazabilidad de respuestas 400.
    if not url:
        logger.warning("Solicitud rechazada: la URL esta vacia")
        raise HTTPException(status_code=400, detail="La URL está vacía")

    parsed = urlparse(url)

    if not parsed.scheme:
        logger.warning("Solicitud rechazada: la URL no es valida (sin esquema)")
        raise HTTPException(status_code=400, detail="La URL no es válida")

    if parsed.scheme not in {"http", "https"}:
        logger.warning(
            "Solicitud rechazada: esquema no permitido | scheme=%s",
            parsed.scheme,
        )
        raise HTTPException(
            status_code=400,
            detail="Solo se permiten URLs http o https"
        )

    if not parsed.hostname:
        logger.warning("Solicitud rechazada: la URL no es valida (sin host)")
        raise HTTPException(status_code=400, detail="La URL no es válida")

    # Logging de desarrollo: registramos solo esquema y host para no exponer la URL completa.
    logger.info(
        "URL valida para analisis | scheme=%s | host=%s",
        parsed.scheme,
        parsed.hostname,
    )

    web_risk_result = await check_url_with_web_risk(url)

    response: AnalyzeUrlResponse
    if not web_risk_result["available"]:
        response = AnalyzeUrlResponse(
            risk_level="suspicious",
            analysis_status="partial",
            summary="No fue posible completar el análisis con Web Risk."
        )
    elif web_risk_result["match_found"]:
        response = AnalyzeUrlResponse(
            risk_level="dangerous",
            analysis_status="complete",
            summary="Google Web Risk ha detectado una amenaza en esta URL."
        )
    else:
        response = AnalyzeUrlResponse(
            risk_level="safe",
            analysis_status="complete",
            summary="Google Web Risk no ha detectado amenazas conocidas en esta URL."
        )

    logger.info(
        "Resultado de analisis | risk_level=%s | analysis_status=%s",
        response.risk_level,
        response.analysis_status,
    )

    return response
