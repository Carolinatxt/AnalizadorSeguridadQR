import os
from dotenv import load_dotenv

load_dotenv()

WEBRISK_API_KEY = os.getenv("WEBRISK_API_KEY", "")
IPQS_API_KEY = os.getenv("IPQS_API_KEY", "")