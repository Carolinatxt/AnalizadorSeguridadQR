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
        timeout=httpx.Timeout(
            connect=5.0,
            read=10.0,
            write=10.0,
            pool=5.0,
        ),
        follow_redirects=False,
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
