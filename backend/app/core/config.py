import os
import logging

from dotenv import load_dotenv

load_dotenv()
logger = logging.getLogger(__name__)


def _clean_env_value(value: str | None) -> str | None:
    if value is None:
        return None
    normalized = value.strip()
    return normalized or None


def _get_positive_float_from_env(var_name: str, default: float) -> float:
    raw_value = _clean_env_value(os.getenv(var_name))
    if raw_value is None:
        return default
    try:
        parsed_value = float(raw_value)
    except ValueError:
        logger.warning(
            "%s invalida en entorno; se usa valor por defecto %s",
            var_name,
            default,
        )
        return default

    if parsed_value <= 0:
        logger.warning(
            "%s debe ser > 0; se usa valor por defecto %s",
            var_name,
            default,
        )
        return default
    return parsed_value


def _get_bool_from_env(var_name: str, default: bool) -> bool:
    raw_value = _clean_env_value(os.getenv(var_name))
    if raw_value is None:
        return default

    normalized = raw_value.lower()
    if normalized in {"1", "true", "yes", "on"}:
        return True
    if normalized in {"0", "false", "no", "off"}:
        return False

    logger.warning(
        "%s invalida en entorno; se usa valor por defecto %s",
        var_name,
        default,
    )
    return default


WEBRISK_API_KEY: str | None = _clean_env_value(
    os.getenv("WEBRISK_API_KEY") or os.getenv("GOOGLE_WEB_RISK_API_KEY")
)
IPQS_API_KEY: str | None = _clean_env_value(os.getenv("IPQS_API_KEY"))
SNAPRENDER_API_KEY: str | None = _clean_env_value(os.getenv("SNAPRENDER_API_KEY"))
OPENPHISH_ENABLED: bool = _get_bool_from_env("OPENPHISH_ENABLED", default=False)
OPENPHISH_DB_PATH: str | None = _clean_env_value(os.getenv("OPENPHISH_DB_PATH"))

# Politica de seguridad para /api/v1/analyze:
# cada solicitud consume cuota de proveedores externos (Web Risk + IPQS)
# y ayuda a mitigar abuso del endpoint.
RATE_LIMIT_ANALYZE: str = "30/minute"

# Politica de seguridad para tiempo maximo total del caso de uso /analyze.
ANALYSIS_TOTAL_TIMEOUT_SECONDS: float = _get_positive_float_from_env(
    "ANALYSIS_TOTAL_TIMEOUT_SECONDS",
    default=12.0,
)

# Politica de seguridad para integraciones de captura remota.
# Debe ser corto para evitar bloquear la app si el proveedor externo falla.
SCREENSHOT_TIMEOUT_SECONDS: float = _get_positive_float_from_env(
    "SCREENSHOT_TIMEOUT_SECONDS",
    default=6.0,
)
