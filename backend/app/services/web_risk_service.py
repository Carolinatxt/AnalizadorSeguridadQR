import logging
from urllib.parse import quote, urlsplit

from app.core.config import WEBRISK_API_KEY
from app.core.http_client import get_http_client
from app.utils.url_utils import remove_url_fragment

logger = logging.getLogger(__name__)

async def check_url_with_web_risk(url: str) -> dict:
    if not WEBRISK_API_KEY:
        logger.warning("WEBRISK_API_KEY no configurada; no se puede consultar Web Risk")
        return {
            "provider_status": "error",
            "available": False,
            "match_found": False,
            "threat_types": [],
            "raw_summary": "WEBRISK_API_KEY no configurada"
        }

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
    client = get_http_client()

    try:
        response = await client.get(endpoint, params=params, headers=headers)

        if response.status_code != 200:
            logger.error(
                "Error HTTP al consultar Web Risk | status_code=%s",
                response.status_code,
            )
            return {
                "provider_status": "error",
                "available": False,
                "match_found": False,
                "threat_types": [],
                "raw_summary": f"Web Risk HTTP {response.status_code}"
            }

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

        return {
            "provider_status": "ok",
            "available": True,
            "match_found": len(threat_types) > 0,
            "threat_types": threat_types,
            "raw_summary": "match encontrado" if threat_types else "sin coincidencia"
        }

    except Exception:
        logger.exception("Error inesperado al consultar Web Risk")
        return {
            "provider_status": "error",
            "available": False,
            "match_found": False,
            "threat_types": [],
            "raw_summary": "error de consulta"
        }
