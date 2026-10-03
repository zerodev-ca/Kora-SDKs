# kora-java

Official Java SDK for Kora licensing. Java 17+, works on Paper, Spigot, Folia, Velocity, Fabric and Forge. Uses Gson, which Minecraft already ships.

## Installation

```xml
<dependency>
    <groupId>ca.zerodev</groupId>
    <artifactId>kora-java</artifactId>
    <version>2.1.0</version>
</dependency>
```

## Usage

```java
KoraClient kora = new KoraClient.Builder()
    .url("https://api.yourdomain.com")
    .product("MyPlugin")
    .key(getConfig().getString("license-key"))
    .publicKey(PUBLIC_KEY)
    .build();

KoraResponse result = kora.validate();
if (!result.isValid()) {
    getLogger().severe(result.getMessage());
    getServer().getPluginManager().disablePlugin(this);
}
```

Run `validate()` off the main thread. `getLicense()` returns the key, status, expiry, product, customer and custom `getData()`. With a public key set, every answer must carry a valid `x-kora-signature`, the nonce the client sent and a recent timestamp, or a `KoraException` is thrown.

## Options

| Builder method | Description |
|---|---|
| `apiKey(String)` | Sent as `Authorization: Bearer`. Needed when Require API Key is on. |
| `publicKey(String)` | Kora's Ed25519 public key. Turns on signature checks. |
| `hwid(String)` | Override the hardware ID, which is otherwise a hash of this machine. |
| `version(String)` | Your plugin version, shown on the device in Kora. |
| `intervalMs(long)` | Re-check the license on this interval after a valid result. |
| `onInvalid(Consumer<KoraResponse>)` | Called with the result when a re-check fails. |

## Offline licenses and updates

```java
String file = kora.offline().getOffline();
boolean usable = kora.verifyOffline(file);

KoraResponse update = kora.checkUpdate("1.4.2", "stable");
if (update.isUpdateAvailable()) {
    getLogger().info("Version " + update.getLatest().getVersion() + " is at " + update.getDownloadUrl());
}
```

Call `kora.stop()` in `onDisable` to stop re-checking.
