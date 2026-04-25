import asyncio
import logging
from dataclasses import dataclass

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


def _normalize_web_risk_result(result: WebRiskResult | Exception) -> WebRiskResult:
    if isinstance(result, Exception):
        logger.error(
            "Excepcion no capturada en Web Risk",
            exc_info=result,
        )
        return WebRiskResult.from_internal_error("excepcion interna")
    return result


def _normalize_ipqs_result(result: IpqsResult | Exception) -> IpqsResult:
    if isinstance(result, Exception):
        logger.error(
            "Excepcion no capturada en IPQS",
            exc_info=result,
        )
        return IpqsResult.from_internal_error("excepcion interna")
    return result


async def analyze_url_with_providers(url: str) -> AnalysisOutcome:
    web_risk_result, ipqs_result = await asyncio.gather(
        check_url_with_web_risk(url),
        check_url_with_ipqs(url),
        return_exceptions=True,
    )

    normalized_web_risk = _normalize_web_risk_result(web_risk_result)
    normalized_ipqs = _normalize_ipqs_result(ipqs_result)
    response = _build_response(normalized_web_risk, normalized_ipqs)

    return AnalysisOutcome(
        response=response,
        web_risk=normalized_web_risk,
        ipqs=normalized_ipqs,
    )
