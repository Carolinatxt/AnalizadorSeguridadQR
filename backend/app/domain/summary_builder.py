import logging

from app.services.provider_results import IpqsResult, OpenPhishResult, WebRiskResult

MAX_REASONS = 4
MAX_REASON_LENGTH = 120
_OPENPHISH_EXACT_URL_DANGEROUS_MAX_AGE_DAYS = 30
_OPENPHISH_EXACT_HOST_SUSPICIOUS_MAX_AGE_DAYS = 15
logger = logging.getLogger(__name__)


def build_user_explanation(
    risk_level: str,
    analysis_status: str,
    web_risk: WebRiskResult,
    ipqs: IpqsResult,
    openphish: OpenPhishResult,
) -> tuple[str, list[str]]:
    """Construye textos seguros para usuario final sin decidir el riesgo."""
    summary_type = _get_summary_type(
        risk_level=risk_level,
        analysis_status=analysis_status,
        web_risk=web_risk,
        ipqs=ipqs,
        openphish=openphish,
    )
    summary = _build_summary(
        risk_level=risk_level,
        analysis_status=analysis_status,
        web_risk=web_risk,
        ipqs=ipqs,
        openphish=openphish,
    )
    reasons = _build_reasons(
        risk_level=risk_level,
        analysis_status=analysis_status,
        web_risk=web_risk,
        ipqs=ipqs,
        openphish=openphish,
    )
    normalized_reasons = _normalize_reasons(reasons)
    logger.debug(
        "Resumen de usuario generado | summary_type=%s | signals_for_summary=%s | generated_reasons_count=%s",
        summary_type,
        _collect_signal_labels(web_risk, ipqs, openphish),
        len(normalized_reasons),
    )
    return summary, normalized_reasons


def _get_summary_type(
    risk_level: str,
    analysis_status: str,
    web_risk: WebRiskResult,
    ipqs: IpqsResult,
    openphish: OpenPhishResult,
) -> str:
    if risk_level == "dangerous":
        return "dangerous"
    if analysis_status == "unavailable":
        return "unavailable"
    if analysis_status == "partial":
        return "partial"
    if risk_level == "safe":
        return "safe"
    if _has_visible_signal(web_risk, ipqs, openphish):
        return "suspicious_with_signals"
    return "suspicious_fallback"


def _build_summary(
    risk_level: str,
    analysis_status: str,
    web_risk: WebRiskResult,
    ipqs: IpqsResult,
    openphish: OpenPhishResult,
) -> str:
    if risk_level == "dangerous":
        return "Este enlace se ha clasificado como peligroso por señales graves detectadas durante el análisis."
    if analysis_status == "unavailable":
        return "No fue posible completar el análisis de seguridad."
    if analysis_status == "partial":
        if _has_visible_signal(web_risk, ipqs, openphish):
            return "Este enlace se ha clasificado como sospechoso porque presenta señales que requieren precaución."
        return "No hay información suficiente para clasificar este enlace como seguro."
    if risk_level == "safe":
        return "No se han detectado amenazas conocidas en este enlace."
    if _has_visible_signal(web_risk, ipqs, openphish):
        return "Este enlace se ha clasificado como sospechoso porque presenta señales que requieren precaución."
    return "No hay información suficiente para clasificar este enlace como seguro."


def _has_visible_signal(
    web_risk: WebRiskResult,
    ipqs: IpqsResult,
    openphish: OpenPhishResult,
) -> bool:
    return (
        bool(web_risk.threat_types)
        or openphish.match_found
        or ipqs.phishing is True
        or ipqs.malware is True
        or ipqs.unsafe is True
        or ipqs.suspicious is True
        or (ipqs.spamming is True and ipqs.risk_score is not None and ipqs.risk_score >= 60)
        or ipqs.parking is True
    )


def _collect_signal_labels(
    web_risk: WebRiskResult,
    ipqs: IpqsResult,
    openphish: OpenPhishResult,
) -> list[str]:
    signals: list[str] = []
    if _is_recent_openphish_exact_url_match(openphish):
        signals.append("openphish_exact_url_recent")
    elif _is_stale_or_unknown_openphish_exact_url_match(openphish):
        signals.append("openphish_exact_url_historical")
    elif _is_recent_openphish_exact_host_match(openphish):
        signals.append("openphish_exact_host_recent")
    elif openphish.match_found:
        signals.append("openphish_match")

    if "SOCIAL_ENGINEERING" in web_risk.threat_types or ipqs.phishing is True:
        signals.append("phishing_or_social_engineering")
    if "MALWARE" in web_risk.threat_types or ipqs.malware is True:
        signals.append("malware")
    if "UNWANTED_SOFTWARE" in web_risk.threat_types:
        signals.append("unwanted_software")
    if ipqs.unsafe is True:
        signals.append("unsafe")
    if ipqs.suspicious is True:
        signals.append("suspicious")
    if ipqs.spamming is True:
        signals.append("spamming")
    if ipqs.parking is True:
        signals.append("parking")
    if ipqs.domain_age_human:
        signals.append("domain_age")
    if openphish.is_spear is True:
        signals.append("openphish_spear")
    return signals


def _build_reasons(
    risk_level: str,
    analysis_status: str,
    web_risk: WebRiskResult,
    ipqs: IpqsResult,
    openphish: OpenPhishResult,
) -> list[str]:
    if risk_level == "safe":
        return []
    if analysis_status == "unavailable":
        return [
            "No fue posible obtener una evaluación del enlace.",
            "Este enlace no puede clasificarse como seguro sin análisis completo.",
        ]

    reasons: list[str] = []

    # Prioridad 1: OpenPhish
    reasons.extend(_build_openphish_reasons(openphish))

    # Prioridad 2: Web Risk
    has_phishing_signal = ipqs.phishing is True or "SOCIAL_ENGINEERING" in web_risk.threat_types
    has_malware_signal = ipqs.malware is True or "MALWARE" in web_risk.threat_types
    has_unwanted_software_signal = "UNWANTED_SOFTWARE" in web_risk.threat_types

    if has_phishing_signal:
        reasons.append("Se han detectado indicios de robo de datos o suplantación.")
    if has_malware_signal:
        reasons.append("Se han detectado señales compatibles con software malicioso.")
    if has_unwanted_software_signal:
        reasons.append("El enlace aparece asociado a software no deseado.")

    # Prioridad 3: IPQS
    has_strong_signal = has_phishing_signal or has_malware_signal

    if ipqs.unsafe is True:
        reasons.append("El enlace aparece marcado como inseguro.")
    if ipqs.suspicious is True:
        reasons.append("El enlace presenta señales de comportamiento anómalo.")
    if _should_show_spam_reason(ipqs, reasons):
        reasons.append("El enlace aparece asociado a actividad de spam.")
    if ipqs.parking is True:
        reasons.append("El dominio no parece mostrar un sitio web legítimo activo.")
    if ipqs.domain_age_human and has_strong_signal:
        reasons.append("El dominio es muy reciente, lo que refuerza la sospecha junto con otras señales.")

    if reasons:
        return reasons

    # Prioridad 4: estado técnico
    if analysis_status == "partial":
        return [
            "El análisis no pudo completarse del todo.",
            "No se han reunido señales suficientes para clasificar el enlace como seguro.",
        ]
    if risk_level == "suspicious" and analysis_status == "complete":
        return [
            "El análisis no ha encontrado señales suficientes para clasificar el enlace como seguro.",
        ]

    return reasons


def _build_openphish_reasons(openphish: OpenPhishResult) -> list[str]:
    if not (openphish.available and openphish.match_found):
        return []

    reasons: list[str] = []
    if _is_recent_openphish_exact_url_match(openphish):
        reasons.append("El enlace coincide con una URL identificada como phishing.")
    elif _is_stale_or_unknown_openphish_exact_url_match(openphish):
        if openphish.age_days is None:
            reasons.append("El enlace coincide con una URL identificada previamente como phishing.")
        else:
            reasons.append("Este enlace fue identificado previamente como phishing.")
    elif _is_recent_openphish_exact_host_match(openphish):
        reasons.append("El dominio aparece relacionado con URLs de phishing recientes.")
    elif openphish.match_type == "exact_host":
        reasons.append("El dominio aparece relacionado con URLs de phishing registradas previamente.")

    if openphish.is_spear is True:
        reasons.append("El enlace aparece asociado a una campaña de phishing dirigido.")

    return reasons


def _is_recent_openphish_exact_url_match(openphish: OpenPhishResult) -> bool:
    return (
        openphish.available
        and openphish.match_found
        and openphish.match_type == "exact_url"
        and openphish.age_days is not None
        and openphish.age_days <= _OPENPHISH_EXACT_URL_DANGEROUS_MAX_AGE_DAYS
    )


def _is_stale_or_unknown_openphish_exact_url_match(openphish: OpenPhishResult) -> bool:
    return (
        openphish.available
        and openphish.match_found
        and openphish.match_type == "exact_url"
        and (
            openphish.age_days is None
            or openphish.age_days > _OPENPHISH_EXACT_URL_DANGEROUS_MAX_AGE_DAYS
        )
    )


def _is_recent_openphish_exact_host_match(openphish: OpenPhishResult) -> bool:
    return (
        openphish.available
        and openphish.match_found
        and openphish.match_type == "exact_host"
        and openphish.age_days is not None
        and openphish.age_days <= _OPENPHISH_EXACT_HOST_SUSPICIOUS_MAX_AGE_DAYS
    )


def _should_show_spam_reason(ipqs: IpqsResult, current_reasons: list[str]) -> bool:
    if ipqs.spamming is not True:
        return False
    return bool(current_reasons) or (ipqs.risk_score is not None and ipqs.risk_score >= 60)


def _normalize_reasons(reasons: list[str]) -> list[str]:
    normalized: list[str] = []
    seen: set[str] = set()

    for reason in reasons:
        clean_reason = reason.strip()
        if not clean_reason or len(clean_reason) > MAX_REASON_LENGTH:
            continue
        if clean_reason in seen:
            continue
        seen.add(clean_reason)
        normalized.append(clean_reason)
        if len(normalized) == MAX_REASONS:
            break

    return normalized
