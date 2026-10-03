from dataclasses import dataclass, field
from typing import Optional, Dict, Any

@dataclass
class ValidationResponse:
    valid: bool
    message: str
    code: int
    license: Optional[Dict[str, Any]] = None
    raw: Dict[str, Any] = field(default_factory=dict)
