import logging
from urllib.parse import urlparse

from fastapi import APIRouter, Request

from app.core.rate_limiter import limiter
from app.models.schemas import AnalyzeUrlRequest, AnalyzeUrlResponse
from app.services.analysis_service import analyze_url_with_providers

router = APIRouter()
logger = logging.getLogger(__name__)


@router.post("/api/v1/analyze", response_model=AnalyzeUrlResponse)
@limiter.limit("30/minute")
async def analyze_url(request: Request, payload: AnalyzeUrlRequest) -> AnalyzeUrlResponse:
    # SlowAPI necesita request en la firma para aplicar limites por IP.
    _ = request

    url = payload.url
    logger.info("Nueva peticion de analisis recibida en /api/v1/analyze")

    parsed = urlparse(url)
    logger.info(
        "URL valida para analisis | scheme=%s | host=%s",
        parsed.scheme,
        parsed.hostname,
    )

    outcome = await analyze_url_with_providers(url)

    logger.info(
        "Resultado Web Risk | provider_status=%s | available=%s | match_found=%s | threat_types=%s",
        outcome.web_risk.provider_status,
        outcome.web_risk.available,
        outcome.web_risk.match_found,
        list(outcome.web_risk.threat_types),
    )
    logger.info(
        "Resultado IPQS | provider_status=%s | available=%s | success=%s | risk_score=%s | phishing=%s | malware=%s | suspicious=%s | unsafe=%s",
        outcome.ipqs.provider_status,
        outcome.ipqs.available,
        outcome.ipqs.success,
        outcome.ipqs.risk_score,
        outcome.ipqs.phishing,
        outcome.ipqs.malware,
        outcome.ipqs.suspicious,
        outcome.ipqs.unsafe,
    )

    if outcome.response.analysis_status == "unavailable":
        logger.warning("Analisis unavailable: Web Risk e IPQS no disponibles")
    elif outcome.response.analysis_status == "partial":
        logger.warning("Analisis partial: una API externa no estuvo disponible")

    logger.info(
        "Resultado de analisis | risk_level=%s | analysis_status=%s",
        outcome.response.risk_level,
        outcome.response.analysis_status,
    )

    return outcome.response
