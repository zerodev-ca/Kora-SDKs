# kora-java

Official Java SDK for Kora licensing. Java 17+, works on Paper, Spigot, Folia, Velocity, Fabric and Forge. Uses Gson, which Minecraft already ships.

## Installation

```xml
<dependency>
    <groupId>ca.zerodev</groupId>
    <artifactId>kora-java</artifactId>
    <version>1.1.0</version>
</dependency>
```

## Usage

```java
KoraClient kora = new KoraClient.Builder()
    .url("https://api.yourdomain.com/api/v1")
    .product("MyPlugin")
    .key(getConfig().getString("license-key"))
    .publicKey(PUBLIC_KEY)
    .heartbeatIntervalMs(300000)
    .build();

KoraResponse result = kora.validate();
if (!result.isValid()) {
    getLogger().severe(result.getCode() + ": " + result.getMessage());
}
```

Run `validate()` off the main thread. With a public key set, every answer must carry a valid `x-kora-signature`, the nonce the client sent, and a timestamp within `maxSkewMs` (five minutes by default).

## Other calls

```java
kora.deactivate();
String file = kora.requestOffline();
boolean usable = kora.verifyOffline(file);
JsonObject update = kora.checkUpdate("1.4.2", "stable");
```

Call `kora.stop()` in `onDisable` to stop the heartbeat.
