from typing import Literal

from app.models.schemas import AnalyzeUrlResponse
from app.services.provider_results import IpqsResult, WebRiskResult

AnalysisStatus = Literal["complete", "partial", "unavailable"]
_IPQS_DANGEROUS_SCORE_THRESHOLD: int = 85
_IPQS_SAFE_SCORE_THRESHOLD: int = 60

def _compute_analysis_status(
    web_risk: WebRiskResult,
    ipqs: IpqsResult,
) -> AnalysisStatus:
    if (not web_risk.available) and (not ipqs.available):
        return "unavailable"
    if web_risk.available != ipqs.available:
        return "partial"
    return "complete"


def _is_dangerous(web_risk: WebRiskResult, ipqs: IpqsResult) -> bool:
    # UNWANTED_SOFTWARE no eleva directamente a dangerous: se trata como
    # senal de riesgo medio que bloquea safe y mantiene clasificacion conservadora.
    web_risk_has_malware = "MALWARE" in web_risk.threat_types
    web_risk_has_social_engineering = "SOCIAL_ENGINEERING" in web_risk.threat_types
    ipqs_dangerous = (
        (ipqs.phishing or ipqs.malware)
        and ipqs.risk_score is not None
        and ipqs.risk_score >= _IPQS_DANGEROUS_SCORE_THRESHOLD
    )

    return web_risk_has_malware or web_risk_has_social_engineering or ipqs_dangerous


def _is_safe(web_risk: WebRiskResult, ipqs: IpqsResult) -> bool:
    web_risk_has_any_threat = len(web_risk.threat_types) > 0
    return (
        web_risk.available
        and ipqs.available
        and (not web_risk_has_any_threat)
        and ipqs.success
        and ipqs.risk_score is not None
        and ipqs.risk_score < _IPQS_SAFE_SCORE_THRESHOLD
        and (not ipqs.phishing)
        and (not ipqs.malware)
        and (not ipqs.suspicious)
        and (not ipqs.unsafe)
    )


def _build_response(web_risk: WebRiskResult, ipqs: IpqsResult) -> AnalyzeUrlResponse:
    analysis_status = _compute_analysis_status(web_risk, ipqs)
    if analysis_status == "unavailable":
        return AnalyzeUrlResponse(
            risk_level="suspicious",
            analysis_status="unavailable",
            summary="No fue posible completar el análisis. Inténtalo de nuevo.",
        )

    if _is_dangerous(web_risk, ipqs):
        return AnalyzeUrlResponse(
            risk_level="dangerous",
            analysis_status=analysis_status,
            summary="Se detectaron señales de riesgo alto en esta URL.",
        )
    if _is_safe(web_risk, ipqs):
        return AnalyzeUrlResponse(
            risk_level="safe",
            analysis_status=analysis_status,
            summary="No se detectaron señales de riesgo en esta URL.",
        )
    # Fallback deliberado a suspicious:
    # riesgo medio en escenario de analisis parcial o evidencia insuficiente.
    # Evita falsos "safe" cuando hay incertidumbre operativa o senales ambiguas.
    return AnalyzeUrlResponse(
        risk_level="suspicious",
        analysis_status=analysis_status,
        summary="Se detectaron señales de riesgo potencial o evidencia insuficiente.",
    )
