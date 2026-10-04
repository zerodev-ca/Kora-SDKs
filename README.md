# Kora SDKs

Official client SDKs for Kora. Each SDK validates license keys and manages licenses, products and customers through the Kora API.

## Packages

| Package | Language / Platform | Directory |
|---|---|---|
| `@zerodev-ca/kora` | Node.js / TypeScript | `packages/node` |
| `ca.zerodev:kora-java` | Java 17+ (Paper, Spigot, Velocity, Folia) | `packages/java` |
| `zerodev-kora` | Python 3.8+ | `packages/python` |
| `kora-fivem` | FiveM / RedM (callable from Lua and JS) | `packages/fivem` |

## Features

- License validation with the license, product, customer and addons in the answer.
- Optional Ed25519 signature check on validation answers.
- Create, read, update, delete and list licenses, products and customers.

## Server

Set `url` to your Kora API address. Validation calls `POST /api/v1/licenses/validate` and needs no API key unless **Require API Key** is on. Licenses, products and customers use `/api/v1/licenses`, `/api/v1/products` and `/api/v1/customers` and need an API token from the **API Tokens** page with the matching scopes. The **Integration** page in the dashboard has the request URL, the public key and a license tester.
