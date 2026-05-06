from dataclasses import dataclass
from urllib.parse import urlsplit, urlunsplit

_ALLOWED_WEB_SCHEMES = {"http", "https"}


@dataclass(frozen=True)
class NormalizedWebUrl:
    """Representacion normalizada para comparaciones exactas de OpenPhish.

    Se usa tanto al importar el feed como al consultar una URL en runtime
    para que ambas rutas de codigo comparen exactamente con las mismas reglas.
    """

    normalized_url: str
    normalized_host: str


def remove_url_fragment(url: str) -> str:
    parsed = urlsplit(url)
    return urlunsplit((parsed.scheme, parsed.netloc, parsed.path, parsed.query, ""))


def _contains_control_chars(value: str) -> bool:
    return any(ord(character) < 32 or ord(character) == 127 for character in value)


def _build_normalized_netloc(hostname: str, port: int | None) -> str:
    # urlsplit().hostname elimina corchetes en IPv6; los reponemos para
    # reconstruir una URL valida y estable sin tocar path ni query.
    host_for_netloc = f"[{hostname}]" if ":" in hostname else hostname
    if port is None:
        return host_for_netloc
    return f"{host_for_netloc}:{port}"


def normalize_web_url_for_matching(url: str) -> NormalizedWebUrl | None:
    """Normaliza una URL web para comparaciones exactas de OpenPhish.

    Reglas:
    - elimina fragmento usando remove_url_fragment()
    - fuerza scheme y host en minusculas
    - elimina un punto final sobrante del host
    - conserva path y query tal cual
    - no sigue redirecciones ni resuelve DNS

    Devuelve None si la URL no puede normalizarse de forma segura.
    """

    normalized_input = url.strip()
    if not normalized_input or _contains_control_chars(normalized_input):
        return None

    try:
        fragment_free_url = remove_url_fragment(normalized_input)
        parsed = urlsplit(fragment_free_url)
    except ValueError:
        return None

    if parsed.username is not None or parsed.password is not None:
        return None

    normalized_scheme = parsed.scheme.lower()
    if normalized_scheme not in _ALLOWED_WEB_SCHEMES:
        return None

    try:
        hostname = parsed.hostname
    except ValueError:
        return None

    if not hostname:
        return None

    normalized_host = hostname.lower().rstrip(".")
    if not normalized_host:
        return None

    try:
        port = parsed.port
    except ValueError:
        return None

    normalized_netloc = _build_normalized_netloc(normalized_host, port)
    normalized_url = urlunsplit(
        (
            normalized_scheme,
            normalized_netloc,
            parsed.path,
            parsed.query,
            "",
        )
    )

    return NormalizedWebUrl(
        normalized_url=normalized_url,
        normalized_host=normalized_host,
    )
