from pydantic import BaseModel, field_validator

from app.utils.url_validation import validate_public_web_url


class ScreenshotRequest(BaseModel):
    url: str

    @field_validator("url")
    @classmethod
    def url_must_be_a_valid_web_url(cls, value: str) -> str:
        return validate_public_web_url(value)


class ScreenshotResponse(BaseModel):
    available: bool
    image_url: str | None = None
    message: str
