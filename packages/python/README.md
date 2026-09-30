# zerodev-kora

Official Python SDK for Kora licensing.

## Installation

```bash
pip install zerodev-kora
```

## Usage

```python
from kora import KoraClient

client = KoraClient(
    server_url="https://api.yourdomain.com",
    product="MySoftware",
    public_key="-----BEGIN PUBLIC KEY-----\n..."
)

result = client.validate("XXXXX-XXXXX-XXXXX-XXXXX")
if result.valid:
    print(f"License valid for: {result.user}")
else:
    print(f"Validation failed: {result.message}")
```

## Offline Verification

```python
offline = client.verify_offline(signed_offline_token)
if offline.valid:
    print("Offline license is valid!")
```
