from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional

@dataclass
class ValidationResponse:
    valid: bool
    code: str
    message: str
    license: Optional[Dict[str, Any]] = None
    timestamp: Optional[int] = None

@dataclass
class DeactivationResponse:
    success: bool
    code: str
    message: str
    removed: int = 0
    timestamp: Optional[int] = None

@dataclass
class Page:
    total: int
    rows: List[Dict[str, Any]] = field(default_factory=list)
