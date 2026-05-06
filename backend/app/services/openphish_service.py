import logging
import sqlite3
from datetime import datetime, timezone
from pathlib import Path

from app.core.config import OPENPHISH_DB_PATH, OPENPHISH_ENABLED
from app.services.provider_results import OpenPhishMatchType, OpenPhishResult
from app.utils.url_utils import normalize_web_url_for_matching

logger = logging.getLogger(__name__)
_BACKEND_ROOT = Path(__file__).resolve().parents[2]
_LEGACY_DISCOVERED_AT_FORMATS = (
    "%d-%m-%Y %H:%M:%S UTC",
    "%d-%m-%Y %H:%M:%S",
)


def _resolve_openphish_db_path() -> Path | None:
    if not OPENPHISH_DB_PATH:
        return None

    configured_path = Path(OPENPHISH_DB_PATH)
    if configured_path.is_absolute():
        return configured_path
    return (_BACKEND_ROOT / configured_path).resolve()


def _safe_text_or_none(value: object, *, max_length: int = 200) -> str | None:
    if not isinstance(value, str):
        return None
    normalized = value.strip()
    if not normalized:
        return None
    return normalized[:max_length]


def _is_spear_or_none(value: object) -> bool | None:
    if isinstance(value, bool):
        return value
    if isinstance(value, int) and value in {0, 1}:
        return bool(value)
    return None


def _parse_discovered_at(value: object) -> datetime | None:
    if not isinstance(value, str):
        return None

    normalized = value.strip()
    if not normalized:
        return None

    candidate = normalized
    if candidate.endswith("Z"):
        candidate = candidate[:-1] + "+00:00"

    try:
        parsed = datetime.fromisoformat(candidate)
    except ValueError:
        for date_format in _LEGACY_DISCOVERED_AT_FORMATS:
            try:
                parsed = datetime.strptime(normalized, date_format)
                return parsed.replace(tzinfo=timezone.utc)
            except ValueError:
                continue
        return None

    if parsed.tzinfo is None:
        return parsed.replace(tzinfo=timezone.utc)
    return parsed.astimezone(timezone.utc)


def _compute_age_days(discovered_at: object) -> int | None:
    parsed = _parse_discovered_at(discovered_at)
    if parsed is None:
        return None

    now_utc = datetime.now(timezone.utc)
    delta = now_utc - parsed
    return max(delta.days, 0)


def _query_exact_url_match(
    connection: sqlite3.Connection,
    normalized_url: str,
) -> sqlite3.Row | None:
    cursor = connection.execute(
        """
        SELECT
            brand,
            sector,
            family_id,
            is_spear,
            discovered_at
        FROM openphish_entries
        WHERE normalized_url = ?
        ORDER BY discovered_at DESC, id DESC
        LIMIT 1
        """,
        (normalized_url,),
    )
    return cursor.fetchone()


def _query_exact_host_match(
    connection: sqlite3.Connection,
    normalized_host: str,
) -> sqlite3.Row | None:
    cursor = connection.execute(
        """
        SELECT
            brand,
            sector,
            family_id,
            is_spear,
            discovered_at
        FROM openphish_entries
        WHERE normalized_host = ?
        ORDER BY discovered_at DESC, id DESC
        LIMIT 1
        """,
        (normalized_host,),
    )
    return cursor.fetchone()


def _build_success_result(
    row: sqlite3.Row | None,
    *,
    match_type: OpenPhishMatchType,
) -> OpenPhishResult:
    if row is None:
        return OpenPhishResult.from_success(match_found=False)

    return OpenPhishResult.from_success(
        match_found=True,
        match_type=match_type,
        age_days=_compute_age_days(row["discovered_at"]),
        brand=_safe_text_or_none(row["brand"]),
        sector=_safe_text_or_none(row["sector"]),
        family_id=_safe_text_or_none(row["family_id"]),
        is_spear=_is_spear_or_none(row["is_spear"]),
    )


def _log_openphish_result(
    result: OpenPhishResult,
    *,
    request_id: str | None,
    discovered_at_present: bool = False,
) -> None:
    if result.match_found:
        logger.info(
            "Resultado OpenPhish | request_id=%s | provider_status=%s | available=%s | match_found=%s | match_type=%s | discovered_at_present=%s | age_days=%s | family_id=%s | brand=%s | sector=%s | is_spear=%s",
            request_id,
            result.provider_status,
            result.available,
            result.match_found,
            result.match_type,
            discovered_at_present,
            result.age_days,
            result.family_id,
            result.brand,
            result.sector,
            result.is_spear,
        )
        return

    logger.info(
        "Resultado OpenPhish | request_id=%s | provider_status=%s | available=%s | match_found=%s | match_type=%s | discovered_at_present=%s | age_days=%s",
        request_id,
        result.provider_status,
        result.available,
        result.match_found,
        result.match_type,
        discovered_at_present,
        result.age_days,
    )


def _has_discovered_at_value(row: sqlite3.Row | None) -> bool:
    if row is None:
        return False
    return _safe_text_or_none(row["discovered_at"], max_length=80) is not None


def check_url_with_openphish(
    url: str,
    request_id: str | None = None,
) -> OpenPhishResult:
    """Consulta la SQLite local de OpenPhish sin bloquear la logica principal.

    OpenPhish aporta cobertura especifica sobre phishing conocido y documentado,
    pero no sustituye Web Risk ni IPQS, que siguen cubriendo otras amenazas
    como malware, software no deseado o reputacion contextual.

    La cobertura de OpenPhish no es uniforme entre sectores ni regiones, lo
    que refuerza la necesidad de mantener Google Web Risk e IPQualityScore
    como fuentes primarias con consulta en tiempo real.
    """

    if not OPENPHISH_ENABLED:
        result = OpenPhishResult.from_disabled()
        _log_openphish_result(result, request_id=request_id)
        return result

    db_path = _resolve_openphish_db_path()
    if db_path is None or not db_path.is_file():
        result = OpenPhishResult.from_db_missing()
        _log_openphish_result(result, request_id=request_id)
        return result

    normalized_url = normalize_web_url_for_matching(url)
    if normalized_url is None:
        logger.warning(
            "No se pudo normalizar la URL para OpenPhish | request_id=%s",
            request_id,
        )
        result = OpenPhishResult.from_internal_error("url no normalizable")
        _log_openphish_result(result, request_id=request_id)
        return result

    try:
        with sqlite3.connect(db_path) as connection:
            connection.row_factory = sqlite3.Row

            exact_url_match = _query_exact_url_match(
                connection,
                normalized_url.normalized_url,
            )
            if exact_url_match is not None:
                result = _build_success_result(
                    exact_url_match,
                    match_type="exact_url",
                )
                _log_openphish_result(
                    result,
                    request_id=request_id,
                    discovered_at_present=_has_discovered_at_value(exact_url_match),
                )
                return result

            exact_host_match = _query_exact_host_match(
                connection,
                normalized_url.normalized_host,
            )
            result = _build_success_result(
                exact_host_match,
                match_type="exact_host",
            )
            _log_openphish_result(
                result,
                request_id=request_id,
                discovered_at_present=_has_discovered_at_value(exact_host_match),
            )
            return result

    except sqlite3.Error:
        logger.exception(
            "Error al consultar SQLite de OpenPhish | request_id=%s",
            request_id,
        )
        result = OpenPhishResult.from_db_error()
        _log_openphish_result(result, request_id=request_id)
        return result
    except Exception:
        logger.exception(
            "Error inesperado al consultar OpenPhish | request_id=%s",
            request_id,
        )
        result = OpenPhishResult.from_internal_error("error de consulta")
        _log_openphish_result(result, request_id=request_id)
        return result
