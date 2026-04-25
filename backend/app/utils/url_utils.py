from urllib.parse import urlsplit, urlunsplit


def remove_url_fragment(url: str) -> str:
    parsed = urlsplit(url)
    return urlunsplit((parsed.scheme, parsed.netloc, parsed.path, parsed.query, ""))
