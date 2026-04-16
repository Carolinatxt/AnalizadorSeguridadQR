from fastapi import FastAPI
from app.api.routes import router

app = FastAPI(title="AnalizadorSeguridadQR API")

app.include_router(router)


@app.get("/api/v1/health")
def health():
    return {"status": "ok", "service": "backend"}