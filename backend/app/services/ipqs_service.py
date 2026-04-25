import logging
from urllib.parse import quote, urlsplit

import httpx

from app.core.config import IPQS_API_KEY
from app.core.http_client import get_http_client
from app.services.provider_results import IpqsResult
from app.utils.url_utils import remove_url_fragment

logger = logging.getLogger(__name__)

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


async def check_url_with_ipqs(url: str) -> IpqsResult:
    if not IPQS_API_KEY:
        logger.warning("IPQS_API_KEY no configurada; no se puede consultar IPQS")
        return IpqsResult.from_unavailable("IPQS_API_KEY no configurada")

    analysis_url = remove_url_fragment(url)
    encoded_url = quote(analysis_url, safe="")
    host = urlsplit(analysis_url).hostname

    logger.info("Consultando IPQS | host=%s", host)

    endpoint = "https://ipqualityscore.com/api/json/url"
    # Limitacion del proveedor: IPQS exige API key en la ruta.
    # No registrar request_url evita exponer secretos en logs de aplicacion.
    request_url = f"{endpoint}/{IPQS_API_KEY}/{encoded_url}"

    try:
        client = get_http_client()
        response = await client.get(request_url)

        if response.status_code != 200:
            logger.error(
                "Error HTTP al consultar IPQS | status_code=%s",
                response.status_code,
            )
            return IpqsResult.from_http_error(response.status_code)

        data = response.json()
        success = _to_bool(data.get("success"))
        risk_score = _to_int_or_none(data.get("risk_score"))
        phishing = _to_bool(data.get("phishing"))
        malware = _to_bool(data.get("malware"))
        suspicious = _to_bool(data.get("suspicious"))
        unsafe = _to_bool(data.get("unsafe"))

        if not success:
            logger.warning("IPQS respondio success=false | host=%s", host)
            return IpqsResult.from_api_error(
                risk_score=risk_score,
                phishing=phishing,
                malware=malware,
                suspicious=suspicious,
                unsafe=unsafe,
            )

        logger.info(
            "Respuesta IPQS correcta | host=%s | risk_score=%s | phishing=%s | malware=%s | suspicious=%s | unsafe=%s",
            host,
            risk_score,
            phishing,
            malware,
            suspicious,
            unsafe,
        )
        return IpqsResult.from_success(
            risk_score=risk_score,
            phishing=phishing,
            malware=malware,
            suspicious=suspicious,
            unsafe=unsafe,
        )

    except RuntimeError:
        raise
    except httpx.HTTPError:
        logger.exception("Error HTTP al consultar IPQS")
        return IpqsResult.from_internal_error("error de consulta")
    except Exception:
        logger.exception("Error inesperado al consultar IPQS")
        return IpqsResult.from_internal_error("error de consulta")
