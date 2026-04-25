import logging
from urllib.parse import urlsplit

import httpx

from app.core.config import WEBRISK_API_KEY
from app.core.http_client import get_http_client
from app.services.provider_results import WebRiskResult
from app.utils.url_utils import remove_url_fragment

logger = logging.getLogger(__name__)

async def check_url_with_web_risk(
    url: str,
    request_id: str | None = None,
) -> WebRiskResult:
    if not WEBRISK_API_KEY:
        logger.warning(
            "WEBRISK_API_KEY no configurada; no se puede consultar Web Risk | request_id=%s",
            request_id,
        )
        return WebRiskResult.from_unavailable("WEBRISK_API_KEY no configurada")

    analysis_url = remove_url_fragment(url)
    host = urlsplit(analysis_url).hostname

    logger.info(
        "Consultando Google Web Risk | request_id=%s | host=%s",
        request_id,
        host,
    )

    endpoint = "https://webrisk.googleapis.com/v1/uris:search"
    params = [
        # No pre-encodificar: httpx serializa params y evita doble codificacion.
        ("uri", analysis_url),
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
                "Error HTTP al consultar Web Risk | request_id=%s | status_code=%s",
                request_id,
                response.status_code,
            )
            return WebRiskResult.from_http_error(response.status_code)

        try:
            data = response.json()
        except ValueError:
            logger.error(
                "Respuesta no JSON valida de Web Risk | request_id=%s | host=%s",
                request_id,
                host,
            )
            return WebRiskResult.from_internal_error("respuesta no valida del proveedor")

        threat = data.get("threat")
        raw_threat_types = threat.get("threatTypes", []) if threat else []
        threat_types = [t for t in raw_threat_types if isinstance(t, str)]

        if threat_types:
            logger.warning(
                "Amenaza detectada por Web Risk | request_id=%s | threat_types=%s",
                request_id,
                threat_types,
            )
        else:
            logger.info(
                "Web Risk sin coincidencias para la URL analizada | request_id=%s",
                request_id,
            )

        return WebRiskResult.from_success(threat_types)

    except RuntimeError:
        raise
    except httpx.HTTPError:
        logger.exception(
            "Error HTTP al consultar Web Risk | request_id=%s",
            request_id,
        )
        return WebRiskResult.from_internal_error("error de consulta")
    except Exception:
        logger.exception(
            "Error inesperado al consultar Web Risk | request_id=%s",
            request_id,
        )
        return WebRiskResult.from_internal_error("error de consulta")
