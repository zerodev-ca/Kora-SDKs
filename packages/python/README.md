# zerodev-kora

Official Python SDK for Kora licensing.

## Installation

```bash
pip install zerodev-kora
```

## Usage

```python
import os
from kora import Client

client = Client(
    url="https://api.yourdomain.com/api/v1",
    product="MySoftware",
    key=os.environ["LICENSE_KEY"],
    public_key="-----BEGIN PUBLIC KEY-----\n...",
    heartbeat_interval_sec=300
)

result = client.validate()
if result.valid:
    print(f"Licensed to {result.user} with {result.features}")
else:
    print(f"{result.code}: {result.message}")
```

`url` is your Kora API address followed by `/api/v1`. With `public_key` set, every answer is checked for a valid `x-kora-signature`, the nonce the client sent, and a timestamp within `max_skew_sec` (five minutes by default).

## Releasing the device

```python
client.deactivate()
```

## Offline licenses

```python
import json

with open("license.json", "w") as file:
    json.dump(client.request_offline(), file)

with open("license.json") as file:
    offline = client.verify_offline(file.read())
if offline is not None:
    print("Offline license valid until", offline["expires_at"] + offline["grace_period_ms"])
```

## Updates

```python
update = client.check_update("1.4.2")
if update.get("update_available"):
    print(update["latest"]["version"], update["download_url"])
```
