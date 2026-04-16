from urllib.parse import quote, urlsplit, urlunsplit

import httpx

from app.core.config import WEBRISK_API_KEY


def remove_url_fragment(url: str) -> str:
    parsed = urlsplit(url)
    return urlunsplit((parsed.scheme, parsed.netloc, parsed.path, parsed.query, ""))


async def check_url_with_web_risk(url: str) -> dict:
    if not WEBRISK_API_KEY:
        return {
            "provider_status": "error",
            "available": False,
            "match_found": False,
            "threat_types": [],
            "raw_summary": "WEBRISK_API_KEY no configurada"
        }

    analysis_url = remove_url_fragment(url)
    encoded_url = quote(analysis_url, safe="")

    endpoint = (
        "https://webrisk.googleapis.com/v1/uris:search"
        f"?key={WEBRISK_API_KEY}"
        f"&uri={encoded_url}"
        "&threatTypes=MALWARE"
        "&threatTypes=SOCIAL_ENGINEERING"
        "&threatTypes=UNWANTED_SOFTWARE"
    )

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(endpoint)

        if response.status_code != 200:
            return {
                "provider_status": "error",
                "available": False,
                "match_found": False,
                "threat_types": [],
                "raw_summary": f"Web Risk HTTP {response.status_code}"
            }

        data = response.json()

        threat = data.get("threat")
        threat_types = threat.get("threatTypes", []) if threat else []

        return {
            "provider_status": "ok",
            "available": True,
            "match_found": len(threat_types) > 0,
            "threat_types": threat_types,
            "raw_summary": "match encontrado" if threat_types else "sin coincidencia"
        }

    except Exception as exc:
        return {
            "provider_status": "error",
            "available": False,
            "match_found": False,
            "threat_types": [],
            "raw_summary": f"Error Web Risk: {str(exc)}"
        }