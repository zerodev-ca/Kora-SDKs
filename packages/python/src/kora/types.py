from dataclasses import dataclass, field
from typing import Optional, Dict, Any

@dataclass
class ValidationResponse:
    valid: bool
    message: str
    code: int
    license: Optional[Dict[str, Any]] = None
    nonce: Optional[str] = None
    timestamp: Optional[int] = None
    raw: Dict[str, Any] = field(default_factory=dict)

@dataclass
class OfflineResponse(ValidationResponse):
    offline: Optional[Dict[str, Any]] = None

@dataclass
class UpdateResponse(ValidationResponse):
    update_available: bool = False
    current: Optional[str] = None
    latest: Optional[Dict[str, Any]] = None
    download_url: Optional[str] = None
