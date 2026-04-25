import os

from dotenv import load_dotenv

load_dotenv()


def _clean_env_value(value: str | None) -> str | None:
    if value is None:
        return None
    normalized = value.strip()
    return normalized or None


WEBRISK_API_KEY: str | None = _clean_env_value(
    os.getenv("WEBRISK_API_KEY") or os.getenv("GOOGLE_WEB_RISK_API_KEY")
)
IPQS_API_KEY: str | None = _clean_env_value(os.getenv("IPQS_API_KEY"))
