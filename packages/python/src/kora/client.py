import re
from typing import Any, Dict, Optional

import requests

from .error import KoraError
from .resource import Resource
from .signature import Signature
from .types import ValidationResponse

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

    def validate(self, key: str, product: Optional[str] = None) -> ValidationResponse:
        name = product if product is not None else self.product
        response = self._send("POST", "/licenses/validate", {"license_key": key, "product_name": name} if name is not None else {"license_key": key})
        if response.status_code == 429 or response.status_code >= 500:
            self._fail(response)
        if self.public_key and not self.signature.verify(response.text, response.headers.get("x-kora-signature"), self.public_key):
            raise KoraError("Response signature verification failed", response.status_code)
        data = self._data(response)
        return ValidationResponse(valid=data["valid"] is True, code=data["code"], message=data["message"], license=data["license"], timestamp=data["timestamp"])
