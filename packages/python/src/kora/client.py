import secrets
import time
from typing import Optional, Dict, Any

import requests

from .signature import Signature
from .machine import Machine
from .heartbeat import Heartbeat
from .offline import Offline
from .types import ValidationResponse

class Client:
    def __init__(
        self,
        url: str,
        product: str,
        key: Optional[str] = None,
        public_key: Optional[str] = None,
        signature_header: str = "x-kora-signature",
        heartbeat_interval_sec: float = 0.0,
        machine_id: Optional[str] = None,
        max_skew_sec: float = 300.0
    ):
        self.url = url.rstrip("/")
        self.product = product
        self.key = key
        self.public_key = public_key
        self.signature_header = signature_header
        self.heartbeat_interval_sec = heartbeat_interval_sec
        self.machine_id = machine_id
        self.max_skew_sec = max_skew_sec
        self.signature_handler = Signature()
        self.machine_handler = Machine()
        self.offline_handler = Offline()
        self.heartbeat_runner = None
        self.session_token = None

    def token(self) -> Optional[str]:
        return self.session_token

    def hwid(self) -> str:
        if self.machine_id:
            return self.machine_id
        return self.machine_handler.hwid()

    def offline(self) -> Offline:
        return self.offline_handler

    def _key(self, custom_key: Optional[str]) -> str:
        target_key = custom_key if custom_key else self.key
        if not target_key:
            raise ValueError("License key is required")
        return target_key

    def _post(self, path: str, payload: Dict[str, Any]) -> Dict[str, Any]:
        nonce = payload["nonce"] if payload.get("nonce") else secrets.token_hex(16)
        response = requests.post(f"{self.url}{path}", json={**payload, "nonce": nonce}, headers={"Accept": "application/json"}, timeout=15)
        if response.status_code == 429:
            raise ValueError("The license server is rate limiting this machine")
        text = response.text
        data = response.json()
        if not self.public_key:
            return data
        sig = response.headers.get(self.signature_header)
        if not sig or not self.signature_handler.verify(text, sig, self.public_key):
            raise ValueError("Response signature verification failed")
        if data.get("nonce") != nonce:
            raise ValueError("Response does not belong to this request")
        timestamp = data.get("timestamp")
        if not isinstance(timestamp, (int, float)) or abs(time.time() * 1000 - timestamp) > self.max_skew_sec * 1000:
            raise ValueError("Response is too old, check this machine's clock")
        return data

    def _request(self, key: str, extra: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        payload = {
            "license_key": key,
            "product_name": self.product,
            "hwid": self.hwid(),
            "identifier": self.hwid()
        }
        if self.session_token:
            payload["session_id"] = self.session_token
        if extra:
            payload.update(extra)
        return payload

    def _response(self, data: Dict[str, Any]) -> ValidationResponse:
        session_info = data.get("session")
        return ValidationResponse(
            valid=data.get("valid", False),
            message=data.get("message", ""),
            code=data.get("code"),
            product=data.get("product"),
            user=data.get("user"),
            status=data.get("status"),
            expires_at=data.get("expires_at"),
            addons=data.get("addons"),
            nonce=data.get("nonce"),
            timestamp=data.get("timestamp"),
            session_token=session_info.get("token") if isinstance(session_info, dict) else None,
            raw=data
        )

    def validate(self, custom_key: Optional[str] = None, extra: Optional[Dict[str, Any]] = None) -> ValidationResponse:
        target_key = self._key(custom_key)
        result = self._response(self._post("/licenses/validate", self._request(target_key, extra)))
        if result.session_token:
            self.session_token = result.session_token
            if self.heartbeat_interval_sec > 0 and self.heartbeat_runner is None:
                self.heartbeat_runner = Heartbeat(self.heartbeat_interval_sec, lambda: self.validate(target_key))
                self.heartbeat_runner.start()
        return result

    def deactivate(self, custom_key: Optional[str] = None) -> ValidationResponse:
        result = self._response(self._post("/licenses/deactivate", self._request(self._key(custom_key))))
        self.stop()
        self.session_token = None
        return result

    def request_offline(self, custom_key: Optional[str] = None) -> Dict[str, Any]:
        data = self._post("/licenses/offline", self._request(self._key(custom_key)))
        if not data.get("valid") or "offline" not in data:
            raise ValueError(data.get("message", "Offline license refused"))
        return data["offline"]

    def verify_offline(self, content: str) -> Optional[Dict[str, Any]]:
        if not self.public_key:
            raise ValueError("A public key is required to verify offline licenses")
        return self.offline_handler.verify(content, self.public_key, self.hwid())

    def check_update(self, version: str, channel: str = "stable", custom_key: Optional[str] = None) -> Dict[str, Any]:
        return self._post("/updates/check", {"license_key": self._key(custom_key), "product_name": self.product, "version": version, "channel": channel})

    def stop(self) -> None:
        if self.heartbeat_runner is not None:
            self.heartbeat_runner.stop()
            self.heartbeat_runner = None
