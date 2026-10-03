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

- License validation, with the license, product, customer and custom data in the answer.
- A hardware ID sent with every check, so Kora can enforce device limits.
- Ed25519 signature checks with nonce and timestamp, so a fake or replayed answer is rejected.
- Signed offline license files with a grace period (Node, Python, Java).
- Update checks with signed download links.
- Optional API key, for when **Require API Key** is on.
- Optional re-check on an interval that reports when a license stops being valid.

## Server

Set `url` to your Kora API address. The SDKs call `/api/v1/licenses/validate`, `/api/v1/licenses/offline` and `/api/v1/updates/check`. Copy the public key from the **Integration** page in the Kora dashboard, or from `GET /api/v1/public-key`.
