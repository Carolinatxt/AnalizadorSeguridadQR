from __future__ import annotations

import ipaddress
from dataclasses import dataclass
from difflib import SequenceMatcher
from typing import Literal
from urllib.parse import unquote, urlsplit

try:
    import tldextract
except ImportError:  # pragma: no cover - fallback defensivo si falta la dependencia
    tldextract = None


LocalHeuristicCode = Literal[
    "PUBLIC_IP_HOST",
    "URL_SHORTENER",
    "EXTREME_HOST_COMPLEXITY",
    "EMBEDDED_BRAND",
    "LOOKALIKE_BRAND",
    "SUSPICIOUS_KEYWORD_HOST",
    "SUSPICIOUS_KEYWORD_PATH",
]

LocalHeuristicSeverity = Literal["high", "medium", "low"]


@dataclass(frozen=True)
class LocalHeuristicSignal:
    code: LocalHeuristicCode
    severity: LocalHeuristicSeverity
    reason: str


@dataclass(frozen=True)
class LocalHeuristicResult:
    available: bool
    signals: tuple[LocalHeuristicSignal, ...] = ()


@dataclass(frozen=True)
class DomainParts:
    subdomain: str
    domain: str
    suffix: str
    registered_domain: str


@dataclass(frozen=True)
class KnownBrand:
    canonical_name: str
    tokens: tuple[str, ...]
    official_domains: tuple[str, ...]


_PUBLIC_IP_HOST_REASON = (
    "El enlace usa una direccion IP en lugar de un dominio reconocible."
)
_URL_SHORTENER_REASON = "El enlace usa un acortador que oculta el destino real."
_EMBEDDED_BRAND_REASON = (
    "El dominio usa una marca conocida dentro de una direccion que no parece oficial."
)
_LOOKALIKE_BRAND_REASON = (
    "El dominio se parece visualmente al de una marca conocida."
)
_EXTREME_HOST_COMPLEXITY_REASON = "La direccion es larga o dificil de interpretar."
_SUSPICIOUS_KEYWORD_REASON = (
    "El enlace contiene terminos habituales en paginas de verificacion o inicio de sesion."
)
_KNOWN_URL_SHORTENERS = frozenset(
    {
        "bit.ly",
        "tinyurl.com",
        "t.co",
        "cutt.ly",
        "is.gd",
        "ow.ly",
        "rebrand.ly",
        "shorturl.at",
        "buff.ly",
        "goo.gl",
        "bitly.com",
    }
)
_TLD_EXTRACTOR = (
    tldextract.TLDExtract(suffix_list_urls=None) if tldextract is not None else None
)
_LOOKALIKE_CHAR_MAP = str.maketrans(
    {
        "0": "o",
        "1": "l",
        "3": "e",
        "5": "s",
        "@": "a",
        "$": "s",
    }
)
_LOOKALIKE_MIN_LENGTH = 5
_LOOKALIKE_MAX_LENGTH_DIFF = 2
_LOOKALIKE_MIN_SIMILARITY = 0.83
_LOOKALIKE_MIN_COMMON_PREFIX = 3
_COMMON_MULTI_PART_SUFFIXES = frozenset(
    {
        "co.uk",
        "com.au",
        "com.bd",
        "com.bn",
        "com.br",
        "com.co",
        "com.es",
        "com.ge",
        "com.py",
        "com.tr",
        "edu.bd",
        "org.uk",
    }
)
_KNOWN_INFRASTRUCTURE_DOMAINS = frozenset(
    {
        "cloudfront.net",
        "amazonaws.com",
        "workers.dev",
        "vercel.app",
        "netlify.app",
        "webflow.io",
        "blogspot.com",
        "github.io",
        "pages.dev",
        "azurewebsites.net",
        "firebaseapp.com",
        "appspot.com",
    }
)
_STRONG_SUSPICIOUS_KEYWORDS = frozenset(
    {
        "verify",
        "wallet",
        "invoice",
        "security",
        "account",
        "auth",
        "password",
        "billing",
        "recovery",
    }
)
_MEDIUM_SUSPICIOUS_KEYWORDS = frozenset(
    {
        "login",
        "support",
        "admin",
        "update",
        "secure",
        "confirm",
    }
)

# La lista se limita a marcas de alto impacto y frecuentes en phishing para
# mantener el sistema auditable y mantenible. No pretende ser exhaustiva.
_KNOWN_BRANDS: tuple[KnownBrand, ...] = (
    KnownBrand("Amazon", ("amazon", "aws"), ("amazon.com", "amazon.es", "aws.amazon.com")),
    KnownBrand("Meta/Facebook", ("meta", "facebook"), ("meta.com", "facebook.com")),
    KnownBrand(
        "Microsoft",
        ("microsoft", "office365", "outlook", "onedrive"),
        ("microsoft.com", "office.com", "outlook.com", "live.com", "onedrive.com"),
    ),
    KnownBrand("Google", ("google", "gmail"), ("google.com", "gmail.com")),
    KnownBrand("Apple", ("apple", "icloud"), ("apple.com", "icloud.com")),
    KnownBrand("PayPal", ("paypal",), ("paypal.com",)),
    KnownBrand("Adobe", ("adobe", "acrobat"), ("adobe.com",)),
    KnownBrand("DocuSign", ("docusign",), ("docusign.com",)),
    KnownBrand("Dropbox", ("dropbox",), ("dropbox.com",)),
    KnownBrand("LinkedIn", ("linkedin",), ("linkedin.com",)),
    KnownBrand("Netflix", ("netflix",), ("netflix.com",)),
    KnownBrand("Spotify", ("spotify",), ("spotify.com",)),
    KnownBrand("Discord", ("discord",), ("discord.com",)),
    KnownBrand("Telegram", ("telegram",), ("telegram.org", "t.me")),
    KnownBrand("WhatsApp", ("whatsapp",), ("whatsapp.com",)),
    KnownBrand("Instagram", ("instagram",), ("instagram.com",)),
    KnownBrand("TikTok", ("tiktok",), ("tiktok.com",)),
    KnownBrand("X/Twitter", ("twitter", "x"), ("twitter.com", "x.com")),
    KnownBrand("Roblox", ("roblox",), ("roblox.com",)),
    KnownBrand("Steam", ("steam",), ("steampowered.com", "steamcommunity.com")),
    KnownBrand("Coinbase", ("coinbase",), ("coinbase.com",)),
    KnownBrand("Binance", ("binance",), ("binance.com",)),
    KnownBrand("Kraken", ("kraken",), ("kraken.com",)),
    KnownBrand("Gemini", ("gemini",), ("gemini.com",)),
    KnownBrand("Ledger", ("ledger",), ("ledger.com",)),
    KnownBrand("Trezor", ("trezor",), ("trezor.io",)),
    KnownBrand("MetaMask", ("metamask",), ("metamask.io",)),
    KnownBrand("Trust Wallet", ("trustwallet",), ("trustwallet.com",)),
    KnownBrand("Exodus", ("exodus",), ("exodus.com",)),
    KnownBrand("Shopee", ("shopee",), ("shopee.com",)),
    KnownBrand("DHL", ("dhl",), ("dhl.com",)),
    KnownBrand("UPS", ("ups",), ("ups.com",)),
    KnownBrand("USPS", ("usps",), ("usps.com",)),
    KnownBrand("FedEx", ("fedex",), ("fedex.com",)),
    KnownBrand("Nubank", ("nubank",), ("nubank.com.br", "nu.com.co")),
    KnownBrand("Mercado Libre", ("mercadolibre", "mercadopago"), ("mercadolibre.com", "mercadopago.com")),
    KnownBrand("BBVA", ("bbva",), ("bbva.com", "bbva.es")),
    KnownBrand("Santander", ("santander",), ("santander.com", "santander.es")),
    KnownBrand("CaixaBank", ("caixabank",), ("caixabank.es",)),
    KnownBrand("Sabadell", ("sabadell",), ("bancsabadell.com", "sabadell.com")),
    KnownBrand("Movistar", ("movistar",), ("movistar.es", "movistar.com")),
    KnownBrand("Bizum", ("bizum",), ("bizum.es",)),
    KnownBrand("SEPE", ("sepe",), ("sepe.es",)),
    KnownBrand(
        "Agencia Tributaria",
        ("agenciatributaria", "agencia-tributaria", "aeat"),
        ("agenciatributaria.es", "aeat.es"),
    ),
    KnownBrand("Correos", ("correos",), ("correos.es",)),
)


def _normalize_hostname(hostname: str | None) -> str:
    if not hostname:
        return ""
    return hostname.rstrip(".").lower()


def extract_domain_parts(hostname: str | None) -> DomainParts:
    normalized_hostname = _normalize_hostname(hostname)
    if not normalized_hostname:
        return DomainParts("", "", "", "")

    try:
        ipaddress.ip_address(normalized_hostname)
    except ValueError:
        pass
    else:
        return DomainParts("", normalized_hostname, "", normalized_hostname)

    if _TLD_EXTRACTOR is not None:
        try:
            extracted = _TLD_EXTRACTOR(normalized_hostname)
            registered_domain = ""
            if extracted.domain and extracted.suffix:
                registered_domain = f"{extracted.domain}.{extracted.suffix}"
            elif extracted.domain:
                registered_domain = extracted.domain

            return DomainParts(
                subdomain=extracted.subdomain or "",
                domain=extracted.domain or "",
                suffix=extracted.suffix or "",
                registered_domain=registered_domain,
            )
        except Exception:
            pass

    # Fallback local y seguro si la libreria no esta disponible o falla.
    labels = [label for label in normalized_hostname.split(".") if label]
    if not labels:
        return DomainParts("", "", "", "")
    if len(labels) == 1:
        return DomainParts("", labels[0], "", labels[0])
    if len(labels) >= 3:
        candidate_suffix = f"{labels[-2]}.{labels[-1]}"
        if candidate_suffix in _COMMON_MULTI_PART_SUFFIXES:
            subdomain = ".".join(labels[:-3])
            domain = labels[-3]
            registered_domain = f"{domain}.{candidate_suffix}"
            return DomainParts(subdomain, domain, candidate_suffix, registered_domain)

    subdomain = ".".join(labels[:-2])
    domain = labels[-2]
    suffix = labels[-1]
    registered_domain = f"{domain}.{suffix}"
    return DomainParts(subdomain, domain, suffix, registered_domain)


def _host_matches_domain(hostname: str, domain: str) -> bool:
    return hostname == domain or hostname.endswith(f".{domain}")


def _registered_domain_is_known_infrastructure(registered_domain: str) -> bool:
    normalized_registered_domain = _normalize_hostname(registered_domain)
    if not normalized_registered_domain:
        return False
    return normalized_registered_domain in _KNOWN_INFRASTRUCTURE_DOMAINS


def _normalize_token_text(value: str) -> str:
    return "".join(character for character in value.lower() if character.isalnum())


def _normalize_lookalike_text(value: str) -> str:
    translated = value.lower().translate(_LOOKALIKE_CHAR_MAP)
    return "".join(character for character in translated if character.isalnum())


def _normalize_keyword_text(value: str) -> str:
    return "".join(
        character.lower() if character.isalnum() else " "
        for character in unquote(value)
    )


def _contains_keyword(text: str, keyword: str) -> bool:
    normalized_text = _normalize_keyword_text(text)
    keyword_parts = [part for part in normalized_text.split() if part]
    if keyword in keyword_parts:
        return True
    return keyword in normalized_text


def _common_prefix_length(left: str, right: str) -> int:
    size = min(len(left), len(right))
    for index in range(size):
        if left[index] != right[index]:
            return index
    return size


def _domain_is_official(registered_domain: str, official_domains: tuple[str, ...]) -> bool:
    normalized_registered_domain = _normalize_hostname(registered_domain)
    if not normalized_registered_domain:
        return False

    return any(
        normalized_registered_domain == _normalize_hostname(official_domain)
        for official_domain in official_domains
    )


def _brand_token_appears_in_text(token: str, text: str) -> bool:
    normalized_token = _normalize_token_text(token)
    normalized_text = _normalize_token_text(text)
    if not normalized_token or not normalized_text:
        return False

    if len(normalized_token) >= 4:
        return normalized_token in normalized_text

    labels = [_normalize_token_text(label) for label in text.split(".") if label]
    return any(label == normalized_token for label in labels)


def _build_embedded_brand_signal(hostname: str | None) -> LocalHeuristicSignal | None:
    normalized_hostname = _normalize_hostname(hostname)
    if not normalized_hostname:
        return None

    domain_parts = extract_domain_parts(normalized_hostname)
    registered_domain = domain_parts.registered_domain
    if not registered_domain:
        return None

    searchable_text = ".".join(
        part for part in (domain_parts.subdomain, domain_parts.domain) if part
    )
    if not searchable_text:
        return None

    for known_brand in _KNOWN_BRANDS:
        if _domain_is_official(registered_domain, known_brand.official_domains):
            continue

        if any(
            _brand_token_appears_in_text(token, searchable_text)
            for token in known_brand.tokens
        ):
            return LocalHeuristicSignal(
                code="EMBEDDED_BRAND",
                severity="high",
                reason=_EMBEDDED_BRAND_REASON,
            )

    return None


def _build_lookalike_brand_signal(hostname: str | None) -> LocalHeuristicSignal | None:
    normalized_hostname = _normalize_hostname(hostname)
    if not normalized_hostname:
        return None

    domain_parts = extract_domain_parts(normalized_hostname)
    registered_domain = domain_parts.registered_domain
    if not registered_domain or not domain_parts.domain:
        return None

    domain_core_candidates = {
        candidate
        for candidate in [domain_parts.domain, *domain_parts.domain.split("-")]
        if candidate
    }

    for known_brand in _KNOWN_BRANDS:
        if _domain_is_official(registered_domain, known_brand.official_domains):
            continue

        for token in known_brand.tokens:
            normalized_token = _normalize_lookalike_text(token)
            raw_token = _normalize_token_text(token)
            if len(normalized_token) < _LOOKALIKE_MIN_LENGTH:
                continue

            for candidate in domain_core_candidates:
                raw_candidate = _normalize_token_text(candidate)
                normalized_candidate = _normalize_lookalike_text(candidate)
                if (
                    len(raw_candidate) < _LOOKALIKE_MIN_LENGTH
                    or len(normalized_candidate) < _LOOKALIKE_MIN_LENGTH
                ):
                    continue

                if raw_candidate == raw_token:
                    continue

                if (
                    normalized_candidate == normalized_token
                    and raw_candidate != normalized_token
                ):
                    return LocalHeuristicSignal(
                        code="LOOKALIKE_BRAND",
                        severity="medium",
                        reason=_LOOKALIKE_BRAND_REASON,
                    )

                if (
                    normalized_token in normalized_candidate
                    or normalized_candidate in normalized_token
                ):
                    continue

                if abs(len(normalized_candidate) - len(normalized_token)) > _LOOKALIKE_MAX_LENGTH_DIFF:
                    continue

                similarity = SequenceMatcher(
                    None, normalized_candidate, normalized_token
                ).ratio()
                common_prefix = _common_prefix_length(
                    normalized_candidate, normalized_token
                )
                if (
                    similarity >= _LOOKALIKE_MIN_SIMILARITY
                    and common_prefix >= _LOOKALIKE_MIN_COMMON_PREFIX
                ):
                    return LocalHeuristicSignal(
                        code="LOOKALIKE_BRAND",
                        severity="medium",
                        reason=_LOOKALIKE_BRAND_REASON,
                    )

    return None


def _build_extreme_host_complexity_signal(hostname: str | None) -> LocalHeuristicSignal | None:
    normalized_hostname = _normalize_hostname(hostname)
    if not normalized_hostname:
        return None

    domain_parts = extract_domain_parts(normalized_hostname)
    if _registered_domain_is_known_infrastructure(domain_parts.registered_domain):
        return None

    labels = [label for label in normalized_hostname.split(".") if label]
    hostname_length = len(normalized_hostname)
    hyphen_count = normalized_hostname.count("-")

    if hostname_length > 45 and len(labels) > 4:
        return LocalHeuristicSignal(
            code="EXTREME_HOST_COMPLEXITY",
            severity="medium",
            reason=_EXTREME_HOST_COMPLEXITY_REASON,
        )

    if hostname_length > 40 and hyphen_count > 4:
        return LocalHeuristicSignal(
            code="EXTREME_HOST_COMPLEXITY",
            severity="medium",
            reason=_EXTREME_HOST_COMPLEXITY_REASON,
        )

    return None


def _build_suspicious_keyword_host_signal(hostname: str | None) -> LocalHeuristicSignal | None:
    normalized_hostname = _normalize_hostname(hostname)
    if not normalized_hostname:
        return None

    if any(
        _contains_keyword(normalized_hostname, keyword)
        for keyword in _STRONG_SUSPICIOUS_KEYWORDS
    ):
        return LocalHeuristicSignal(
            code="SUSPICIOUS_KEYWORD_HOST",
            severity="medium",
            reason=_SUSPICIOUS_KEYWORD_REASON,
        )

    if any(
        _contains_keyword(normalized_hostname, keyword)
        for keyword in _MEDIUM_SUSPICIOUS_KEYWORDS
    ):
        return LocalHeuristicSignal(
            code="SUSPICIOUS_KEYWORD_HOST",
            severity="low",
            reason=_SUSPICIOUS_KEYWORD_REASON,
        )

    return None


def _build_suspicious_keyword_path_signal(path: str) -> LocalHeuristicSignal | None:
    if not path:
        return None

    all_keywords = _STRONG_SUSPICIOUS_KEYWORDS | _MEDIUM_SUSPICIOUS_KEYWORDS
    if not any(_contains_keyword(path, keyword) for keyword in all_keywords):
        return None

    return LocalHeuristicSignal(
        code="SUSPICIOUS_KEYWORD_PATH",
        severity="low",
        reason=_SUSPICIOUS_KEYWORD_REASON,
    )


def _build_public_ip_host_signal(hostname: str | None) -> LocalHeuristicSignal | None:
    normalized_hostname = _normalize_hostname(hostname)
    if not normalized_hostname:
        return None

    try:
        parsed_ip = ipaddress.ip_address(normalized_hostname)
    except ValueError:
        return None

    # Esta heuristica solo marca IPs literales publicas. Las IPs privadas,
    # locales o reservadas se filtran antes en la validacion de entrada.
    if (
        parsed_ip.is_private
        or parsed_ip.is_loopback
        or parsed_ip.is_link_local
        or parsed_ip.is_reserved
        or parsed_ip.is_multicast
        or parsed_ip.is_unspecified
    ):
        return None

    return LocalHeuristicSignal(
        code="PUBLIC_IP_HOST",
        severity="medium",
        reason=_PUBLIC_IP_HOST_REASON,
    )


def _build_url_shortener_signal(hostname: str | None) -> LocalHeuristicSignal | None:
    normalized_hostname = _normalize_hostname(hostname)
    if not normalized_hostname:
        return None

    domain_parts = extract_domain_parts(normalized_hostname)
    candidate_domain = domain_parts.registered_domain or normalized_hostname

    if candidate_domain not in _KNOWN_URL_SHORTENERS and not any(
        _host_matches_domain(normalized_hostname, shortener_domain)
        for shortener_domain in _KNOWN_URL_SHORTENERS
    ):
        return None

    return LocalHeuristicSignal(
        code="URL_SHORTENER",
        severity="medium",
        reason=_URL_SHORTENER_REASON,
    )


def analyze_local_heuristics(url: str) -> LocalHeuristicResult:
    """Analiza patrones estructurales locales sin hacer red.

    Esta capa no decide si una URL es maliciosa. Solo permite detectar
    caracteristicas estructurales que, en fases posteriores, pueden impedir
    clasificar un enlace como seguro con confianza alta.
    Las heuristicas locales no afirman que una URL sea maliciosa. Detectan
    caracteristicas estructurales que impiden clasificarla como segura con
    confianza alta. En un contexto de quishing, esta politica conservadora
    reduce el riesgo de falsos negativos en URLs muy recientes que aun no
    han sido indexadas por proveedores externos.

    Reglas de esta funcion:
    - no hace peticiones de red;
    - no resuelve DNS;
    - no sigue redirecciones;
    - no abre la URL;
    - no lanza excepciones al flujo principal si puede evitarlo.
    """

    try:
        parsed = urlsplit(url)
    except ValueError:
        return LocalHeuristicResult(available=False)

    # En esta fase solo dejamos la estructura preparada. Las heuristicas
    # concretas se iran incorporando de forma incremental en las siguientes
    # fases sin tocar el contrato del backend.
    if not parsed.scheme or not parsed.netloc:
        return LocalHeuristicResult(available=False)

    try:
        hostname = parsed.hostname
    except ValueError:
        return LocalHeuristicResult(available=False)

    signals: list[LocalHeuristicSignal] = []

    public_ip_host_signal = _build_public_ip_host_signal(hostname)
    if public_ip_host_signal is not None:
        signals.append(public_ip_host_signal)

    url_shortener_signal = _build_url_shortener_signal(hostname)
    if url_shortener_signal is not None:
        signals.append(url_shortener_signal)

    embedded_brand_signal = _build_embedded_brand_signal(hostname)
    if embedded_brand_signal is not None:
        signals.append(embedded_brand_signal)

    lookalike_brand_signal = _build_lookalike_brand_signal(hostname)
    if lookalike_brand_signal is not None:
        signals.append(lookalike_brand_signal)

    extreme_host_complexity_signal = _build_extreme_host_complexity_signal(hostname)
    if extreme_host_complexity_signal is not None:
        signals.append(extreme_host_complexity_signal)

    suspicious_keyword_host_signal = _build_suspicious_keyword_host_signal(hostname)
    if suspicious_keyword_host_signal is not None:
        signals.append(suspicious_keyword_host_signal)

    suspicious_keyword_path_signal = _build_suspicious_keyword_path_signal(parsed.path)
    if suspicious_keyword_path_signal is not None:
        signals.append(suspicious_keyword_path_signal)

    return LocalHeuristicResult(
        available=True,
        signals=tuple(signals),
    )
