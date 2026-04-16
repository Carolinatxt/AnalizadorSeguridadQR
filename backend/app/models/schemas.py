from pydantic import BaseModel


class AnalyzeUrlRequest(BaseModel):
    url: str


class AnalyzeUrlResponse(BaseModel):
    risk_level: str
    analysis_status: str
    summary: str
