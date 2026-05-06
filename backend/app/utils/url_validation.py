import ipaddress
from urllib.parse import urlsplit


_MAX_URL_LENGTH = 2048
_ALLOWED_SCHEMES = {"http", "https"}


def _contains_control_chars(value: str) -> bool:
    return any(ord(character) < 32 or ord(character) == 127 for character in value)


def _is_non_public_special_ip_literal(hostname: str) -> bool:
    try:
        parsed_ip = ipaddress.ip_address(hostname)
    except ValueError:
        return False

    return (
        parsed_ip.is_private
        or parsed_ip.is_loopback
        or parsed_ip.is_link_local
        or parsed_ip.is_reserved
        or parsed_ip.is_multicast
        or parsed_ip.is_unspecified
    )


def validate_public_web_url(value: str) -> str:
    # Esta funcion concentra la validacion comun de entrada para URLs web.
    # Mantenerla en un unico sitio evita que /analyze y futuros endpoints
    # apliquen reglas distintas por accidente.
    normalized = value.strip()
    if not normalized:
        raise ValueError("La URL no puede estar vacia")
    if len(normalized) > _MAX_URL_LENGTH:
        raise ValueError(f"La URL no puede superar {_MAX_URL_LENGTH} caracteres")
    if _contains_control_chars(normalized):
        raise ValueError("La URL contiene caracteres no permitidos")

    try:
        parsed = urlsplit(normalized)
    except ValueError as exc:
        raise ValueError("La URL no tiene un formato valido") from exc

    if not parsed.scheme:
        raise ValueError("La URL debe incluir un esquema http o https")
    if parsed.scheme not in _ALLOWED_SCHEMES:
        raise ValueError("Solo se permiten URLs http o https")
    if "@" in parsed.netloc:
        raise ValueError("La URL no puede incluir credenciales")

    try:
        hostname = parsed.hostname
    except ValueError as exc:
        raise ValueError("La URL debe tener un host valido") from exc

    try:
        parsed.port
    except ValueError as exc:
        raise ValueError("La URL debe tener un puerto valido") from exc

    if not hostname:
        raise ValueError("La URL debe tener un host valido")
    if _is_non_public_special_ip_literal(hostname):
        raise ValueError(
            "La URL usa una direccion de red privada, local o reservada y no puede analizarse de forma segura"
        )

    return normalized
