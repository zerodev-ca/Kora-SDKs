# Kora Go SDK

```bash
go get github.com/zerodev-ca/Kora-SDKs/packages/go
```

## Validation

```go
client := kora.New(kora.Options{URL: "https://kora.example.com", Product: "MyApp", PublicKey: os.Getenv("KORA_PUBLIC_KEY")})

result, err := client.Validate(context.Background(), licenseKey, map[string]string{"hwid": machineID})
if err != nil {
    log.Fatal(err)
}
if !result.Valid {
    log.Fatal(result.Message)
}
```

With `PublicKey` set, an answer without a valid `x-kora-signature` returns an error. A rejected license is not an error: check `result.Valid` and `result.Code`.

## Devices

```go
activated, err := client.Activate(ctx, licenseKey, map[string]string{"hwid": machineID})
released, err := client.Deactivate(ctx, licenseKey, map[string]string{"hwid": machineID})
```

`Activate` takes a free seat in each whitelist group of the product, then validates. `Deactivate` frees it and returns `Success`, `Code` and `Removed`.

## Offline licenses

```go
file, err := admin.File(ctx, "42")
license, err := client.Offline(file)
```

`Offline` checks the file against `PublicKey` with no network call and fails when the signature is wrong, the license expired or `RefreshAfter` has passed.

## Licenses, products and customers

```go
admin := kora.New(kora.Options{URL: "https://kora.example.com", APIKey: os.Getenv("KORA_TOKEN")})

page, err := admin.Licenses.List(ctx, map[string]string{"status": "active", "limit": "25"})
license, err := admin.Licenses.Create(ctx, map[string]any{"product_id": 1, "customer_id": 4})
_, err = admin.Licenses.Update(ctx, "42", map[string]any{"status": "suspended"})
_, err = admin.Customers.Update(ctx, "4", map[string]any{"blocked": true})
merged, err := admin.Merge(ctx, "4", "9")
```

Failed calls return a `*kora.Error` with `Status`.
