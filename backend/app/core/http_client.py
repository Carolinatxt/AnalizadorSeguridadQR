import logging

import httpx

_http_client: httpx.AsyncClient | None = None
logger = logging.getLogger(__name__)


async def init_http_client() -> None:
    global _http_client
    if _http_client is not None:
        logger.warning(
            "init_http_client llamado con cliente ya inicializado; ignorando"
        )
        return

    _http_client = httpx.AsyncClient(
        timeout=httpx.Timeout(10.0),
        limits=httpx.Limits(
            max_connections=100,
            max_keepalive_connections=20,
        ),
    )


def get_http_client() -> httpx.AsyncClient:
    if _http_client is None:
        raise RuntimeError("HTTP client global no inicializado")
    return _http_client


async def close_http_client() -> None:
    global _http_client
    if _http_client is None:
        return
    await _http_client.aclose()
    _http_client = None
