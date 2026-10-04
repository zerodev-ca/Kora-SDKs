# kora-java

Official Java SDK for Kora. Java 17+, works on Paper, Spigot, Velocity and Folia.

```xml
<dependency>
    <groupId>ca.zerodev</groupId>
    <artifactId>kora-java</artifactId>
    <version>3.0.0</version>
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

Every resource has `list`, `get`, `create`, `update` and `delete`. `delete(id, true)` forces removal of products and customers that still have licenses. Failed requests throw `KoraException` with `getStatus()`.
