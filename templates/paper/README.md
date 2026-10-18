# Paper plugin template

A Paper 1.21 plugin with a Kora license check already built in. Copy this folder, rename the package and `plugin.yml`, and put your own code in `MyPlugin`.

```bash
mvn package
```

The jar in `target/` has the Kora SDK shaded in under your own package.

## What it does

1. Reads `license` from `config.yml`. An empty key disables the plugin with a message telling the server owner where to put it.
2. Activates the server on the license with a server id stored in `plugins/MyPlugin/server-id`, so each server takes one seat when the product has a seat limit.
3. Disables the plugin with Kora's own message when the license is rejected.
4. When Kora cannot be reached, falls back to `plugins/MyPlugin/license.lic`, a signed offline license the buyer downloads from the client dashboard.

The check runs off the main thread, so a slow network never stalls startup. `isLicensed()` tells the rest of your code whether the check passed.

## Config

| Key | What it is |
| --- | --- |
| `license` | The buyer's license key |
| `kora.url` | Your Kora API address |
| `kora.product` | The product name in Kora |
| `kora.public-key` | The public key from the Integration page, used to check signed answers and offline files |
