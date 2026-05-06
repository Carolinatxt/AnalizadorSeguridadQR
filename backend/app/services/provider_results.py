from dataclasses import dataclass
from typing import Literal

ProviderStatus = Literal[
    "ok",
    "config_error",
    "http_error",
    "network_error",
    "api_error",
    "parse_error",
    "internal_error",
    "timeout_error",
]
OpenPhishProviderStatus = Literal[
    "ok",
    "disabled",
    "db_missing",
    "db_error",
    "internal_error",
]
OpenPhishMatchType = Literal["exact_url", "exact_host", "none"]


@dataclass(frozen=True)
class WebRiskResult:
    """Resultado interno de proveedor Web Risk.

    Nota: available=False significa que el resultado no es utilizable
    para la decisión final (p.ej. config/http/network/parse/api_error),
    no necesariamente que el proveedor este completamente caido.
    """

    provider_status: ProviderStatus
    available: bool
    match_found: bool
    threat_types: tuple[str, ...] = ()
    raw_summary: str = ""

    @classmethod
    def from_config_error(cls, reason: str) -> "WebRiskResult":
        return cls(
            provider_status="config_error",
            available=False,
            match_found=False,
            threat_types=(),
            raw_summary=reason,
        )

    @classmethod
    def from_http_error(
        cls,
        status_code: int,
    ) -> "WebRiskResult":
        return cls(
            provider_status="http_error",
            available=False,
            match_found=False,
            threat_types=(),
            raw_summary=f"Web Risk HTTP {status_code}",
        )

    @classmethod
    def from_parse_error(
        cls,
        summary: str = "respuesta no valida del proveedor",
    ) -> "WebRiskResult":
        return cls(
            provider_status="parse_error",
            available=False,
            match_found=False,
            threat_types=(),
            raw_summary=summary,
        )

    @classmethod
    def from_network_error(
        cls,
        summary: str = "error de red o timeout de transporte",
    ) -> "WebRiskResult":
        return cls(
            provider_status="network_error",
            available=False,
            match_found=False,
            threat_types=(),
            raw_summary=summary,
        )

    @classmethod
    def from_internal_error(cls, summary: str = "error de consulta") -> "WebRiskResult":
        return cls(
            provider_status="internal_error",
            available=False,
            match_found=False,
            threat_types=(),
            raw_summary=summary,
        )

    @classmethod
    def from_timeout_error(cls, summary: str = "timeout total") -> "WebRiskResult":
        return cls(
            provider_status="timeout_error",
            available=False,
            match_found=False,
            threat_types=(),
            raw_summary=summary,
        )

    @classmethod
    def from_success(cls, threat_types: list[str]) -> "WebRiskResult":
        normalized = tuple(threat_types)
        return cls(
            provider_status="ok",
            available=True,
            match_found=len(normalized) > 0,
            threat_types=normalized,
            raw_summary="match encontrado" if normalized else "sin coincidencia",
        )


@dataclass(frozen=True)
class IpqsResult:
    """Resultado interno de proveedor IPQS.

    Nota: available=False significa que el resultado no es utilizable
    para la decisión final (incluyendo api_error), no necesariamente
    indisponibilidad técnica total del proveedor.
    """

    provider_status: ProviderStatus
    available: bool
    success: bool
    risk_score: int | None
    phishing: bool
    malware: bool
    suspicious: bool
    unsafe: bool
    parking: bool | None = None
    spamming: bool | None = None
    domain_age_human: str | None = None
    raw_summary: str = ""

    @classmethod
    def from_config_error(cls, reason: str) -> "IpqsResult":
        return cls(
            provider_status="config_error",
            available=False,
            success=False,
            risk_score=None,
            phishing=False,
            malware=False,
            suspicious=False,
            unsafe=False,
            raw_summary=reason,
        )

    @classmethod
    def from_http_error(
        cls,
        status_code: int,
    ) -> "IpqsResult":
        return cls(
            provider_status="http_error",
            available=False,
            success=False,
            risk_score=None,
            phishing=False,
            malware=False,
            suspicious=False,
            unsafe=False,
            raw_summary=f"IPQS HTTP {status_code}",
        )

    @classmethod
    def from_parse_error(
        cls,
        summary: str = "respuesta no valida del proveedor",
    ) -> "IpqsResult":
        return cls(
            provider_status="parse_error",
            available=False,
            success=False,
            risk_score=None,
            phishing=False,
            malware=False,
            suspicious=False,
            unsafe=False,
            raw_summary=summary,
        )

    @classmethod
    def from_network_error(
        cls,
        summary: str = "error de red o timeout de transporte",
    ) -> "IpqsResult":
        return cls(
            provider_status="network_error",
            available=False,
            success=False,
            risk_score=None,
            phishing=False,
            malware=False,
            suspicious=False,
            unsafe=False,
            raw_summary=summary,
        )

    @classmethod
    def from_internal_error(cls, summary: str = "error de consulta") -> "IpqsResult":
        return cls(
            provider_status="internal_error",
            available=False,
            success=False,
            risk_score=None,
            phishing=False,
            malware=False,
            suspicious=False,
            unsafe=False,
            raw_summary=summary,
        )

    @classmethod
    def from_timeout_error(cls, summary: str = "timeout total") -> "IpqsResult":
        return cls(
            provider_status="timeout_error",
            available=False,
            success=False,
            risk_score=None,
            phishing=False,
            malware=False,
            suspicious=False,
            unsafe=False,
            raw_summary=summary,
        )

    @classmethod
    def from_api_error(
        cls,
        risk_score: int | None,
        phishing: bool,
        malware: bool,
        suspicious: bool,
        unsafe: bool,
        parking: bool | None = None,
        spamming: bool | None = None,
        domain_age_human: str | None = None,
    ) -> "IpqsResult":
        return cls(
            provider_status="api_error",
            available=False,
            success=False,
            risk_score=risk_score,
            phishing=phishing,
            malware=malware,
            suspicious=suspicious,
            unsafe=unsafe,
            parking=parking,
            spamming=spamming,
            domain_age_human=domain_age_human,
            raw_summary="respuesta de proveedor sin exito",
        )

    @classmethod
    def from_success(
        cls,
        risk_score: int | None,
        phishing: bool,
        malware: bool,
        suspicious: bool,
        unsafe: bool,
        parking: bool | None = None,
        spamming: bool | None = None,
        domain_age_human: str | None = None,
    ) -> "IpqsResult":
        return cls(
            provider_status="ok",
            available=True,
            success=True,
            risk_score=risk_score,
            phishing=phishing,
            malware=malware,
            suspicious=suspicious,
            unsafe=unsafe,
            parking=parking,
            spamming=spamming,
            domain_age_human=domain_age_human,
            raw_summary="analisis completado",
        )


@dataclass(frozen=True)
class OpenPhishResult:
    """Resultado interno de OpenPhish.

    OpenPhish aporta cobertura especifica sobre phishing conocido y documentado,
    pero no sustituye Web Risk ni IPQS, que siguen cubriendo otras amenazas
    como malware, software no deseado o reputacion contextual.

    Nota: available=True significa que la SQLite local pudo consultarse.
    No implica que haya coincidencia, ni que el dato sea reciente.
    """

    provider_status: OpenPhishProviderStatus
    available: bool
    match_found: bool
    match_type: OpenPhishMatchType
    age_days: int | None = None
    brand: str | None = None
    sector: str | None = None
    family_id: str | None = None
    is_spear: bool | None = None
    raw_summary: str = ""

    @classmethod
    def from_disabled(cls) -> "OpenPhishResult":
        return cls(
            provider_status="disabled",
            available=False,
            match_found=False,
            match_type="none",
            raw_summary="OpenPhish deshabilitado",
        )

    @classmethod
    def from_db_missing(cls) -> "OpenPhishResult":
        return cls(
            provider_status="db_missing",
            available=False,
            match_found=False,
            match_type="none",
            raw_summary="base SQLite de OpenPhish no disponible",
        )

    @classmethod
    def from_db_error(
        cls,
        summary: str = "error al consultar SQLite de OpenPhish",
    ) -> "OpenPhishResult":
        return cls(
            provider_status="db_error",
            available=False,
            match_found=False,
            match_type="none",
            raw_summary=summary,
        )

    @classmethod
    def from_internal_error(
        cls,
        summary: str = "error interno al consultar OpenPhish",
    ) -> "OpenPhishResult":
        return cls(
            provider_status="internal_error",
            available=False,
            match_found=False,
            match_type="none",
            raw_summary=summary,
        )

    @classmethod
    def from_success(
        cls,
        *,
        match_found: bool,
        match_type: OpenPhishMatchType = "none",
        age_days: int | None = None,
        brand: str | None = None,
        sector: str | None = None,
        family_id: str | None = None,
        is_spear: bool | None = None,
    ) -> "OpenPhishResult":
        resolved_match_type: OpenPhishMatchType = match_type if match_found else "none"
        return cls(
            provider_status="ok",
            available=True,
            match_found=match_found,
            match_type=resolved_match_type,
            age_days=age_days,
            brand=brand,
            sector=sector,
            family_id=family_id,
            is_spear=is_spear,
            raw_summary="match encontrado" if match_found else "sin coincidencia",
        )
