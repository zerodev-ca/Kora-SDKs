# kora-java

Official Java SDK for Kora licensing. Java 17+, works on Paper, Spigot, Folia, Velocity, Fabric and Forge. Uses Gson, which Minecraft already ships.

## Installation

```xml
<dependency>
    <groupId>ca.zerodev</groupId>
    <artifactId>kora-java</artifactId>
    <version>2.0.0</version>
</dependency>
```

## Usage

```java
KoraClient kora = new KoraClient.Builder()
    .url("https://api.yourdomain.com")
    .product("MyPlugin")
    .key(getConfig().getString("license-key"))
    .build();

KoraResponse result = kora.validate();
if (!result.isValid()) {
    getLogger().severe(result.getMessage());
    getServer().getPluginManager().disablePlugin(this);
}
```

Run `validate()` off the main thread. `url` is your Kora API address. `getLicense()` returns the key, status, expiry, product and customer, and `getCode()` the HTTP status.

## Options

| Builder method | Description |
|---|---|
| `apiKey(String)` | Sent as `Authorization: Bearer`. Needed when Require API Key is on. |
| `intervalMs(long)` | Re-check the license on this interval after a valid result. |
| `onInvalid(Consumer<KoraResponse>)` | Called with the result when a re-check fails. |

Call `kora.stop()` in `onDisable` to stop re-checking.
