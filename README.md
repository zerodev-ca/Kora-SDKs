# Kora SDKs

Official client SDKs for checking Kora licenses from applications, plugins and game servers.

## Packages

| Package | Language / Platform | Directory |
|---|---|---|
| `@zerodev-ca/kora` | Node.js / TypeScript | `packages/node` |
| `ca.zerodev:kora-java` | Java 17+ (Paper, Spigot, Velocity, Folia) | `packages/java` |
| `zerodev-kora` | Python 3.8+ | `packages/python` |
| `kora-fivem` | FiveM / RedM (callable from Lua and JS) | `packages/fivem` |

## Features

- License validation against a product, with the license, product and customer in the answer.
- Optional API key, for when **Require API Key** is on in the Kora dashboard.
- Optional re-check on an interval that reports when a license stops being valid.

## Server

Set `url` to your Kora API address. Every SDK posts `license_key` and `product_name` to `/api/v1/licenses/validate` and sends `Authorization: Bearer <api key>` when an API key is set. The **Integration** page in the Kora dashboard shows the address and ready-to-copy examples.
