import logging
from urllib.parse import quote, urlsplit

import httpx

from app.core.config import WEBRISK_API_KEY
from app.core.http_client import get_http_client
from app.services.provider_results import WebRiskResult
from app.utils.url_utils import remove_url_fragment

logger = logging.getLogger(__name__)

async def check_url_with_web_risk(url: str) -> WebRiskResult:
    if not WEBRISK_API_KEY:
        logger.warning("WEBRISK_API_KEY no configurada; no se puede consultar Web Risk")
        return WebRiskResult.from_unavailable("WEBRISK_API_KEY no configurada")

    analysis_url = remove_url_fragment(url)
    encoded_url = quote(analysis_url, safe="")
    host = urlsplit(analysis_url).hostname

    logger.info("Consultando Google Web Risk | host=%s", host)

    endpoint = "https://webrisk.googleapis.com/v1/uris:search"
    params = [
        ("uri", encoded_url),
        ("threatTypes", "MALWARE"),
        ("threatTypes", "SOCIAL_ENGINEERING"),
        ("threatTypes", "UNWANTED_SOFTWARE"),
    ]
    headers = {"X-Goog-Api-Key": WEBRISK_API_KEY}

    try:
        client = get_http_client()
        response = await client.get(endpoint, params=params, headers=headers)

        if response.status_code != 200:
            logger.error(
                "Error HTTP al consultar Web Risk | status_code=%s",
                response.status_code,
            )
            return WebRiskResult.from_http_error(response.status_code)

        data = response.json()

        threat = data.get("threat")
        threat_types = threat.get("threatTypes", []) if threat else []

        if threat_types:
            logger.warning(
                "Amenaza detectada por Web Risk | threat_types=%s",
                threat_types,
            )
        else:
            logger.info("Web Risk sin coincidencias para la URL analizada")

        return WebRiskResult.from_success(threat_types)

    except RuntimeError:
        raise
    except httpx.HTTPError:
        logger.exception("Error HTTP al consultar Web Risk")
        return WebRiskResult.from_internal_error("error de consulta")
    except Exception:
        logger.exception("Error inesperado al consultar Web Risk")
        return WebRiskResult.from_internal_error("error de consulta")
