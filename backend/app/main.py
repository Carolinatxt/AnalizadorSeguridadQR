from fastapi import FastAPI

app = FastAPI(title="AnalizadorSeguridadQR API")


@app.get("/api/v1/health")
def health():
    return {"status": "ok", "service": "backend"}
