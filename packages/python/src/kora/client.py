import base64
import json
import re
from datetime import datetime, timezone
from typing import Any, Dict, Optional, Union
from urllib.parse import quote

import requests

from .error import KoraError
from .resource import Resource
from .signature import Signature
from .types import DeactivationResponse, ValidationResponse

class Client:
    def __init__(self, url: str, api_key: Optional[str] = None, product: Optional[str] = None, public_key: Optional[str] = None, timeout_sec: float = 15.0):
        self.url = re.sub(r"/api/v1$", "", url.rstrip("/"))
        self.api_key = api_key
        self.product = product
        self.public_key = public_key
        self.timeout_sec = timeout_sec
        self.signature = Signature()
        self.licenses = Resource(self, "licenses", "license")
        self.products = Resource(self, "products", "product")
        self.customers = Resource(self, "customers", "customer")

    def _send(self, method: str, path: str, body: Optional[Dict[str, Any]] = None, params: Optional[Dict[str, Any]] = None) -> requests.Response:
        headers = {"Accept": "application/json"}
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"
        return requests.request(method, f"{self.url}/api/v1{path}", json=body, params=params, headers=headers, timeout=self.timeout_sec)

    def _data(self, response: requests.Response) -> Dict[str, Any]:
        try:
            data = response.json()
        except ValueError:
            return {}
        return data if isinstance(data, dict) else {}

    def _fail(self, response: requests.Response) -> None:
        error = self._data(response).get("error")
        raise KoraError(error if isinstance(error, str) else f"Kora answered with status {response.status_code}", response.status_code)

    def call(self, method: str, path: str, body: Optional[Dict[str, Any]] = None, params: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        response = self._send(method, path, body, params)
        if response.status_code >= 400:
            self._fail(response)
        return self._data(response)

    def _check(self, path: str, body: Dict[str, Any]) -> Dict[str, Any]:
        response = self._send("POST", path, body)
        if response.status_code == 429 or response.status_code >= 500:
            self._fail(response)
        if self.public_key and not self.signature.verify(response.text, response.headers.get("x-kora-signature"), self.public_key):
            raise KoraError("Response signature verification failed", response.status_code)
        return self._data(response)

    def _body(self, key: str, product: Optional[str], fields: Dict[str, Any]) -> Dict[str, Any]:
        name = product if product is not None else self.product
        return {**fields, "license_key": key, **({"product_name": name} if name is not None else {})}

    def _validation(self, data: Dict[str, Any]) -> ValidationResponse:
        return ValidationResponse(valid=data["valid"] is True, code=data["code"], message=data["message"], license=data["license"], timestamp=data["timestamp"])

    def validate(self, key: str, product: Optional[str] = None, **fields: Any) -> ValidationResponse:
        return self._validation(self._check("/licenses/validate", self._body(key, product, fields)))

    def activate(self, key: str, product: Optional[str] = None, **fields: Any) -> ValidationResponse:
        return self._validation(self._check("/licenses/activate", self._body(key, product, fields)))

    def deactivate(self, key: str, **fields: Any) -> DeactivationResponse:
        data = self._check("/licenses/deactivate", {**fields, "license_key": key})
        return DeactivationResponse(success=data["success"] is True, code=data["code"], message=data["message"], removed=data["removed"], timestamp=data["timestamp"])

    def file(self, id: Union[int, str]) -> str:
        response = self._send("GET", f"/licenses/{quote(str(id), safe='')}/file")
        if response.status_code >= 400:
            self._fail(response)
        return response.text.strip()

    def offline(self, file: str) -> Dict[str, Any]:
        if not self.public_key:
            raise KoraError("A public_key is required to read offline licenses", 0)
        parts = file.strip().split(".")
        if len(parts) != 2:
            raise KoraError("Offline license file is malformed", 0)
        payload = base64.urlsafe_b64decode(parts[0] + "=" * (-len(parts[0]) % 4)).decode("utf-8")
        signature = base64.b64encode(base64.urlsafe_b64decode(parts[1] + "=" * (-len(parts[1]) % 4))).decode("ascii")
        if not self.signature.verify(payload, signature, self.public_key):
            raise KoraError("Offline license signature verification failed", 0)
        license = json.loads(payload)
        now = datetime.now(timezone.utc)
        if datetime.fromisoformat(license["refresh_after"].replace("Z", "+00:00")) < now:
            raise KoraError("Offline license needs a refresh", 0)
        if license["expires_at"] is not None and datetime.fromisoformat(license["expires_at"].replace("Z", "+00:00")) < now:
            raise KoraError("Offline license expired", 0)
        return license

    def merge(self, customer: Union[int, str], duplicate: Union[int, str]) -> Dict[str, Any]:
        return self.call("POST", f"/customers/{quote(str(customer), safe='')}/merge", {"customer_id": duplicate})["customer"]
