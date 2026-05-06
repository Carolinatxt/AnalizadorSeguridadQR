from typing import Literal

from app.domain.local_heuristics import LocalHeuristicResult
from app.domain.summary_builder import build_user_explanation
from app.models.schemas import AnalyzeUrlResponse
from app.services.provider_results import IpqsResult, OpenPhishResult, WebRiskResult

AnalysisStatus = Literal["complete", "partial", "unavailable"]
DangerousSource = Literal[
    "openphish_exact_url",
    "web_risk_malware",
    "web_risk_social_engineering",
    "ipqs_phishing_high_score",
    "ipqs_malware_high_score",
]
_IPQS_DANGEROUS_SCORE_THRESHOLD: int = 85
_IPQS_SAFE_SCORE_THRESHOLD: int = 60
_OPENPHISH_EXACT_URL_DANGEROUS_MAX_AGE_DAYS: int = 30
_OPENPHISH_EXACT_HOST_SUSPICIOUS_MAX_AGE_DAYS: int = 15
_LOCAL_HEURISTIC_DIRECT_BLOCK_CODES = frozenset(
    {
        "PUBLIC_IP_HOST",
        "URL_SHORTENER",
        "EMBEDDED_BRAND",
        "LOOKALIKE_BRAND",
        "EXTREME_HOST_COMPLEXITY",
    }
)
_LOCAL_HEURISTIC_KEYWORD_HOST_CODE = "SUSPICIOUS_KEYWORD_HOST"
_LOCAL_HEURISTIC_KEYWORD_PATH_CODE = "SUSPICIOUS_KEYWORD_PATH"


def compute_analysis_status(
    web_risk: WebRiskResult,
    ipqs: IpqsResult,
    openphish: OpenPhishResult,
) -> AnalysisStatus:
    if web_risk.available and ipqs.available:
        return "complete"
    if web_risk.available != ipqs.available:
        return "partial"
    if openphish.available and openphish.match_found:
        return "partial"
    return "unavailable"


def _is_openphish_exact_url_match(openphish: OpenPhishResult) -> bool:
    return openphish.available and openphish.match_found and openphish.match_type == "exact_url"


def _is_openphish_exact_host_match(openphish: OpenPhishResult) -> bool:
    return openphish.available and openphish.match_found and openphish.match_type == "exact_host"


def _is_recent_openphish_exact_url_match(openphish: OpenPhishResult) -> bool:
    return (
        _is_openphish_exact_url_match(openphish)
        and openphish.age_days is not None
        and openphish.age_days <= _OPENPHISH_EXACT_URL_DANGEROUS_MAX_AGE_DAYS
    )


def _is_stale_or_unknown_openphish_exact_url_match(openphish: OpenPhishResult) -> bool:
    return _is_openphish_exact_url_match(openphish) and (
        openphish.age_days is None
        or openphish.age_days > _OPENPHISH_EXACT_URL_DANGEROUS_MAX_AGE_DAYS
    )


def _is_recent_openphish_exact_host_match(openphish: OpenPhishResult) -> bool:
    return (
        _is_openphish_exact_host_match(openphish)
        and openphish.age_days is not None
        and openphish.age_days <= _OPENPHISH_EXACT_HOST_SUSPICIOUS_MAX_AGE_DAYS
    )


def _openphish_blocks_safe(openphish: OpenPhishResult) -> bool:
    # Regla formal de producto:
    # si OpenPhish esta disponible y encuentra cualquier coincidencia,
    # safe queda bloqueado aunque esa coincidencia no eleve por si sola
    # a dangerous. Si no hay dangerous, el flujo cae a suspicious.
    return openphish.available and openphish.match_found


def _local_heuristics_blocks_safe(local_heuristics: LocalHeuristicResult) -> bool:
    if not local_heuristics.available:
        return False

    signal_codes = {signal.code for signal in local_heuristics.signals}
    if signal_codes & _LOCAL_HEURISTIC_DIRECT_BLOCK_CODES:
        return True

    # La keyword en host solo bloquea safe si convive con otra senal
    # heuristica relevante. La keyword en path nunca bloquea por si sola.
    if _LOCAL_HEURISTIC_KEYWORD_HOST_CODE not in signal_codes:
        return False

    concurrent_relevant_codes = (
        signal_codes
        - {_LOCAL_HEURISTIC_KEYWORD_HOST_CODE, _LOCAL_HEURISTIC_KEYWORD_PATH_CODE}
    )
    return bool(concurrent_relevant_codes & _LOCAL_HEURISTIC_DIRECT_BLOCK_CODES)


def is_dangerous(
    web_risk: WebRiskResult,
    ipqs: IpqsResult,
    openphish: OpenPhishResult,
) -> bool:
    # UNWANTED_SOFTWARE no eleva directamente a dangerous: se trata como
    # señal de riesgo medio que bloquea safe y mantiene clasificación conservadora.
    web_risk_has_malware = "MALWARE" in web_risk.threat_types
    web_risk_has_social_engineering = "SOCIAL_ENGINEERING" in web_risk.threat_types
    openphish_dangerous = _is_recent_openphish_exact_url_match(openphish)
    ipqs_dangerous = (
        (ipqs.phishing is True or ipqs.malware is True)
        and ipqs.risk_score is not None
        and ipqs.risk_score >= _IPQS_DANGEROUS_SCORE_THRESHOLD
    )

    return (
        openphish_dangerous
        or web_risk_has_malware
        or web_risk_has_social_engineering
        or ipqs_dangerous
    )


def get_dangerous_source(
    web_risk: WebRiskResult,
    ipqs: IpqsResult,
    openphish: OpenPhishResult,
) -> DangerousSource | None:
    if _is_recent_openphish_exact_url_match(openphish):
        return "openphish_exact_url"
    if "MALWARE" in web_risk.threat_types:
        return "web_risk_malware"
    if "SOCIAL_ENGINEERING" in web_risk.threat_types:
        return "web_risk_social_engineering"
    if (
        ipqs.phishing is True
        and ipqs.risk_score is not None
        and ipqs.risk_score >= _IPQS_DANGEROUS_SCORE_THRESHOLD
    ):
        return "ipqs_phishing_high_score"
    if (
        ipqs.malware is True
        and ipqs.risk_score is not None
        and ipqs.risk_score >= _IPQS_DANGEROUS_SCORE_THRESHOLD
    ):
        return "ipqs_malware_high_score"
    return None


def is_safe(
    web_risk: WebRiskResult,
    ipqs: IpqsResult,
    openphish: OpenPhishResult,
    local_heuristics: LocalHeuristicResult,
) -> bool:
    web_risk_has_any_threat = len(web_risk.threat_types) > 0
    return (
        web_risk.available
        and ipqs.available
        and (not _openphish_blocks_safe(openphish))
        and (not _local_heuristics_blocks_safe(local_heuristics))
        and (not web_risk_has_any_threat)
        and ipqs.success
        and ipqs.risk_score is not None
        and ipqs.risk_score < _IPQS_SAFE_SCORE_THRESHOLD
        and ipqs.phishing is False
        and ipqs.malware is False
        and ipqs.suspicious is False
        and ipqs.unsafe is False
    )


def build_response(
    web_risk: WebRiskResult,
    ipqs: IpqsResult,
    openphish: OpenPhishResult,
    local_heuristics: LocalHeuristicResult,
) -> AnalyzeUrlResponse:
    analysis_status = compute_analysis_status(web_risk, ipqs, openphish)
    if is_dangerous(web_risk, ipqs, openphish):
        risk_level = "dangerous"
    elif is_safe(web_risk, ipqs, openphish, local_heuristics):
        risk_level = "safe"
    else:
        # Fallback deliberado a suspicious:
        # incluye señales de OpenPhish no concluyentes, Web Risk con
        # UNWANTED_SOFTWARE, señales medias de IPQS y estados partial/unavailable.
        # Evita falsos "safe" cuando hay incertidumbre operativa o señales ambiguas.
        risk_level = "suspicious"

    summary, reasons = build_user_explanation(
        risk_level=risk_level,
        analysis_status=analysis_status,
        web_risk=web_risk,
        ipqs=ipqs,
        openphish=openphish,
        local_heuristics=local_heuristics,
    )
    return AnalyzeUrlResponse(
        risk_level=risk_level,
        analysis_status=analysis_status,
        summary=summary,
        reasons=reasons,
    )
