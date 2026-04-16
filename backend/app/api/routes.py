from urllib.parse import urlparse

from fastapi import APIRouter, HTTPException

from app.models.schemas import AnalyzeUrlRequest, AnalyzeUrlResponse
from app.services.web_risk_service import check_url_with_web_risk

router = APIRouter()


@router.post("/api/v1/analyze", response_model=AnalyzeUrlResponse)
async def analyze_url(payload: AnalyzeUrlRequest) -> AnalyzeUrlResponse:
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

    web_risk_result = await check_url_with_web_risk(url)

    if not web_risk_result["available"]:
        return AnalyzeUrlResponse(
            risk_level="suspicious",
            analysis_status="partial",
            summary="No fue posible completar el análisis con Web Risk."
        )

    if web_risk_result["match_found"]:
        return AnalyzeUrlResponse(
            risk_level="dangerous",
            analysis_status="complete",
            summary="Google Web Risk ha detectado una amenaza en esta URL."
        )

    return AnalyzeUrlResponse(
        risk_level="safe",
        analysis_status="complete",
        summary="Google Web Risk no ha detectado amenazas conocidas en esta URL."
    )