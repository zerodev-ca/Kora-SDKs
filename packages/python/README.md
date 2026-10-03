# zerodev-kora

Official Python SDK for Kora licensing.

## Installation

```bash
pip install zerodev-kora
```

## Usage

```python
import os, sys
from kora import Client

client = Client(
    url="https://api.yourdomain.com",
    product="MySoftware",
    key=os.environ["LICENSE_KEY"]
)

result = client.validate()
if not result.valid:
    sys.exit(result.message)

print("Licensed to", result.license["customer"]["name"])
```

`url` is your Kora API address. `result` holds `valid`, `message`, `code` (the HTTP status), `license` and the full answer in `raw`.

## Options

| Option | Description |
|---|---|
| `api_key` | Sent as `Authorization: Bearer`. Needed when Require API Key is on. |
| `interval_sec` | Re-check the license on this interval after a valid result. |
| `on_invalid` | Called with the result when a re-check fails. |
| `timeout_sec` | Request timeout, 15 seconds by default. |

Call `client.stop()` to stop re-checking.
