from typing import Any, Dict, Union
from urllib.parse import quote

from .types import Page

class Resource:
    def __init__(self, transport: Any, name: str, single: str):
        self.transport = transport
        self.name = name
        self.single = single

    def _path(self, id: Union[int, str]) -> str:
        return f"/{self.name}/{quote(str(id), safe='')}"

    def list(self, **query: Any) -> Page:
        data = self.transport.call("GET", f"/{self.name}", params={key: value for key, value in query.items() if value is not None})
        return Page(total=data["total"], rows=data[self.name])

    def get(self, id: Union[int, str]) -> Dict[str, Any]:
        return self.transport.call("GET", self._path(id))

    def create(self, **fields: Any) -> Dict[str, Any]:
        return self.transport.call("POST", f"/{self.name}", body=fields)[self.single]

    def update(self, id: Union[int, str], **fields: Any) -> Dict[str, Any]:
        return self.transport.call("PATCH", self._path(id), body=fields)[self.single]

    def delete(self, id: Union[int, str], force: bool = False) -> None:
        self.transport.call("DELETE", self._path(id), params={"force": "true"} if force else None)
