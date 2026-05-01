import logging
from urllib.parse import quote, urlsplit

import httpx

from app.core.config import IPQS_API_KEY
from app.core.http_client import get_http_client
from app.services.provider_results import IpqsResult
from app.utils.url_utils import remove_url_fragment

logger = logging.getLogger(__name__)
# TODO: mover logs nominales de proveedor a DEBUG en producción estable.

def _provider_success_to_bool(value: object) -> bool:
    if isinstance(value, bool):
        return value
    if isinstance(value, str):
        return value.strip().lower() in {"1", "true", "yes"}
    if isinstance(value, (int, float)):
        return value != 0
    return False


def _security_flag_to_bool(
    value: object,
    field_name: str,
    request_id: str | None = None,
) -> bool:
    if isinstance(value, bool):
        return value
    if value is not None:
        logger.warning(
            "Flag de seguridad con tipo inesperado descartado | field=%s | type=%s | request_id=%s",
            field_name,
            type(value).__name__,
            request_id,
        )
    return False


def _to_int_or_none(value: object) -> int | None:
    try:
        return int(value) if value is not None else None
    except (TypeError, ValueError):
        return None


def _bool_or_none(value: object) -> bool | None:
    return value if isinstance(value, bool) else None


def _domain_age_human_or_none(value: object) -> str | None:
    if not isinstance(value, dict):
        return None
    human = value.get("human")
    if not isinstance(human, str):
        return None
    normalized = human.strip()
    if not normalized or len(normalized) > 80:
        return None
    return normalized


def _normalize_risk_score(
    value: int | None,
    request_id: str | None = None,
    host: str | None = None,
) -> int | None:
    if value is None:
        return None
    if 0 <= value <= 100:
        return value
    logger.warning(
        "IPQS devolvio risk_score fuera de rango [0,100] y se descarta | request_id=%s | host=%s | risk_score=%s",
        request_id,
        host,
        value,
    )
    return None


async def check_url_with_ipqs(
    url: str,
    request_id: str | None = None,
) -> IpqsResult:
    if not IPQS_API_KEY:
        logger.warning(
            "IPQS_API_KEY no configurada; no se puede consultar IPQS | request_id=%s",
            request_id,
        )
        return IpqsResult.from_config_error("IPQS_API_KEY no configurada")

    analysis_url = remove_url_fragment(url)
    encoded_url = quote(analysis_url, safe="")
    host = urlsplit(analysis_url).hostname

    logger.info(
        "Consultando IPQS | request_id=%s | host=%s",
        request_id,
        host,
    )

    endpoint = "https://ipqualityscore.com/api/json/url"
    # Limitacion del proveedor: IPQS exige API key en la ruta.
    # No registrar request_url evita exponer secretos en logs de aplicación.
    request_url = f"{endpoint}/{IPQS_API_KEY}/{encoded_url}"

    try:
        client = get_http_client()
        response = await client.get(request_url)

        if response.status_code != 200:
            logger.error(
                "Error HTTP al consultar IPQS | request_id=%s | status_code=%s",
                request_id,
                response.status_code,
            )
            return IpqsResult.from_http_error(response.status_code)

        try:
            data = response.json()
        except ValueError:
            logger.error(
                "Respuesta no JSON valida de IPQS | request_id=%s | host=%s",
                request_id,
                host,
            )
            return IpqsResult.from_parse_error("respuesta no valida del proveedor")

        success = _provider_success_to_bool(data.get("success"))
        risk_score = _normalize_risk_score(
            _to_int_or_none(data.get("risk_score")),
            request_id=request_id,
            host=host,
        )
        phishing = _security_flag_to_bool(data.get("phishing"), "phishing", request_id)
        malware = _security_flag_to_bool(data.get("malware"), "malware", request_id)
        suspicious = _security_flag_to_bool(data.get("suspicious"), "suspicious", request_id)
        unsafe = _security_flag_to_bool(data.get("unsafe"), "unsafe", request_id)
        parking = _bool_or_none(data.get("parking"))
        spamming = _bool_or_none(data.get("spamming"))
        domain_age_human = _domain_age_human_or_none(data.get("domain_age"))

        if not success:
            logger.warning(
                "IPQS respondio success=false | request_id=%s | host=%s",
                request_id,
                host,
            )
            return IpqsResult.from_api_error(
                risk_score=risk_score,
                phishing=phishing,
                malware=malware,
                suspicious=suspicious,
                unsafe=unsafe,
                parking=parking,
                spamming=spamming,
                domain_age_human=domain_age_human,
            )

        logger.info(
            "Respuesta IPQS correcta | request_id=%s | host=%s | risk_score=%s | phishing=%s | malware=%s | suspicious=%s | unsafe=%s | parking=%s | spamming=%s | domain_age_human_present=%s",
            request_id,
            host,
            risk_score,
            phishing,
            malware,
            suspicious,
            unsafe,
            parking,
            spamming,
            domain_age_human is not None,
        )
        return IpqsResult.from_success(
            risk_score=risk_score,
            phishing=phishing,
            malware=malware,
            suspicious=suspicious,
            unsafe=unsafe,
            parking=parking,
            spamming=spamming,
            domain_age_human=domain_age_human,
        )

    except RuntimeError:
        raise
    except httpx.HTTPError:
        logger.exception(
            "Error HTTP al consultar IPQS | request_id=%s",
            request_id,
        )
        return IpqsResult.from_network_error()
    except Exception:
        logger.exception(
            "Error inesperado al consultar IPQS | request_id=%s",
            request_id,
        )
        return IpqsResult.from_internal_error("error de consulta")
