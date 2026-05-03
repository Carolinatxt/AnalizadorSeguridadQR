import logging
import time
from dataclasses import dataclass
from urllib.parse import urlparse, urlsplit

import httpx

from app.core.config import SNAPRENDER_API_KEY, SCREENSHOT_TIMEOUT_SECONDS
from app.core.http_client import get_http_client
from app.utils.url_utils import remove_url_fragment

logger = logging.getLogger(__name__)

_SNAPRENDER_SIGN_ENDPOINT = "https://app.snap-render.com/v1/screenshot/sign"
_SNAPRENDER_EXPECTED_HOST = "app.snap-render.com"
_SNAPRENDER_TIMEOUT = httpx.Timeout(SCREENSHOT_TIMEOUT_SECONDS)
_SNAPRENDER_PAYLOAD = {
    "expires_in": 3600,
    "format": "webp",
    # Viewport movil mas cercano a telefonos Android actuales.
    # Ayuda a que el render del proveedor encaje mejor con layouts responsive.
    "width": 412,
    "height": 915,
    "full_page": False,
    "cache": True,
    "cache_ttl": 86400,
}
_GENERIC_USER_MESSAGE = "No se pudo generar la vista previa."
_TEMPORARY_USER_MESSAGE = (
    "La vista previa no esta disponible temporalmente. Intentalo mas tarde."
)


@dataclass(frozen=True)
class ScreenshotResult:
    available: bool
    image_url: str | None
    provider_status: str
    message: str


def _build_result(
    *,
    available: bool,
    image_url: str | None,
    provider_status: str,
    message: str,
) -> ScreenshotResult:
    return ScreenshotResult(
        available=available,
        image_url=image_url,
        provider_status=provider_status,
        message=message,
    )


def _log_provider_result(
    *,
    log_level: int,
    request_id: str | None,
    provider_status: str,
    host: str | None,
    duration_ms: int,
) -> None:
    logger.log(
        log_level,
        "Resultado SnapRender | request_id=%s | provider=snaprender | provider_status=%s | host=%s | duration_ms=%s",
        request_id,
        provider_status,
        host,
        duration_ms,
    )


def _finish_result(
    *,
    log_level: int,
    request_id: str | None,
    provider_status: str,
    host: str | None,
    start_time: float,
    message: str,
    available: bool,
    image_url: str | None,
) -> ScreenshotResult:
    duration_ms = int((time.perf_counter() - start_time) * 1000)
    _log_provider_result(
        log_level=log_level,
        request_id=request_id,
        provider_status=provider_status,
        host=host,
        duration_ms=duration_ms,
    )
    return _build_result(
        available=available,
        image_url=image_url,
        provider_status=provider_status,
        message=message,
    )


def _extract_error_code(data: object) -> str | None:
    if not isinstance(data, dict):
        return None
    error = data.get("error")
    if not isinstance(error, dict):
        return None
    code = error.get("code")
    if not isinstance(code, str):
        return None
    normalized = code.strip().upper()
    return normalized or None


def _extract_signed_url(data: object) -> str | None:
    if not isinstance(data, dict):
        return None
    signed_url = data.get("signed_url")
    if not isinstance(signed_url, str):
        return None
    normalized = signed_url.strip()
    return normalized or None


def _safe_response_json(response: httpx.Response) -> dict[str, object]:
    try:
        data = response.json()
    except ValueError:
        return {}
    return data if isinstance(data, dict) else {}


def _is_valid_snaprender_signed_url(signed_url: str) -> bool:
    parsed = urlparse(signed_url)
    return (
        parsed.scheme == "https"
        and parsed.hostname is not None
        and parsed.hostname == _SNAPRENDER_EXPECTED_HOST
    )


async def create_screenshot_preview(
    url: str,
    request_id: str | None = None,
) -> ScreenshotResult:
    analysis_url = remove_url_fragment(url)
    host = urlsplit(analysis_url).hostname
    start_time = time.perf_counter()

    if not SNAPRENDER_API_KEY:
        return _finish_result(
            log_level=logging.WARNING,
            request_id=request_id,
            provider_status="config_error",
            host=host,
            start_time=start_time,
            message=_GENERIC_USER_MESSAGE,
            available=False,
            image_url=None,
        )

    payload = {
        "url": analysis_url,
        **_SNAPRENDER_PAYLOAD,
    }
    headers = {
        "X-API-Key": SNAPRENDER_API_KEY,
    }

    try:
        client = get_http_client()
        response = await client.post(
            _SNAPRENDER_SIGN_ENDPOINT,
            json=payload,
            headers=headers,
            timeout=_SNAPRENDER_TIMEOUT,
        )
    except RuntimeError:
        raise
    except httpx.TimeoutException:
        return _finish_result(
            log_level=logging.WARNING,
            request_id=request_id,
            provider_status="timeout",
            host=host,
            start_time=start_time,
            message=_GENERIC_USER_MESSAGE,
            available=False,
            image_url=None,
        )
    except httpx.HTTPError:
        logger.exception(
            "Error HTTP al solicitar signed URL a SnapRender | request_id=%s | provider=snaprender | host=%s",
            request_id,
            host,
        )
        return _finish_result(
            log_level=logging.ERROR,
            request_id=request_id,
            provider_status="network_error",
            host=host,
            start_time=start_time,
            message=_GENERIC_USER_MESSAGE,
            available=False,
            image_url=None,
        )
    except Exception:
        logger.exception(
            "Error inesperado al solicitar signed URL a SnapRender | request_id=%s | provider=snaprender | host=%s",
            request_id,
            host,
        )
        return _finish_result(
            log_level=logging.ERROR,
            request_id=request_id,
            provider_status="internal_error",
            host=host,
            start_time=start_time,
            message=_GENERIC_USER_MESSAGE,
            available=False,
            image_url=None,
        )

    data = _safe_response_json(response)

    if response.status_code == 401:
        return _finish_result(
            log_level=logging.ERROR,
            request_id=request_id,
            provider_status="config_error",
            host=host,
            start_time=start_time,
            message=_GENERIC_USER_MESSAGE,
            available=False,
            image_url=None,
        )

    if response.status_code == 429:
        error_code = _extract_error_code(data)
        provider_status = "quota_exceeded" if error_code == "QUOTA_EXCEEDED" else "rate_limited"
        return _finish_result(
            log_level=logging.WARNING,
            request_id=request_id,
            provider_status=provider_status,
            host=host,
            start_time=start_time,
            message=_TEMPORARY_USER_MESSAGE,
            available=False,
            image_url=None,
        )

    if response.status_code in {408, 504}:
        return _finish_result(
            log_level=logging.WARNING,
            request_id=request_id,
            provider_status="timeout",
            host=host,
            start_time=start_time,
            message=_GENERIC_USER_MESSAGE,
            available=False,
            image_url=None,
        )

    if response.status_code != 200:
        error_code = _extract_error_code(data)
        if error_code == "BLOCKED_URL":
            return _finish_result(
                log_level=logging.WARNING,
                request_id=request_id,
                provider_status="blocked_url",
                host=host,
                start_time=start_time,
                message=_GENERIC_USER_MESSAGE,
                available=False,
                image_url=None,
            )

        return _finish_result(
            log_level=logging.WARNING,
            request_id=request_id,
            provider_status="provider_error",
            host=host,
            start_time=start_time,
            message=_GENERIC_USER_MESSAGE,
            available=False,
            image_url=None,
        )

    if not data:
        return _finish_result(
            log_level=logging.ERROR,
            request_id=request_id,
            provider_status="parse_error",
            host=host,
            start_time=start_time,
            message=_GENERIC_USER_MESSAGE,
            available=False,
            image_url=None,
        )

    signed_url = _extract_signed_url(data)
    if not signed_url or not _is_valid_snaprender_signed_url(signed_url):
        return _finish_result(
            log_level=logging.ERROR,
            request_id=request_id,
            provider_status="invalid_signed_url",
            host=host,
            start_time=start_time,
            message=_GENERIC_USER_MESSAGE,
            available=False,
            image_url=None,
        )

    return _finish_result(
        log_level=logging.INFO,
        request_id=request_id,
        provider_status="ok",
        host=host,
        start_time=start_time,
        message="Vista previa generada correctamente.",
        available=True,
        image_url=signed_url,
    )
