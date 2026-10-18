# zerodev/kora

Official PHP SDK for Kora. Needs PHP 8.1+ with the `curl`, `json` and `sodium` extensions.

```bash
composer require zerodev/kora
```

## Validation

```php
use ZeroDev\Kora\Client;

$kora = new Client("https://kora.example.com", product: "MyPlugin", publicKey: getenv("KORA_PUBLIC_KEY"));

$result = $kora->validate($licenseKey, ["hwid" => $machineId]);
if (!$result["valid"]) {
    exit($result["message"]);
}
```

With `publicKey` set, an answer without a valid `x-kora-signature` throws `KoraException`. A rejected license is not an exception: check `valid` and `code`.

## Devices

```php
$kora->activate($licenseKey, ["hwid" => $machineId]);
$kora->deactivate($licenseKey, ["hwid" => $machineId]);
```

`activate` takes a free seat in each whitelist group of the product, then validates. `deactivate` frees it and returns `success`, `code` and `removed`.

## Offline licenses

```php
$file = $admin->file(42);
$license = $kora->offline($file);
```

`offline` checks the file against `publicKey` with no network call and throws when the signature is wrong, the license expired or `refresh_after` has passed.

## Licenses, products and customers

```php
$admin = new Client("https://kora.example.com", apiKey: getenv("KORA_TOKEN"));

$page = $admin->licenses->list(["status" => "active", "limit" => 25]);
$license = $admin->licenses->create(["product_id" => 1, "customer_id" => 4]);
$admin->licenses->update($license["id"], ["status" => "suspended"]);
$admin->customers->update(4, ["blocked" => true]);
$admin->merge(4, 9);
```

Failed calls throw `KoraException` with `status`.
