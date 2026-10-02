# Kora SDKs

Official client SDKs for integrating Kora license validation and session management into applications, plugins, and game servers.

## Packages

| Package | Language / Platform | Directory |
|---|---|---|
| `@zerodev-ca/kora` | Node.js / TypeScript | `packages/node` |
| `ca.zerodev:kora-java` | Java 17+ (Paper, Spigot, Velocity, Folia) | `packages/java` |
| `zerodev-kora` | Python 3.8+ | `packages/python` |
| `kora-fivem` | FiveM / RedM (callable from Lua and JS) | `packages/fivem` |

## Features

- Ed25519 response signature validation using backend public keys, with nonce and timestamp checks against replayed answers.
- Automatic hardware identification (HWID) hashing across platforms.
- Background session keep-alive heartbeats.
- Offline license file and grace period verification.
- Variable and user variable retrieval during validation.
- Device release, offline license requests and update checks.

## Server

Point `url` at your Kora API followed by `/api/v1`. Every SDK posts to `/licenses/validate`, `/licenses/deactivate`, `/licenses/offline` and `/updates/check`, and verifies the `x-kora-signature` header with the public key from `GET /api/v1/public-key`.
