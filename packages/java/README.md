# kora-java

Official Java SDK for Kora. Java 17+, works on Paper, Spigot, Velocity and Folia.

```xml
<dependency>
    <groupId>ca.zerodev</groupId>
    <artifactId>kora-java</artifactId>
    <version>3.1.1</version>
</dependency>
```

## Validation

```java
KoraClient kora = new KoraClient.Builder()
    .url("https://kora.example.com")
    .product("MyPlugin")
    .publicKey(publicKeyPem)
    .build();

KoraResponse result = kora.validate(getConfig().getString("license"));
if (!result.isValid()) {
    getLogger().severe(result.getMessage());
    getServer().getPluginManager().disablePlugin(this);
}
```

`KoraResponse` has `isValid()`, `getCode()`, `getMessage()`, `getLicense()` and `getTimestamp()`. With a public key set, an answer without a valid `x-kora-signature` throws `KoraException`.

Extra fields go in a map, for example the device id a whitelist group reads: `kora.validate(key, "MyPlugin", Map.of("hwid", machineId))`.

## Devices

```java
KoraResponse activated = kora.activate(key, Map.of("hwid", machineId));
if (activated.getCode().equals("seats_full")) {
    getLogger().warning(activated.getMessage());
}

kora.deactivate(key, Map.of("hwid", machineId));
```

`activate` takes a free seat in each whitelist group of the product for the values in the request, then validates. `deactivate` frees them and returns a `KoraDeactivation` with `isSuccess()`, `getCode()` (`deactivated`, `not_activated`, `not_found`) and `getRemoved()`.

## Offline licenses

```java
String file = admin.file(license.get("id").getAsLong());
JsonObject offline = kora.offline(file);
```

`file` downloads a signed license file with an API token. `offline` checks the file against the public key without a network call and throws `KoraException` when the signature is wrong, the license expired or `refresh_after` has passed. Validate online first and fall back to the file when Kora cannot be reached.

## Licenses, products and customers

```java
KoraClient kora = new KoraClient.Builder().url("https://kora.example.com").apiKey(token).build();

JsonObject product = kora.products().create(Map.of("name", "MyPlugin", "version", "1.0.0"));
JsonObject customer = kora.customers().create(Map.of("name", "Jane", "email", "jane@example.com"));
JsonObject license = kora.licenses().create(Map.of("product_id", product.get("id").getAsLong(), "customer_id", customer.get("id").getAsLong()));

KoraPage page = kora.licenses().list(Map.of("status", "active", "limit", 25));
kora.licenses().update(license.get("id").getAsLong(), Map.of("status", "suspended"));
kora.licenses().delete(license.get("id").getAsLong());
```

Every resource has `list`, `get`, `create`, `update` and `delete`. Block a customer with `customers().update(id, Map.of("blocked", true))` and fold a duplicate into a customer with `kora.merge(id, duplicateId)`. `delete(id, true)` forces removal of products and customers that still have licenses. Failed requests throw `KoraException` with `getStatus()`.
