# zerodev-kora

Official Python SDK for Kora.

```bash
pip install zerodev-kora
```

## Validation

```python
from kora import Client

kora = Client("https://kora.example.com", product="MyApp", public_key=open("kora.pem").read())

result = kora.validate("XXXXX-XXXXX-XXXXX-XXXXX")
if not result.valid:
    raise SystemExit(result.message)
```

`result` holds `valid`, `code`, `message`, `license` and `timestamp`. With `public_key` set, an answer without a valid `x-kora-signature` raises `KoraError`.

## Licenses, products and customers

```python
kora = Client("https://kora.example.com", api_key="kora_...")

product = kora.products.create(name="MyApp", version="1.0.0")
customer = kora.customers.create(name="Jane", email="jane@example.com")
license = kora.licenses.create(product_id=product["id"], customer_id=customer["id"])

page = kora.licenses.list(status="active", limit=25)
kora.licenses.update(license["id"], status="suspended")
kora.licenses.delete(license["id"])
```

Every resource has `list`, `get`, `create`, `update` and `delete`. `delete(id, force=True)` forces removal of products and customers that still have licenses. Failed requests raise `KoraError` with `status`.

## Options

| Argument | Description |
|---|---|
| `url` | Kora API address. |
| `api_key` | API token, sent as `Authorization: Bearer`. |
| `product` | Default product name for `validate`. |
| `public_key` | Kora's Ed25519 public key. Turns on signature checks. |
| `timeout_sec` | Request timeout. Defaults to 15. |
