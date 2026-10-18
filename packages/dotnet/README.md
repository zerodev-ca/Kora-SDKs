# ZeroDev.Kora

Official .NET SDK for Kora. Targets .NET 8.

```bash
dotnet add package ZeroDev.Kora
```

## Validation

```csharp
using Kora;

using var kora = new KoraClient("https://kora.example.com", product: "MyApp", publicKey: Environment.GetEnvironmentVariable("KORA_PUBLIC_KEY"));

var result = await kora.ValidateAsync(licenseKey, fields: new() { ["hwid"] = machineId });
if (!result.Valid) throw new Exception(result.Message);
```

With `publicKey` set, an answer without a valid `x-kora-signature` throws `KoraException`. A rejected license is not an exception: check `Valid` and `Code`.

## Devices

```csharp
await kora.ActivateAsync(licenseKey, fields: new() { ["hwid"] = machineId });
await kora.DeactivateAsync(licenseKey, new() { ["hwid"] = machineId });
```

`ActivateAsync` takes a free seat in each whitelist group of the product, then validates. `DeactivateAsync` frees it and returns `Success`, `Code` and `Removed`.

## Offline licenses

```csharp
var file = await admin.FileAsync("42");
var license = kora.Offline(file);
```

`Offline` checks the file against `publicKey` with no network call and throws when the signature is wrong, the license expired or `refresh_after` has passed.

## Licenses, products and customers

```csharp
using var admin = new KoraClient("https://kora.example.com", apiKey: Environment.GetEnvironmentVariable("KORA_TOKEN"));

var page = await admin.Licenses.ListAsync(new() { ["status"] = "active", ["limit"] = "25" });
var license = await admin.Licenses.CreateAsync(new { product_id = 1, customer_id = 4 });
await admin.Licenses.UpdateAsync(license["id"]!.ToString(), new { status = "suspended" });
await admin.Customers.UpdateAsync("4", new { blocked = true });
await admin.MergeAsync("4", "9");
```

Failed calls throw `KoraException` with `Status`.
