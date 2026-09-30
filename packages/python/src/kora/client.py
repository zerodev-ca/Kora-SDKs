import json
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
        machine_id: Optional[str] = None
    ):
        self.url = url.rstrip("/")
        self.product = product
        self.key = key
        self.public_key = public_key
        self.signature_header = signature_header
        self.heartbeat_interval_sec = heartbeat_interval_sec
        self.machine_id = machine_id
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

    def validate(self, custom_key: Optional[str] = None, extra: Optional[Dict[str, Any]] = None) -> ValidationResponse:
        target_key = custom_key if custom_key else self.key
        if not target_key:
            raise ValueError("License key is required")

        payload = {
            "license_key": target_key,
            "product_name": self.product,
            "hwid": self.hwid(),
            "identifier": self.hwid()
        }
        if self.session_token:
            payload["session_id"] = self.session_token
        if extra:
            payload.update(extra)

        target_url = f"{self.url}/licenses/validate"
        response = requests.post(target_url, json=payload, headers={"Accept": "application/json"}, timeout=15)
        text = response.text

        if self.public_key:
            sig = response.headers.get(self.signature_header)
            if not sig or not self.signature_handler.verify(text, sig, self.public_key):
                raise ValueError("Response signature verification failed")

        data = response.json()
        session_info = data.get("session")
        session_token = session_info.get("token") if isinstance(session_info, dict) else None
        if session_token:
            self.session_token = session_token
            if self.heartbeat_interval_sec > 0 and self.heartbeat_runner is None:
                self.heartbeat_runner = Heartbeat(self.heartbeat_interval_sec, lambda: self.validate(target_key))
                self.heartbeat_runner.start()

        return ValidationResponse(
            valid=data.get("valid", False),
            message=data.get("message", ""),
            product=data.get("product"),
            user=data.get("user"),
            status=data.get("status"),
            expires_at=data.get("expires_at"),
            addons=data.get("addons"),
            nonce=data.get("nonce"),
            timestamp=data.get("timestamp"),
            variables=data.get("variables"),
            user_variables=data.get("user_variables"),
            session_token=session_token
        )

    def stop(self) -> None:
        if self.heartbeat_runner is not None:
            self.heartbeat_runner.stop()
            self.heartbeat_runner = None
