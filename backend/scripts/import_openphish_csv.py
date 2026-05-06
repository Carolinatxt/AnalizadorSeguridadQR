from __future__ import annotations

import argparse
import csv
import sqlite3
import sys
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path

BACKEND_ROOT = Path(__file__).resolve().parents[1]
if str(BACKEND_ROOT) not in sys.path:
    sys.path.insert(0, str(BACKEND_ROOT))

from app.utils.url_utils import normalize_web_url_for_matching

_URL_FIELD_CANDIDATES = ("url", "uri")
_ISOTIME_FIELD_CANDIDATES = ("isotime",)
_DISCOVER_TIME_FIELD_CANDIDATES = ("discover_time",)
_BRAND_FIELD_CANDIDATES = ("brand",)
_SECTOR_FIELD_CANDIDATES = ("sector",)
_FAMILY_ID_FIELD_CANDIDATES = ("family_id",)
_IS_SPEAR_FIELD_CANDIDATES = ("is_spear",)


@dataclass(frozen=True)
class ImportStats:
    rows_read: int = 0
    rows_imported: int = 0
    rows_ignored_duplicate: int = 0
    rows_invalid: int = 0


def _clean_csv_value(value: object) -> str | None:
    if not isinstance(value, str):
        return None
    normalized = value.strip()
    return normalized or None


def _get_first_present_value(
    row: dict[str, object],
    candidate_names: tuple[str, ...],
) -> str | None:
    for field_name in candidate_names:
        value = _clean_csv_value(row.get(field_name))
        if value is not None:
            return value
    return None


def _normalize_optional_text(
    row: dict[str, object],
    candidate_names: tuple[str, ...],
    *,
    max_length: int = 200,
) -> str | None:
    value = _get_first_present_value(row, candidate_names)
    if value is None:
        return None
    return value[:max_length]


def _select_discovered_at(row: dict[str, object]) -> str | None:
    # OpenPhish expone dos campos temporales. Priorizamos "isotime" porque
    # ya viene en ISO 8601 y es mas robusto para el calculo posterior.
    isotime = _normalize_optional_text(
        row,
        _ISOTIME_FIELD_CANDIDATES,
        max_length=80,
    )
    if isotime is not None:
        return isotime

    return _normalize_optional_text(
        row,
        _DISCOVER_TIME_FIELD_CANDIDATES,
        max_length=80,
    )


def _parse_is_spear(value: str | None) -> int | None:
    if value is None:
        return None

    normalized = value.strip().lower()
    if normalized in {"1", "true", "yes", "y"}:
        return 1
    if normalized in {"0", "false", "no", "n"}:
        return 0
    return None


def _ensure_schema(connection: sqlite3.Connection) -> None:
    connection.execute(
        """
        CREATE TABLE IF NOT EXISTS openphish_entries (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            normalized_url TEXT NOT NULL UNIQUE,
            normalized_host TEXT NOT NULL,
            brand TEXT,
            sector TEXT,
            family_id TEXT,
            is_spear INTEGER,
            discovered_at TEXT,
            imported_at TEXT NOT NULL,
            CHECK (is_spear IN (0, 1) OR is_spear IS NULL)
        )
        """
    )
    connection.execute(
        "CREATE INDEX IF NOT EXISTS idx_openphish_entries_normalized_url "
        "ON openphish_entries(normalized_url)"
    )
    connection.execute(
        "CREATE INDEX IF NOT EXISTS idx_openphish_entries_normalized_host "
        "ON openphish_entries(normalized_host)"
    )
    connection.execute(
        "CREATE INDEX IF NOT EXISTS idx_openphish_entries_family_id "
        "ON openphish_entries(family_id)"
    )
    connection.execute(
        "CREATE INDEX IF NOT EXISTS idx_openphish_entries_discovered_at_desc "
        "ON openphish_entries(discovered_at DESC)"
    )


def _build_insert_row(
    row: dict[str, object],
) -> tuple[str, str, str | None, str | None, str | None, int | None, str | None] | None:
    raw_url = _get_first_present_value(row, _URL_FIELD_CANDIDATES)
    if raw_url is None:
        return None

    normalized_url_data = normalize_web_url_for_matching(raw_url)
    if normalized_url_data is None:
        return None

    brand = _normalize_optional_text(row, _BRAND_FIELD_CANDIDATES)
    sector = _normalize_optional_text(row, _SECTOR_FIELD_CANDIDATES)
    family_id = _normalize_optional_text(row, _FAMILY_ID_FIELD_CANDIDATES)
    is_spear = _parse_is_spear(_get_first_present_value(row, _IS_SPEAR_FIELD_CANDIDATES))
    discovered_at = _select_discovered_at(row)

    return (
        normalized_url_data.normalized_url,
        normalized_url_data.normalized_host,
        brand,
        sector,
        family_id,
        is_spear,
        discovered_at,
    )


def import_openphish_csv(csv_path: Path, sqlite_path: Path) -> ImportStats:
    imported_at = datetime.now(timezone.utc).isoformat(timespec="seconds")
    stats = ImportStats()

    sqlite_path.parent.mkdir(parents=True, exist_ok=True)

    with sqlite3.connect(sqlite_path) as connection:
        _ensure_schema(connection)

        with csv_path.open("r", encoding="utf-8-sig", newline="") as csv_file:
            reader = csv.DictReader(csv_file)
            for row in reader:
                stats = ImportStats(
                    rows_read=stats.rows_read + 1,
                    rows_imported=stats.rows_imported,
                    rows_ignored_duplicate=stats.rows_ignored_duplicate,
                    rows_invalid=stats.rows_invalid,
                )

                insert_row = _build_insert_row(row)
                if insert_row is None:
                    stats = ImportStats(
                        rows_read=stats.rows_read,
                        rows_imported=stats.rows_imported,
                        rows_ignored_duplicate=stats.rows_ignored_duplicate,
                        rows_invalid=stats.rows_invalid + 1,
                    )
                    continue

                cursor = connection.execute(
                    """
                    INSERT OR IGNORE INTO openphish_entries (
                        normalized_url,
                        normalized_host,
                        brand,
                        sector,
                        family_id,
                        is_spear,
                        discovered_at,
                        imported_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    (*insert_row, imported_at),
                )

                if cursor.rowcount == 1:
                    stats = ImportStats(
                        rows_read=stats.rows_read,
                        rows_imported=stats.rows_imported + 1,
                        rows_ignored_duplicate=stats.rows_ignored_duplicate,
                        rows_invalid=stats.rows_invalid,
                    )
                else:
                    stats = ImportStats(
                        rows_read=stats.rows_read,
                        rows_imported=stats.rows_imported,
                        rows_ignored_duplicate=stats.rows_ignored_duplicate + 1,
                        rows_invalid=stats.rows_invalid,
                    )

        connection.commit()

    return stats


def _build_argument_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Importa un CSV de OpenPhish a una SQLite local para el backend.",
    )
    parser.add_argument(
        "csv_path",
        type=Path,
        help="Ruta al CSV de OpenPhish del Academic Use Program.",
    )
    parser.add_argument(
        "sqlite_path",
        type=Path,
        help="Ruta destino de la SQLite de OpenPhish.",
    )
    return parser


def main() -> int:
    parser = _build_argument_parser()
    args = parser.parse_args()

    csv_path: Path = args.csv_path
    sqlite_path: Path = args.sqlite_path

    if not csv_path.is_file():
        parser.error("El CSV indicado no existe o no es un archivo.")

    stats = import_openphish_csv(csv_path=csv_path, sqlite_path=sqlite_path)
    print(f"Filas leidas: {stats.rows_read}")
    print(f"Filas importadas: {stats.rows_imported}")
    print(f"Filas ignoradas por duplicado: {stats.rows_ignored_duplicate}")
    print(f"Filas invalidas: {stats.rows_invalid}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
