import logging
import os

LOG_FORMAT = "%(asctime)s | %(levelname)s | %(name)s | %(message)s"
_ALLOWED_LOG_LEVELS: dict[str, int] = {
    "DEBUG": logging.DEBUG,
    "INFO": logging.INFO,
    "WARNING": logging.WARNING,
    "ERROR": logging.ERROR,
    "CRITICAL": logging.CRITICAL,
}


def configure_logging() -> None:
    raw_log_level = (os.getenv("LOG_LEVEL") or "INFO").strip().upper()
    log_level = _ALLOWED_LOG_LEVELS.get(raw_log_level)
    if log_level is None:
        log_level = logging.INFO
        # Aviso temprano: todavia no hay logger configurado.
        print(
            f"[logging_config] LOG_LEVEL={raw_log_level!r} no reconocido; usando INFO"
        )

    logging.basicConfig(
        level=log_level,
        format=LOG_FORMAT,
        force=True,
    )

    # Bajamos ruido de librerias HTTP para evitar imprimir URLs completas o datos sensibles.
    logging.getLogger("httpx").setLevel(logging.WARNING)
    logging.getLogger("httpcore").setLevel(logging.WARNING)
