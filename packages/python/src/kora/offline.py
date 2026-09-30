import json
import time
from typing import Optional, Dict, Any
from .signature import Signature

class Offline:
    def __init__(self):
        self.signature = Signature()

    def verify(self, content: str, public_key_pem: str) -> Optional[Dict[str, Any]]:
        try:
            data = json.loads(content)
            if "signature" not in data or "expires_at" not in data:
                return None
            sig = data["signature"]
            copy_data = dict(data)
            del copy_data["signature"]
            serialized = json.dumps(copy_data, separators=(",", ":"), sort_keys=True)
            if not self.signature.verify(serialized, sig, public_key_pem):
                return None
            now_ms = int(time.time() * 1000)
            allowed_until = data["expires_at"] + data.get("grace_period_ms", 0)
            if now_ms > allowed_until:
                return None
            return data
        except Exception:
            return None
