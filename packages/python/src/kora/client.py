import re
from typing import Optional, Callable

import requests

from .heartbeat import Heartbeat
from .types import ValidationResponse

class Client:
    def __init__(
        self,
        url: str,
        product: str,
        key: Optional[str] = None,
        api_key: Optional[str] = None,
        interval_sec: float = 0.0,
        timeout_sec: float = 15.0,
        on_invalid: Optional[Callable[[ValidationResponse], None]] = None
    ):
        self.url = re.sub(r"/api/v1$", "", url.rstrip("/"))
        self.product = product
        self.key = key
        self.api_key = api_key
        self.interval_sec = interval_sec
        self.timeout_sec = timeout_sec
        self.on_invalid = on_invalid
        self.heartbeat_runner = None

    def endpoint(self) -> str:
        return f"{self.url}/api/v1/licenses/validate"

    def _key(self, custom_key: Optional[str]) -> str:
        target_key = custom_key if custom_key is not None else self.key
        if not target_key:
            raise ValueError("License key is required")
        return target_key

    def validate(self, custom_key: Optional[str] = None) -> ValidationResponse:
        target_key = self._key(custom_key)
        headers = {"Accept": "application/json"}
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"
        response = requests.post(self.endpoint(), json={"license_key": target_key, "product_name": self.product}, headers=headers, timeout=self.timeout_sec)
        try:
            data = response.json()
        except ValueError:
            data = {}
        if not isinstance(data, dict):
            data = {}
        message = data.get("message") if isinstance(data.get("message"), str) else data.get("error") if isinstance(data.get("error"), str) else f"Kora answered with status {response.status_code}"
        result = ValidationResponse(valid=data.get("valid") is True, message=message, code=response.status_code, license=data.get("license"), raw=data)
        if result.valid:
            self._watch(target_key)
        return result

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
