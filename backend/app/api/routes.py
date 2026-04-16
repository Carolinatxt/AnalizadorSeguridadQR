from urllib.parse import urlparse

from fastapi import APIRouter, HTTPException

from app.models.schemas import AnalyzeUrlRequest, AnalyzeUrlResponse

router = APIRouter()


@router.post("/api/v1/analyze", response_model=AnalyzeUrlResponse)
def analyze_url(payload: AnalyzeUrlRequest) -> AnalyzeUrlResponse:
    url = payload.url.strip()

    if not url:
        raise HTTPException(status_code=400, detail="La URL está vacía")

    parsed = urlparse(url)

    if not parsed.scheme:
        raise HTTPException(status_code=400, detail="La URL no es válida")

    if parsed.scheme not in {"http", "https"}:
        raise HTTPException(
            status_code=400,
            detail="Solo se permiten URLs http o https"
        )

    if not parsed.hostname:
        raise HTTPException(status_code=400, detail="La URL no es válida")

    return AnalyzeUrlResponse(
        risk_level="safe",
        analysis_status="complete",
        summary="Backend conectado correctamente."
    )