# Kora SDKs

Official client SDKs for integrating Kora license validation and session management into applications, plugins, and game servers.

## Packages

| Package | Language / Platform | Directory |
|---|---|---|
| `@zerodev/kora` | Node.js / TypeScript | `packages/node` |
| `ca.zerodev:kora-java` | Java 17+ (Paper, Spigot, Velocity, Folia) | `packages/java` |
| `kora-sdk` | Python 3.8+ | `packages/python` |
| `kora-fivem` | FiveM / RedM (Lua & JS) | `packages/fivem` |

## Features

- Ed25519 response signature validation using backend public keys.
- Automatic hardware identification (HWID) hashing across platforms.
- Background session keep-alive heartbeats.
- Offline license file and grace period verification.
- Variable and user variable retrieval during validation.
