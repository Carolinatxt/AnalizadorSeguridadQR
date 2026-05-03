from typing import Literal

from pydantic import BaseModel, Field, field_validator

from app.utils.url_validation import validate_public_web_url


class AnalyzeUrlRequest(BaseModel):
    url: str

    @field_validator("url")
    @classmethod
    def url_must_be_http_or_https(cls, value: str) -> str:
        return validate_public_web_url(value)


class AnalyzeUrlResponse(BaseModel):
    risk_level: Literal["safe", "suspicious", "dangerous"]
    analysis_status: Literal["complete", "partial", "unavailable"]
    summary: str
    reasons: list[str] = Field(default_factory=list)
    # FUTURO: añadir details cuando frontend soporte explicabilidad avanzada.
