import logging
from urllib.parse import quote, urlsplit, urlunsplit

import httpx

from app.core.config import IPQS_API_KEY

logger = logging.getLogger(__name__)


def remove_url_fragment(url: str) -> str:
    # Mantenemos el mismo criterio de V0 que en Web Risk: ignorar fragmentos (#...).
    parsed = urlsplit(url)
    return urlunsplit((parsed.scheme, parsed.netloc, parsed.path, parsed.query, ""))


def _to_bool(value: object) -> bool:
    if isinstance(value, bool):
        return value
    if isinstance(value, str):
        return value.strip().lower() in {"1", "true", "yes"}
    if isinstance(value, (int, float)):
        return value != 0
    return False


def _to_int_or_none(value: object) -> int | None:
    try:
        return int(value) if value is not None else None
    except (TypeError, ValueError):
        return None


async def check_url_with_ipqs(url: str) -> dict:
    if not IPQS_API_KEY:
        logger.warning("IPQS_API_KEY no configurada; no se puede consultar IPQS")
        return {
            "provider_status": "error",
            "available": False,
            "success": False,
            "risk_score": None,
            "phishing": False,
            "malware": False,
            "suspicious": False,
            "unsafe": False,
            "raw_summary": "IPQS_API_KEY no configurada",
        }

    analysis_url = remove_url_fragment(url)
    encoded_url = quote(analysis_url, safe="")
    host = urlsplit(analysis_url).hostname

    logger.info("Consultando IPQS | host=%s", host)

    endpoint = "https://ipqualityscore.com/api/json/url"
    request_url = f"{endpoint}/{IPQS_API_KEY}/{encoded_url}"

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(request_url)

        if response.status_code != 200:
            logger.error(
                "Error HTTP al consultar IPQS | status_code=%s",
                response.status_code,
            )
            return {
                "provider_status": "error",
                "available": False,
                "success": False,
                "risk_score": None,
                "phishing": False,
                "malware": False,
                "suspicious": False,
                "unsafe": False,
                "raw_summary": f"IPQS HTTP {response.status_code}",
            }

        data = response.json()
        success = _to_bool(data.get("success"))
        risk_score = _to_int_or_none(data.get("risk_score"))
        phishing = _to_bool(data.get("phishing"))
        malware = _to_bool(data.get("malware"))
        suspicious = _to_bool(data.get("suspicious"))
        unsafe = _to_bool(data.get("unsafe"))

        if not success:
            logger.warning("IPQS respondio success=false | host=%s", host)
            return {
                "provider_status": "api_error",
                "available": False,
                "success": False,
                "risk_score": risk_score,
                "phishing": phishing,
                "malware": malware,
                "suspicious": suspicious,
                "unsafe": unsafe,
                "raw_summary": "respuesta de proveedor sin exito",
            }

        logger.info(
            "Respuesta IPQS correcta | host=%s | risk_score=%s | phishing=%s | malware=%s | suspicious=%s | unsafe=%s",
            host,
            risk_score,
            phishing,
            malware,
            suspicious,
            unsafe,
        )
        return {
            "provider_status": "ok",
            "available": True,
            "success": True,
            "risk_score": risk_score,
            "phishing": phishing,
            "malware": malware,
            "suspicious": suspicious,
            "unsafe": unsafe,
            "raw_summary": "analisis completado",
        }

    except Exception:
        logger.exception("Error inesperado al consultar IPQS")
        return {
            "provider_status": "error",
            "available": False,
            "success": False,
            "risk_score": None,
            "phishing": False,
            "malware": False,
            "suspicious": False,
            "unsafe": False,
            "raw_summary": "error de consulta",
        }
