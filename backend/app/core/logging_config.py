import logging

LOG_FORMAT = "%(asctime)s | %(levelname)s | %(name)s | %(message)s"


def configure_logging() -> None:
    # Configuracion minima para desarrollo: logs en consola con nivel INFO.
    logging.basicConfig(
        level=logging.INFO,
        format=LOG_FORMAT,
        force=True,
    )

    # Bajamos ruido de librerias HTTP para evitar imprimir URLs completas o datos sensibles.
    logging.getLogger("httpx").setLevel(logging.WARNING)
    logging.getLogger("httpcore").setLevel(logging.WARNING)
