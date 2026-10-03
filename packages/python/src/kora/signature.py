import base64
import json
from typing import Any, Dict

from cryptography.hazmat.primitives.serialization import load_pem_public_key

class Signature:
    def verify(self, body: str, signature: str, public_key: str) -> bool:
        try:
            load_pem_public_key(public_key.encode("utf-8")).verify(base64.b64decode(signature), body.encode("utf-8"))
            return True
        except Exception:
            return False

    def canonical(self, value: Dict[str, Any]) -> str:
        return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False)
