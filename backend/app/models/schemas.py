from typing import Literal
from urllib.parse import urlparse

from pydantic import BaseModel, Field, field_validator


class AnalyzeUrlRequest(BaseModel):
    url: str

    @field_validator("url")
    @classmethod
    def url_must_be_http_or_https(cls, value: str) -> str:
        normalized = value.strip()
        if not normalized:
            raise ValueError("La URL no puede estar vacia")
        if len(normalized) > 2048:
            raise ValueError("La URL no puede superar 2048 caracteres")

        parsed = urlparse(normalized)
        if parsed.scheme not in {"http", "https"}:
            raise ValueError("Solo se permiten URLs http o https")
        if not parsed.hostname:
            raise ValueError("La URL debe tener un host valido")

        return normalized


class AnalyzeUrlResponse(BaseModel):
    risk_level: Literal["safe", "suspicious", "dangerous"]
    analysis_status: Literal["complete", "partial", "unavailable"]
    summary: str
    reasons: list[str] = Field(default_factory=list)
    # FUTURO: añadir details cuando frontend soporte explicabilidad avanzada.
