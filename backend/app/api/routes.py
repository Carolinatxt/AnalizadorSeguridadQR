import asyncio
import logging
from urllib.parse import urlparse

from fastapi import APIRouter

from app.models.schemas import AnalyzeUrlRequest, AnalyzeUrlResponse
from app.services.ipqs_service import check_url_with_ipqs
from app.services.web_risk_service import check_url_with_web_risk

router = APIRouter()
logger = logging.getLogger(__name__)


def _web_risk_exception_result() -> dict:
    return {
        "provider_status": "error",
        "available": False,
        "match_found": False,
        "threat_types": [],
        "raw_summary": "excepcion interna",
    }


def _ipqs_exception_result() -> dict:
    return {
        "provider_status": "error",
        "available": False,
        "success": False,
        "risk_score": None,
        "phishing": False,
        "malware": False,
        "suspicious": False,
        "unsafe": False,
        "raw_summary": "excepcion interna",
    }


@router.post("/api/v1/analyze", response_model=AnalyzeUrlResponse)
async def analyze_url(payload: AnalyzeUrlRequest) -> AnalyzeUrlResponse:
    # Llega validada por AnalyzeUrlRequest; aqui preservamos solo log funcional.
    url = payload.url
    logger.info("Nueva peticion de analisis recibida en /api/v1/analyze")

    parsed = urlparse(url)

    # Logging de desarrollo: registramos solo esquema y host para no exponer la URL completa.
    logger.info(
        "URL valida para analisis | scheme=%s | host=%s",
        parsed.scheme,
        parsed.hostname,
    )

    web_risk_result, ipqs_result = await asyncio.gather(
        check_url_with_web_risk(url),
        check_url_with_ipqs(url),
        return_exceptions=True,
    )
    if isinstance(web_risk_result, Exception):
        logger.error(
            "Excepcion no capturada en Web Risk",
            exc_info=web_risk_result,
        )
        web_risk_result = _web_risk_exception_result()
    if isinstance(ipqs_result, Exception):
        logger.error(
            "Excepcion no capturada en IPQS",
            exc_info=ipqs_result,
        )
        ipqs_result = _ipqs_exception_result()

    logger.info(
        "Resultado Web Risk | provider_status=%s | available=%s | match_found=%s | threat_types=%s",
        web_risk_result["provider_status"],
        web_risk_result["available"],
        web_risk_result["match_found"],
        web_risk_result["threat_types"],
    )
    logger.info(
        "Resultado IPQS | provider_status=%s | available=%s | success=%s | risk_score=%s | phishing=%s | malware=%s | suspicious=%s | unsafe=%s",
        ipqs_result["provider_status"],
        ipqs_result["available"],
        ipqs_result["success"],
        ipqs_result["risk_score"],
        ipqs_result["phishing"],
        ipqs_result["malware"],
        ipqs_result["suspicious"],
        ipqs_result["unsafe"],
    )

    # Estado tecnico del analisis.
    web_risk_available = web_risk_result["available"]
    ipqs_available = ipqs_result["available"]
    analysis_unavailable = (not web_risk_available) and (not ipqs_available)
    analysis_partial = web_risk_available != ipqs_available

    # Señales Web Risk.
    threat_types = set(web_risk_result["threat_types"])
    web_risk_has_malware = "MALWARE" in threat_types
    web_risk_has_social_engineering = "SOCIAL_ENGINEERING" in threat_types
    web_risk_has_any_threat = len(threat_types) > 0

    # Señales IPQS.
    ipqs_success = ipqs_result["success"]
    ipqs_risk_score = ipqs_result["risk_score"]
    ipqs_phishing = ipqs_result["phishing"]
    ipqs_malware = ipqs_result["malware"]
    ipqs_suspicious = ipqs_result["suspicious"]
    ipqs_unsafe = ipqs_result["unsafe"]

    ipqs_dangerous = (
        (ipqs_phishing or ipqs_malware)
        and ipqs_risk_score is not None
        and ipqs_risk_score >= 85
    )

    # Regla V0 de peligro: basta una evidencia fuerte.
    is_dangerous = (
        web_risk_has_malware
        or web_risk_has_social_engineering
        or ipqs_dangerous
    )

    # Regla V0 de seguro: deben cumplirse todas las condiciones.
    is_safe = (
        web_risk_available
        and ipqs_available
        and (not web_risk_has_any_threat)
        and ipqs_success
        and ipqs_risk_score is not None
        and ipqs_risk_score < 60
        and (not ipqs_phishing)
        and (not ipqs_malware)
        and (not ipqs_suspicious)
        and (not ipqs_unsafe)
    )

    response: AnalyzeUrlResponse
    if analysis_unavailable:
        response = AnalyzeUrlResponse(
            risk_level="suspicious",
            analysis_status="unavailable",
            summary="No fue posible completar el análisis. Inténtalo de nuevo.",
        )
        logger.warning("Analisis unavailable: Web Risk e IPQS no disponibles")
    else:
        analysis_status = "partial" if analysis_partial else "complete"
        if analysis_partial:
            logger.warning(
                "Analisis partial: una API externa no estuvo disponible"
            )

        if is_dangerous:
            response = AnalyzeUrlResponse(
                risk_level="dangerous",
                analysis_status=analysis_status,
                summary="Se detectaron señales de riesgo alto en esta URL.",
            )
        elif is_safe:
            response = AnalyzeUrlResponse(
                risk_level="safe",
                analysis_status=analysis_status,
                summary="No se detectaron señales de riesgo en esta URL.",
            )
        else:
            response = AnalyzeUrlResponse(
                risk_level="suspicious",
                analysis_status=analysis_status,
                summary="Se detectaron señales de riesgo potencial o evidencia insuficiente.",
            )

    logger.info(
        "Resultado de analisis | risk_level=%s | analysis_status=%s",
        response.risk_level,
        response.analysis_status,
    )

    return response

