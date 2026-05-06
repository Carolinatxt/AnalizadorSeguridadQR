import logging
import time
from urllib.parse import urlparse

from fastapi import APIRouter, Request

from app.core.config import RATE_LIMIT_ANALYZE
from app.core.rate_limiter import limiter
from app.domain.analysis_rules import get_dangerous_source
from app.models.screenshot_schemas import ScreenshotRequest, ScreenshotResponse
from app.models.schemas import AnalyzeUrlRequest, AnalyzeUrlResponse
from app.services.analysis_service import analyze_url_with_providers
from app.services.screenshot_service import create_screenshot_preview

router = APIRouter()
logger = logging.getLogger(__name__)
RATE_LIMIT_SCREENSHOT = "5/minute"


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
    logger.info(
        "Resultado OpenPhish | request_id=%s | provider_status=%s | available=%s | match_found=%s | match_type=%s | age_days=%s | family_id=%s | brand=%s | sector=%s | is_spear=%s",
        request_id,
        outcome.openphish.provider_status,
        outcome.openphish.available,
        outcome.openphish.match_found,
        outcome.openphish.match_type,
        outcome.openphish.age_days,
        outcome.openphish.family_id,
        outcome.openphish.brand,
        outcome.openphish.sector,
        outcome.openphish.is_spear,
    )
    logger.info(
        "Resultado heuristicas locales | request_id=%s | available=%s | signals=%s",
        request_id,
        outcome.local_heuristics.available,
        [signal.code for signal in outcome.local_heuristics.signals],
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
    if outcome.openphish.provider_status not in {"ok", "disabled"}:
        logger.warning(
            "Proveedor OpenPhish con resultado no utilizable | request_id=%s | provider_status=%s | raw_summary=%s",
            request_id,
            outcome.openphish.provider_status,
            outcome.openphish.raw_summary,
        )

    dangerous_source = None
    if outcome.response.risk_level == "dangerous":
        dangerous_source = get_dangerous_source(
            outcome.web_risk,
            outcome.ipqs,
            outcome.openphish,
        )
        logger.warning(
            "Resultado dangerous detectado | request_id=%s | dangerous_source=%s | analysis_status=%s",
            request_id,
            dangerous_source,
            outcome.response.analysis_status,
        )

    logger.info(
        "Resultado de analisis | request_id=%s | risk_level=%s | analysis_status=%s | dangerous_source=%s | duration_ms=%s",
        request_id,
        outcome.response.risk_level,
        outcome.response.analysis_status,
        dangerous_source,
        duration_ms,
    )

    return outcome.response


@router.post("/api/v1/screenshot", response_model=ScreenshotResponse)
@limiter.limit(RATE_LIMIT_SCREENSHOT)
async def create_screenshot(
    request: Request,
    payload: ScreenshotRequest,
) -> ScreenshotResponse:
    request_id = getattr(request.state, "request_id", None)

    url = payload.url
    logger.info(
        "Nueva peticion de vista previa recibida en /api/v1/screenshot | request_id=%s",
        request_id,
    )

    parsed = urlparse(url)
    logger.info(
        "URL valida para vista previa | request_id=%s | scheme=%s | host=%s",
        request_id,
        parsed.scheme,
        parsed.hostname,
    )

    start_time = time.perf_counter()
    result = await create_screenshot_preview(url, request_id=request_id)
    duration_ms = int((time.perf_counter() - start_time) * 1000)

    logger.info(
        "Respuesta de vista previa | request_id=%s | available=%s | provider_status=%s | duration_ms=%s",
        request_id,
        result.available,
        result.provider_status,
        duration_ms,
    )

    return ScreenshotResponse(
        available=result.available,
        image_url=result.image_url,
        message=result.message,
    )
