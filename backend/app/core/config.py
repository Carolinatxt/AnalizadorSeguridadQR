import os
from dotenv import load_dotenv

load_dotenv()

WEBRISK_API_KEY = (
    os.getenv("WEBRISK_API_KEY")
    or os.getenv("GOOGLE_WEB_RISK_API_KEY")
    or ""
)
IPQS_API_KEY = os.getenv("IPQS_API_KEY", "")
