import logging
import time
from urllib.parse import urlparse

from fastapi import APIRouter, Request

from app.core.config import RATE_LIMIT_ANALYZE
from app.core.rate_limiter import limiter
from app.models.schemas import AnalyzeUrlRequest, AnalyzeUrlResponse
from app.services.analysis_service import analyze_url_with_providers

router = APIRouter()
logger = logging.getLogger(__name__)


@router.post("/api/v1/analyze", response_model=AnalyzeUrlResponse)
@limiter.limit(RATE_LIMIT_ANALYZE)
async def analyze_url(request: Request, payload: AnalyzeUrlRequest) -> AnalyzeUrlResponse:
    request_id = getattr(request.state, "request_id", None)

    url = payload.url
    logger.info(
        "Nueva peticion de analisis recibida en /api/v1/analyze | request_id=%s",
        request_id,
    )

    parsed = urlparse(url)
    logger.info(
        "URL valida para analisis | request_id=%s | scheme=%s | host=%s",
        request_id,
        parsed.scheme,
        parsed.hostname,
    )

    start_time = time.perf_counter()
    outcome = await analyze_url_with_providers(url, request_id=request_id)
    duration_ms = int((time.perf_counter() - start_time) * 1000)

    logger.info(
        "Resultado Web Risk | request_id=%s | provider_status=%s | available=%s | match_found=%s | threat_types=%s",
        request_id,
        outcome.web_risk.provider_status,
        outcome.web_risk.available,
        outcome.web_risk.match_found,
        list(outcome.web_risk.threat_types),
    )
    logger.info(
        "Resultado IPQS | request_id=%s | provider_status=%s | available=%s | success=%s | risk_score=%s | phishing=%s | malware=%s | suspicious=%s | unsafe=%s",
        request_id,
        outcome.ipqs.provider_status,
        outcome.ipqs.available,
        outcome.ipqs.success,
        outcome.ipqs.risk_score,
        outcome.ipqs.phishing,
        outcome.ipqs.malware,
        outcome.ipqs.suspicious,
        outcome.ipqs.unsafe,
    )
    if outcome.web_risk.provider_status != "ok":
        logger.warning(
            "Proveedor Web Risk con resultado no utilizable | request_id=%s | provider_status=%s | raw_summary=%s",
            request_id,
            outcome.web_risk.provider_status,
            outcome.web_risk.raw_summary,
        )
    if outcome.ipqs.provider_status != "ok":
        logger.warning(
            "Proveedor IPQS con resultado no utilizable | request_id=%s | provider_status=%s | raw_summary=%s",
            request_id,
            outcome.ipqs.provider_status,
            outcome.ipqs.raw_summary,
        )

    logger.info(
        "Resultado de analisis | request_id=%s | risk_level=%s | analysis_status=%s | duration_ms=%s",
        request_id,
        outcome.response.risk_level,
        outcome.response.analysis_status,
        duration_ms,
    )

    return outcome.response
