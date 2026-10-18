# zerodev-kora

Official Rust SDK for Kora. Blocking, built on `ureq`.

```bash
cargo add zerodev-kora
```

## Validation

```rust
let kora = kora::Client::new("https://kora.example.com").product("MyApp").public_key(std::env::var("KORA_PUBLIC_KEY")?);

let result = kora.validate(&license_key, &[("hwid", machine_id.as_str())])?;
if !result.valid {
    return Err(result.message.into());
}
```

With a public key set, an answer without a valid `x-kora-signature` returns `Error::Signature`. A rejected license is not an error: check `valid` and `code`.

## Devices

```rust
let activated = kora.activate(&license_key, &[("hwid", machine_id.as_str())])?;
let released = kora.deactivate(&license_key, &[("hwid", machine_id.as_str())])?;
```

`activate` takes a free seat in each whitelist group of the product, then validates. `deactivate` frees it and returns `success`, `code` and `removed`.

## Offline licenses

```rust
let file = admin.file("42")?;
let license = kora.offline(&file)?;
```

`offline` checks the file against the public key with no network call and fails when the signature is wrong, the license expired or `refresh_after` has passed.

## Licenses, products and customers

```rust
let admin = kora::Client::new("https://kora.example.com").api_key(std::env::var("KORA_TOKEN")?);

let (total, rows) = admin.licenses().list(&[("status", "active"), ("limit", "25")])?;
let license = admin.licenses().create(serde_json::json!({ "product_id": 1, "customer_id": 4 }))?;
admin.licenses().update("42", serde_json::json!({ "status": "suspended" }))?;
admin.customers().update("4", serde_json::json!({ "blocked": true }))?;
admin.merge("4", "9")?;
```

Failed calls return `Error::Status(status, message)`.
