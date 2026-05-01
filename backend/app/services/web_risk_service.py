import logging
from urllib.parse import urlsplit

import httpx

from app.core.config import WEBRISK_API_KEY
from app.core.http_client import get_http_client
from app.services.provider_results import WebRiskResult
from app.utils.url_utils import remove_url_fragment

logger = logging.getLogger(__name__)
# TODO: mover logs nominales de proveedor a DEBUG en producción estable.
# Allowlist conservadora alineada con reglas actuales del motor de decisión.
# Tipos nuevos del proveedor deben evaluarse explicitamente antes de incluirse.
_ALLOWED_THREAT_TYPES = frozenset({
    "MALWARE",
    "SOCIAL_ENGINEERING",
    "UNWANTED_SOFTWARE",
})


def _normalize_threat_types(raw_threat_types: list[object]) -> list[str]:
    if not isinstance(raw_threat_types, list):
        return []
    normalized: list[str] = []
    seen: set[str] = set()
    for threat_type in raw_threat_types:
        if not isinstance(threat_type, str):
            continue
        if threat_type not in _ALLOWED_THREAT_TYPES:
            continue
        if threat_type in seen:
            continue
        seen.add(threat_type)
        normalized.append(threat_type)
    return normalized

async def check_url_with_web_risk(
    url: str,
    request_id: str | None = None,
) -> WebRiskResult:
    if not WEBRISK_API_KEY:
        logger.warning(
            "WEBRISK_API_KEY no configurada; no se puede consultar Web Risk | request_id=%s",
            request_id,
        )
        return WebRiskResult.from_config_error("WEBRISK_API_KEY no configurada")

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
            return WebRiskResult.from_parse_error("respuesta no valida del proveedor")

        threat = data.get("threat")
        raw_threat_types = threat.get("threatTypes", []) if threat else []
        threat_types = _normalize_threat_types(raw_threat_types)

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
        return WebRiskResult.from_network_error()
    except Exception:
        logger.exception(
            "Error inesperado al consultar Web Risk | request_id=%s",
            request_id,
        )
        return WebRiskResult.from_internal_error("error de consulta")
