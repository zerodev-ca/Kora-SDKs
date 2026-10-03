import json
import re
import secrets
import time
from typing import Optional, Callable, Dict, Any, Tuple, Union

import requests

from .heartbeat import Heartbeat
from .machine import Machine
from .signature import Signature
from .types import ValidationResponse, OfflineResponse, UpdateResponse

class Client:
    def __init__(
        self,
        url: str,
        product: str,
        key: Optional[str] = None,
        api_key: Optional[str] = None,
        public_key: Optional[str] = None,
        hwid: Optional[str] = None,
        version: Optional[str] = None,
        interval_sec: float = 0.0,
        timeout_sec: float = 15.0,
        max_skew_sec: float = 300.0,
        on_invalid: Optional[Callable[[ValidationResponse], None]] = None
    ):
        self.url = re.sub(r"/api/v1$", "", url.rstrip("/"))
        self.product = product
        self.key = key
        self.api_key = api_key
        self.public_key = public_key
        self.machine_id = hwid
        self.version = version
        self.interval_sec = interval_sec
        self.timeout_sec = timeout_sec
        self.max_skew_sec = max_skew_sec
        self.on_invalid = on_invalid
        self.machine = Machine()
        self.signature = Signature()
        self.heartbeat_runner = None

    def hwid(self) -> str:
        return self.machine_id if self.machine_id else self.machine.hwid()

    def _key(self, custom_key: Optional[str]) -> str:
        target_key = custom_key if custom_key is not None else self.key
        if not target_key:
            raise ValueError("License key is required")
        return target_key

    def _post(self, path: str, extra: Dict[str, Any], target_key: str) -> Tuple[int, Dict[str, Any]]:
        nonce = secrets.token_hex(16)
        payload = {"license_key": target_key, "product_name": self.product, "hwid": self.hwid(), "device_name": self.machine.name(), "platform": self.machine.platform(), "nonce": nonce, **extra}
        if self.version:
            payload["version"] = payload.get("version", self.version)
        headers = {"Accept": "application/json"}
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"
        response = requests.post(f"{self.url}/api/v1{path}", json=payload, headers=headers, timeout=self.timeout_sec)
        if self.public_key:
            self._check(response.text, response.headers.get("x-kora-signature"), nonce)
        try:
            data = response.json()
        except ValueError:
            data = {}
        return response.status_code, data if isinstance(data, dict) else {}

    def _check(self, text: str, signature: Optional[str], nonce: str) -> None:
        if not signature or not self.signature.verify(text, signature, self.public_key):
            raise ValueError("Response signature verification failed")
        data = json.loads(text)
        if data.get("nonce") != nonce:
            raise ValueError("Response does not belong to this request")
        stamp = data.get("timestamp")
        if not isinstance(stamp, (int, float)) or abs(time.time() * 1000 - stamp) > self.max_skew_sec * 1000:
            raise ValueError("Response is too old, check this machine's clock")

    def _fields(self, status: int, data: Dict[str, Any]) -> Dict[str, Any]:
        message = data.get("message") if isinstance(data.get("message"), str) else data.get("error") if isinstance(data.get("error"), str) else f"Kora answered with status {status}"
        return {"valid": data.get("valid") is True, "message": message, "code": status, "license": data.get("license"), "nonce": data.get("nonce"), "timestamp": data.get("timestamp"), "raw": data}

    def validate(self, custom_key: Optional[str] = None) -> ValidationResponse:
        target_key = self._key(custom_key)
        result = ValidationResponse(**self._fields(*self._post("/licenses/validate", {}, target_key)))
        if result.valid:
            self._watch(target_key)
        return result

    def offline(self, custom_key: Optional[str] = None) -> OfflineResponse:
        status, data = self._post("/licenses/offline", {}, self._key(custom_key))
        return OfflineResponse(**self._fields(status, data), offline=data.get("offline"))

    def verify_offline(self, content: Union[str, Dict[str, Any]]) -> Optional[Dict[str, Any]]:
        if not self.public_key:
            raise ValueError("A public key is required to verify offline licenses")
        try:
            parsed = json.loads(content) if isinstance(content, str) else dict(content)
            fields = {name: value for name, value in parsed.items() if name != "signature"}
            if not self.signature.verify(self.signature.canonical(fields), parsed["signature"], self.public_key):
                return None
            if str(fields["product"]).lower() != self.product.lower():
                return None
            if fields["hwid"] and fields["hwid"] != self.hwid():
                return None
            if time.time() * 1000 > fields["expires_at"] + fields["grace_period_ms"]:
                return None
            return parsed
        except Exception:
            return None

    def check_update(self, version: str, channel: str = "stable", custom_key: Optional[str] = None) -> UpdateResponse:
        status, data = self._post("/updates/check", {"version": version, "channel": channel}, self._key(custom_key))
        return UpdateResponse(**self._fields(status, data), update_available=data.get("update_available") is True, current=data.get("current"), latest=data.get("latest"), download_url=data.get("download_url"))

    def _watch(self, target_key: str) -> None:
        if self.interval_sec <= 0 or self.heartbeat_runner is not None:
            return
        self.heartbeat_runner = Heartbeat(self.interval_sec, lambda: self._recheck(target_key))
        self.heartbeat_runner.start()

    def _recheck(self, target_key: str) -> None:
        result = self.validate(target_key)
        if not result.valid and self.on_invalid is not None:
            self.on_invalid(result)

    def stop(self) -> None:
        if self.heartbeat_runner is not None:
            self.heartbeat_runner.stop()
            self.heartbeat_runner = None
