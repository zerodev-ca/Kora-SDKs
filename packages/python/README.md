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
    key=os.environ["LICENSE_KEY"],
    public_key=os.environ["KORA_PUBLIC_KEY"]
)

result = client.validate()
if not result.valid:
    sys.exit(result.message)
```

`result` holds `valid`, `message`, `code` (the HTTP status), `license` and the full answer in `raw`. With `public_key` set, every answer must carry a valid `x-kora-signature`, the nonce the client sent and a recent timestamp, or the call raises.

## Options

| Option | Description |
|---|---|
| `api_key` | Sent as `Authorization: Bearer`. Needed when Require API Key is on. |
| `public_key` | Kora's Ed25519 public key. Turns on signature checks. |
| `hwid` | Override the hardware ID, which is otherwise a hash of this machine. |
| `version` | Your product version, shown on the device in Kora. |
| `interval_sec` | Re-check the license on this interval after a valid result. |
| `on_invalid` | Called with the result when a re-check fails. |
| `timeout_sec` / `max_skew_sec` | Request timeout and allowed clock drift. |

## Offline licenses

```python
import json

answer = client.offline()
with open("license.json", "w") as file:
    json.dump(answer.offline, file)

with open("license.json") as file:
    if client.verify_offline(file.read()) is None:
        sys.exit("License file is invalid or expired")
```

## Updates

```python
update = client.check_update("1.4.2", "stable")
if update.update_available:
    print(update.latest["version"], update.download_url)
```

Call `client.stop()` to stop re-checking.
