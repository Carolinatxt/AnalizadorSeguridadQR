import asyncio
import logging
from dataclasses import dataclass

from app.core.config import ANALYSIS_TOTAL_TIMEOUT_SECONDS
from app.domain.analysis_rules import _build_response
from app.models.schemas import AnalyzeUrlResponse
from app.services.ipqs_service import check_url_with_ipqs
from app.services.provider_results import IpqsResult, WebRiskResult
from app.services.web_risk_service import check_url_with_web_risk

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class AnalysisOutcome:
    response: AnalyzeUrlResponse
    web_risk: WebRiskResult
    ipqs: IpqsResult


def _normalize_web_risk_result(
    result: WebRiskResult | Exception,
    request_id: str | None = None,
) -> WebRiskResult:
    if isinstance(result, Exception):
        logger.error(
            "Excepcion no capturada en Web Risk | request_id=%s",
            request_id,
            exc_info=result,
        )
        return WebRiskResult.from_internal_error("excepcion interna")
    return result


def _normalize_ipqs_result(
    result: IpqsResult | Exception,
    request_id: str | None = None,
) -> IpqsResult:
    if isinstance(result, Exception):
        logger.error(
            "Excepcion no capturada en IPQS | request_id=%s",
            request_id,
            exc_info=result,
        )
        return IpqsResult.from_internal_error("excepcion interna")
    return result


async def analyze_url_with_providers(
    url: str,
    request_id: str | None = None,
) -> AnalysisOutcome:
    try:
        web_risk_result, ipqs_result = await asyncio.wait_for(
            asyncio.gather(
                check_url_with_web_risk(url, request_id=request_id),
                check_url_with_ipqs(url, request_id=request_id),
                return_exceptions=True,
            ),
            timeout=ANALYSIS_TOTAL_TIMEOUT_SECONDS,
        )
    except asyncio.TimeoutError:
        logger.error(
            "Timeout total del analisis | request_id=%s",
            request_id,
        )
        timeout_web_risk = WebRiskResult.from_timeout_error("timeout total del analisis")
        timeout_ipqs = IpqsResult.from_timeout_error("timeout total del analisis")
        return AnalysisOutcome(
            response=_build_response(timeout_web_risk, timeout_ipqs),
            web_risk=timeout_web_risk,
            ipqs=timeout_ipqs,
        )

    normalized_web_risk = _normalize_web_risk_result(
        web_risk_result,
        request_id=request_id,
    )
    normalized_ipqs = _normalize_ipqs_result(
        ipqs_result,
        request_id=request_id,
    )
    response = _build_response(normalized_web_risk, normalized_ipqs)

    return AnalysisOutcome(
        response=response,
        web_risk=normalized_web_risk,
        ipqs=normalized_ipqs,
    )
