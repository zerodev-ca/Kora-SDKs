from dataclasses import dataclass
from typing import Optional, Dict, Any, List

@dataclass
class ValidationResponse:
    valid: bool
    message: str
    product: Optional[str] = None
    user: Optional[str] = None
    status: Optional[str] = None
    expires_at: Optional[str] = None
    addons: Optional[List[str]] = None
    nonce: Optional[str] = None
    timestamp: Optional[int] = None
    variables: Optional[Dict[str, Any]] = None
    user_variables: Optional[Dict[str, Any]] = None
    session_token: Optional[str] = None
