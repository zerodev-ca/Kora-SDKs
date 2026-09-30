import base64
from cryptography.hazmat.primitives.serialization import load_pem_public_key
from cryptography.exceptions import InvalidSignature

class Signature:
    def verify(self, body: str, signature_base64: str, public_key_pem: str) -> bool:
        try:
            key = load_pem_public_key(public_key_pem.encode("utf-8"))
            signature = base64.b64decode(signature_base64)
            key.verify(signature, body.encode("utf-8"))
            return True
        except (InvalidSignature, Exception):
            return False
