import asyncio
import logging
from dataclasses import dataclass

from app.core.config import ANALYSIS_TOTAL_TIMEOUT_SECONDS
from app.domain.analysis_rules import build_response
from app.domain.local_heuristics import LocalHeuristicResult, analyze_local_heuristics
from app.models.schemas import AnalyzeUrlResponse
from app.services.ipqs_service import check_url_with_ipqs
from app.services.openphish_service import check_url_with_openphish
from app.services.provider_results import IpqsResult, OpenPhishResult, WebRiskResult
from app.services.web_risk_service import check_url_with_web_risk

logger = logging.getLogger(__name__)
_TIMEOUT_SUMMARY = "timeout total del analisis"


@dataclass(frozen=True)
class AnalysisOutcome:
    response: AnalyzeUrlResponse
    web_risk: WebRiskResult
    ipqs: IpqsResult
    openphish: OpenPhishResult
    local_heuristics: LocalHeuristicResult


def _normalize_web_risk_result(
    result: WebRiskResult | BaseException,
    request_id: str | None = None,
) -> WebRiskResult:
    if isinstance(result, Exception):
        logger.error(
            "Excepcion no capturada en Web Risk | request_id=%s",
            request_id,
            exc_info=result,
        )
        return WebRiskResult.from_internal_error("excepcion interna")
    if isinstance(result, BaseException):
        raise result
    return result


def _normalize_ipqs_result(
    result: IpqsResult | BaseException,
    request_id: str | None = None,
) -> IpqsResult:
    if isinstance(result, Exception):
        logger.error(
            "Excepcion no capturada en IPQS | request_id=%s",
            request_id,
            exc_info=result,
        )
        return IpqsResult.from_internal_error("excepcion interna")
    if isinstance(result, BaseException):
        raise result
    return result


def _normalize_openphish_result(
    result: OpenPhishResult | BaseException,
    request_id: str | None = None,
) -> OpenPhishResult:
    if isinstance(result, Exception):
        logger.error(
            "Excepcion no capturada en OpenPhish | request_id=%s",
            request_id,
            exc_info=result,
        )
        return OpenPhishResult.from_internal_error("excepcion interna")
    if isinstance(result, BaseException):
        raise result
    return result


def _safe_analyze_local_heuristics(
    url: str,
    request_id: str | None = None,
) -> LocalHeuristicResult:
    try:
        return analyze_local_heuristics(url)
    except Exception as exc:  # pragma: no cover - ruta defensiva
        logger.error(
            "Excepcion no capturada en heuristicas locales | request_id=%s",
            request_id,
            exc_info=exc,
        )
        return LocalHeuristicResult(available=False)


async def analyze_url_with_providers(
    url: str,
    request_id: str | None = None,
) -> AnalysisOutcome:
    local_heuristics = _safe_analyze_local_heuristics(url, request_id=request_id)

    try:
        web_risk_result, ipqs_result, openphish_result = await asyncio.wait_for(
            asyncio.gather(
                check_url_with_web_risk(url, request_id=request_id),
                check_url_with_ipqs(url, request_id=request_id),
                # SQLite es sincronico; lo movemos a un hilo para no bloquear
                # el event loop del backend mientras consultamos OpenPhish.
                asyncio.to_thread(check_url_with_openphish, url, request_id),
                return_exceptions=True,
            ),
            timeout=ANALYSIS_TOTAL_TIMEOUT_SECONDS,
        )
    except asyncio.TimeoutError:
        logger.error(
            "Timeout total del analisis | request_id=%s",
            request_id,
        )
        timeout_web_risk = WebRiskResult.from_timeout_error(_TIMEOUT_SUMMARY)
        timeout_ipqs = IpqsResult.from_timeout_error(_TIMEOUT_SUMMARY)
        # OpenPhish no expone estado timeout especifico; degradamos a
        # internal_error para no convertirlo en dependencia critica.
        timeout_openphish = OpenPhishResult.from_internal_error(_TIMEOUT_SUMMARY)
        return AnalysisOutcome(
            response=build_response(
                timeout_web_risk,
                timeout_ipqs,
                timeout_openphish,
                local_heuristics,
            ),
            web_risk=timeout_web_risk,
            ipqs=timeout_ipqs,
            openphish=timeout_openphish,
            local_heuristics=local_heuristics,
        )

    normalized_web_risk = _normalize_web_risk_result(
        web_risk_result,
        request_id=request_id,
    )
    normalized_ipqs = _normalize_ipqs_result(
        ipqs_result,
        request_id=request_id,
    )
    normalized_openphish = _normalize_openphish_result(
        openphish_result,
        request_id=request_id,
    )
    response = build_response(
        normalized_web_risk,
        normalized_ipqs,
        normalized_openphish,
        local_heuristics,
    )

    return AnalysisOutcome(
        response=response,
        web_risk=normalized_web_risk,
        ipqs=normalized_ipqs,
        openphish=normalized_openphish,
        local_heuristics=local_heuristics,
    )
