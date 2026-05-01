from typing import Literal

from app.domain.summary_builder import build_user_explanation
from app.models.schemas import AnalyzeUrlResponse
from app.services.provider_results import IpqsResult, WebRiskResult

AnalysisStatus = Literal["complete", "partial", "unavailable"]
_IPQS_DANGEROUS_SCORE_THRESHOLD: int = 85
_IPQS_SAFE_SCORE_THRESHOLD: int = 60

def compute_analysis_status(
    web_risk: WebRiskResult,
    ipqs: IpqsResult,
) -> AnalysisStatus:
    if (not web_risk.available) and (not ipqs.available):
        return "unavailable"
    if web_risk.available != ipqs.available:
        return "partial"
    return "complete"


def is_dangerous(web_risk: WebRiskResult, ipqs: IpqsResult) -> bool:
    # UNWANTED_SOFTWARE no eleva directamente a dangerous: se trata como
    # señal de riesgo medio que bloquea safe y mantiene clasificación conservadora.
    web_risk_has_malware = "MALWARE" in web_risk.threat_types
    web_risk_has_social_engineering = "SOCIAL_ENGINEERING" in web_risk.threat_types
    ipqs_dangerous = (
        (ipqs.phishing is True or ipqs.malware is True)
        and ipqs.risk_score is not None
        and ipqs.risk_score >= _IPQS_DANGEROUS_SCORE_THRESHOLD
    )

    return web_risk_has_malware or web_risk_has_social_engineering or ipqs_dangerous


def is_safe(web_risk: WebRiskResult, ipqs: IpqsResult) -> bool:
    web_risk_has_any_threat = len(web_risk.threat_types) > 0
    return (
        web_risk.available
        and ipqs.available
        and (not web_risk_has_any_threat)
        and ipqs.success
        and ipqs.risk_score is not None
        and ipqs.risk_score < _IPQS_SAFE_SCORE_THRESHOLD
        and ipqs.phishing is False
        and ipqs.malware is False
        and ipqs.suspicious is False
        and ipqs.unsafe is False
    )


def build_response(web_risk: WebRiskResult, ipqs: IpqsResult) -> AnalyzeUrlResponse:
    analysis_status = compute_analysis_status(web_risk, ipqs)
    if analysis_status == "unavailable":
        risk_level = "suspicious"
    elif is_dangerous(web_risk, ipqs):
        risk_level = "dangerous"
    elif is_safe(web_risk, ipqs):
        risk_level = "safe"
    else:
        # Fallback deliberado a suspicious:
        # riesgo medio en escenario de análisis parcial o evidencia insuficiente.
        # Evita falsos "safe" cuando hay incertidumbre operativa o señales ambiguas.
        risk_level = "suspicious"

    summary, reasons = build_user_explanation(
        risk_level=risk_level,
        analysis_status=analysis_status,
        web_risk=web_risk,
        ipqs=ipqs,
    )
    return AnalyzeUrlResponse(
        risk_level=risk_level,
        analysis_status=analysis_status,
        summary=summary,
        reasons=reasons,
    )
