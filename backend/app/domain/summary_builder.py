import logging

from app.domain.local_heuristics import LocalHeuristicResult
from app.services.provider_results import IpqsResult, OpenPhishResult, WebRiskResult

MAX_REASONS = 4
MAX_REASON_LENGTH = 120
_OPENPHISH_EXACT_URL_DANGEROUS_MAX_AGE_DAYS = 30
_OPENPHISH_EXACT_HOST_SUSPICIOUS_MAX_AGE_DAYS = 15
logger = logging.getLogger(__name__)
_GENERIC_OPENPHISH_BRANDS = {
    "generic/spear phishing",
    "crypto/wallet",
    "webmail providers",
}
_LOCAL_HEURISTIC_REASON_BY_CODE = {
    "PUBLIC_IP_HOST": "El enlace usa una direccion IP en lugar de un dominio reconocible.",
    "URL_SHORTENER": "El enlace usa un acortador que oculta el destino real.",
    "EXTREME_HOST_COMPLEXITY": "La direccion es larga o dificil de interpretar.",
    "EMBEDDED_BRAND": "El dominio usa una marca conocida dentro de una direccion que no parece oficial.",
    "LOOKALIKE_BRAND": "El dominio se parece visualmente al de una marca conocida.",
    "SUSPICIOUS_KEYWORD_HOST": "El enlace contiene terminos habituales en paginas de verificacion o inicio de sesion.",
    "SUSPICIOUS_KEYWORD_PATH": "El enlace contiene terminos habituales en paginas de verificacion o inicio de sesion.",
}


def build_user_explanation(
    risk_level: str,
    analysis_status: str,
    web_risk: WebRiskResult,
    ipqs: IpqsResult,
    openphish: OpenPhishResult,
    local_heuristics: LocalHeuristicResult,
) -> tuple[str, list[str]]:
    """Construye textos seguros para usuario final sin decidir el riesgo."""
    summary_type = _get_summary_type(
        risk_level=risk_level,
        analysis_status=analysis_status,
        web_risk=web_risk,
        ipqs=ipqs,
        openphish=openphish,
        local_heuristics=local_heuristics,
    )
    summary = _build_summary(
        risk_level=risk_level,
        analysis_status=analysis_status,
        web_risk=web_risk,
        ipqs=ipqs,
        openphish=openphish,
        local_heuristics=local_heuristics,
    )
    reasons = _build_reasons(
        risk_level=risk_level,
        analysis_status=analysis_status,
        web_risk=web_risk,
        ipqs=ipqs,
        openphish=openphish,
        local_heuristics=local_heuristics,
    )
    normalized_reasons = _normalize_reasons(reasons)
    logger.debug(
        "Resumen de usuario generado | summary_type=%s | signals_for_summary=%s | generated_reasons_count=%s",
        summary_type,
        _collect_signal_labels(web_risk, ipqs, openphish, local_heuristics),
        len(normalized_reasons),
    )
    return summary, normalized_reasons


def _get_summary_type(
    risk_level: str,
    analysis_status: str,
    web_risk: WebRiskResult,
    ipqs: IpqsResult,
    openphish: OpenPhishResult,
    local_heuristics: LocalHeuristicResult,
) -> str:
    if risk_level == "dangerous":
        return "dangerous"
    if analysis_status == "unavailable":
        return "unavailable"
    if analysis_status == "partial":
        if _has_visible_signal(web_risk, ipqs, openphish, local_heuristics):
            return "partial_with_signals"
        return "partial"
    if risk_level == "safe":
        return "safe"
    if _has_visible_signal(web_risk, ipqs, openphish, local_heuristics):
        return "suspicious_with_signals"
    return "suspicious_fallback"


def _build_summary(
    risk_level: str,
    analysis_status: str,
    web_risk: WebRiskResult,
    ipqs: IpqsResult,
    openphish: OpenPhishResult,
    local_heuristics: LocalHeuristicResult,
) -> str:
    if risk_level == "dangerous":
        return "Este enlace se ha clasificado como peligroso por senales graves detectadas durante el analisis."
    if analysis_status == "unavailable":
        return "No fue posible completar el analisis de seguridad."
    if analysis_status == "partial":
        if _has_visible_signal(web_risk, ipqs, openphish, local_heuristics):
            return "Este enlace se ha clasificado como sospechoso porque presenta senales que requieren precaucion."
        return "No hay informacion suficiente para clasificar este enlace como seguro."
    if risk_level == "safe":
        return "No se han detectado amenazas conocidas en este enlace."
    if _has_visible_signal(web_risk, ipqs, openphish, local_heuristics):
        return "Este enlace se ha clasificado como sospechoso porque presenta senales que requieren precaucion."
    return "No hay informacion suficiente para clasificar este enlace como seguro."


def _has_visible_signal(
    web_risk: WebRiskResult,
    ipqs: IpqsResult,
    openphish: OpenPhishResult,
    local_heuristics: LocalHeuristicResult,
) -> bool:
    return (
        bool(web_risk.threat_types)
        or openphish.match_found
        or bool(local_heuristics.signals)
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
    local_heuristics: LocalHeuristicResult,
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

    if "SOCIAL_ENGINEERING" in web_risk.threat_types:
        signals.append("web_risk_social_engineering")
    if "MALWARE" in web_risk.threat_types:
        signals.append("web_risk_malware")
    if "UNWANTED_SOFTWARE" in web_risk.threat_types:
        signals.append("web_risk_unwanted_software")
    if ipqs.phishing is True:
        signals.append("ipqs_phishing")
    if ipqs.malware is True:
        signals.append("ipqs_malware")
    if ipqs.unsafe is True:
        signals.append("ipqs_unsafe")
    if ipqs.suspicious is True:
        signals.append("ipqs_suspicious")
    if ipqs.spamming is True:
        signals.append("ipqs_spamming")
    if ipqs.parking is True:
        signals.append("ipqs_parking")
    if ipqs.domain_age_human:
        signals.append("ipqs_domain_age")
    if openphish.is_spear is True:
        signals.append("openphish_spear")
    if local_heuristics.available:
        signals.extend(
            f"local_{signal.code.lower()}" for signal in local_heuristics.signals
        )
    return signals


def _build_reasons(
    risk_level: str,
    analysis_status: str,
    web_risk: WebRiskResult,
    ipqs: IpqsResult,
    openphish: OpenPhishResult,
    local_heuristics: LocalHeuristicResult,
) -> list[str]:
    if risk_level == "safe":
        return []
    if analysis_status == "unavailable":
        return [
            "No fue posible obtener una evaluacion del enlace.",
            "Este enlace no puede clasificarse como seguro sin analisis completo.",
        ]

    reasons: list[str] = []

    # Prioridad 1: OpenPhish
    reasons.extend(_build_openphish_reasons(openphish))

    # Prioridad 2: Web Risk
    has_web_risk_phishing_signal = "SOCIAL_ENGINEERING" in web_risk.threat_types
    has_web_risk_malware_signal = "MALWARE" in web_risk.threat_types
    has_unwanted_software_signal = "UNWANTED_SOFTWARE" in web_risk.threat_types

    if has_web_risk_phishing_signal:
        reasons.append("Se han detectado indicios de robo de datos o suplantacion.")
    if has_web_risk_malware_signal:
        reasons.append("Se han detectado senales compatibles con software malicioso.")
    if has_unwanted_software_signal:
        reasons.append("El enlace aparece asociado a software no deseado.")

    # Prioridad 3: IPQS senales fuertes
    has_ipqs_phishing_signal = ipqs.phishing is True
    has_ipqs_malware_signal = ipqs.malware is True
    has_strong_signal = (
        has_web_risk_phishing_signal
        or has_web_risk_malware_signal
        or has_ipqs_phishing_signal
        or has_ipqs_malware_signal
    )

    if has_ipqs_phishing_signal:
        reasons.append("Se han detectado indicios de robo de datos o suplantacion.")
    if has_ipqs_malware_signal:
        reasons.append("Se han detectado senales compatibles con software malicioso.")
    if ipqs.unsafe is True:
        reasons.append("El enlace aparece marcado como inseguro.")

    # Prioridad 4: heuristicas locales fuertes/medias
    reasons.extend(_build_local_heuristic_reasons(local_heuristics))

    # Prioridad 5: IPQS senales medias
    if ipqs.suspicious is True:
        reasons.append("El enlace presenta senales de comportamiento anomalo.")
    if _should_show_spam_reason(ipqs, reasons):
        reasons.append("El enlace aparece asociado a actividad de spam.")
    if ipqs.parking is True:
        reasons.append("El dominio no parece mostrar un sitio web legitimo activo.")
    if ipqs.domain_age_human and has_strong_signal:
        reasons.append("El dominio es muy reciente, lo que refuerza la sospecha junto con otras senales.")

    if reasons:
        return reasons

    # Prioridad 6: estado tecnico
    if analysis_status == "partial":
        return [
            "El analisis no pudo completarse del todo.",
            "No se han reunido senales suficientes para clasificar el enlace como seguro.",
        ]
    if risk_level == "suspicious" and analysis_status == "complete":
        return [
            "El analisis no ha encontrado senales suficientes para clasificar el enlace como seguro.",
        ]

    return reasons


def _build_local_heuristic_reasons(local_heuristics: LocalHeuristicResult) -> list[str]:
    if not local_heuristics.available:
        return []

    return [
        _LOCAL_HEURISTIC_REASON_BY_CODE[signal.code]
        for signal in local_heuristics.signals
        if signal.code in _LOCAL_HEURISTIC_REASON_BY_CODE
    ]


def _build_openphish_reasons(openphish: OpenPhishResult) -> list[str]:
    if not (openphish.available and openphish.match_found):
        return []

    reasons: list[str] = []
    display_brand = _sanitize_brand_for_display(openphish.brand)
    if _is_recent_openphish_exact_url_match(openphish):
        if display_brand is not None:
            reasons.append(
                f"El enlace coincide con una URL de phishing asociada a {display_brand}."
            )
        else:
            reasons.append("El enlace coincide con una URL identificada como phishing.")
    elif _is_stale_or_unknown_openphish_exact_url_match(openphish):
        if display_brand is not None:
            reasons.append(
                f"Este enlace fue identificado previamente como phishing asociado a {display_brand}."
            )
        elif openphish.age_days is None:
            reasons.append("El enlace coincide con una URL identificada previamente como phishing.")
        else:
            reasons.append("Este enlace fue identificado previamente como phishing.")
    elif _is_recent_openphish_exact_host_match(openphish):
        if display_brand is not None:
            reasons.append(
                f"El dominio aparece relacionado con phishing reciente asociado a {display_brand}."
            )
        else:
            reasons.append("El dominio aparece relacionado con URLs de phishing recientes.")

    if openphish.is_spear is True:
        reasons.append("El enlace aparece asociado a una campana de phishing dirigido.")

    return reasons


def _sanitize_brand_for_display(brand: str | None) -> str | None:
    if brand is None:
        return None

    clean_brand = brand.strip()
    if not clean_brand:
        return None

    normalized_brand = clean_brand.lower()
    if normalized_brand in _GENERIC_OPENPHISH_BRANDS:
        return None
    if normalized_brand.startswith("generic"):
        return None
    if len(clean_brand) > 40:
        return None

    return clean_brand


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
