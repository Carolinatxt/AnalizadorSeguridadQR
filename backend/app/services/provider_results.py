from dataclasses import dataclass, field
from typing import Literal

ProviderStatus = Literal[
    "ok",
    "config_error",
    "http_error",
    "api_error",
    "parse_error",
    "internal_error",
    "timeout_error",
]


@dataclass(frozen=True)
class WebRiskResult:
    provider_status: ProviderStatus
    available: bool
    match_found: bool
    threat_types: tuple[str, ...] = field(default_factory=tuple)
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
        status_code: int | None = None,
        summary: str | None = None,
    ) -> "WebRiskResult":
        resolved_summary = summary
        if resolved_summary is None:
            resolved_summary = (
                f"Web Risk HTTP {status_code}"
                if status_code is not None
                else "error HTTP al consultar Web Risk"
            )
        return cls(
            provider_status="http_error",
            available=False,
            match_found=False,
            threat_types=(),
            raw_summary=resolved_summary,
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
    provider_status: ProviderStatus
    available: bool
    success: bool
    risk_score: int | None
    phishing: bool
    malware: bool
    suspicious: bool
    unsafe: bool
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
        status_code: int | None = None,
        summary: str | None = None,
    ) -> "IpqsResult":
        resolved_summary = summary
        if resolved_summary is None:
            resolved_summary = (
                f"IPQS HTTP {status_code}"
                if status_code is not None
                else "error HTTP al consultar IPQS"
            )
        return cls(
            provider_status="http_error",
            available=False,
            success=False,
            risk_score=None,
            phishing=False,
            malware=False,
            suspicious=False,
            unsafe=False,
            raw_summary=resolved_summary,
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
            raw_summary="analisis completado",
        )
