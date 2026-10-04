import base64
from typing import Optional

from cryptography.hazmat.primitives.serialization import load_pem_public_key

class Signature:
    def verify(self, body: str, signature: Optional[str], public_key: str) -> bool:
        if signature is None:
            return False
        try:
            load_pem_public_key(public_key.encode("utf-8")).verify(base64.b64decode(signature), body.encode("utf-8"))
            return True
        except Exception:
            return False
