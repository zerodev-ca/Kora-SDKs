# Kora SDKs

Official client SDKs for Kora. Each SDK validates license keys and manages licenses, products and customers through the Kora API.

## Packages

| Package | Language / Platform | Directory |
|---|---|---|
| `@zerodev-ca/kora` | Node.js / TypeScript | `packages/node` |
| `ca.zerodev:kora-java` | Java 17+ (Paper, Spigot, Velocity, Folia) | `packages/java` |
| `zerodev-kora` | Python 3.8+ | `packages/python` |
| `kora-fivem` | FiveM / RedM (callable from Lua and JS) | `packages/fivem` |
| `ZeroDev.Kora` | .NET 8 | `packages/dotnet` |
| `zerodev/kora` | PHP 8.1+ | `packages/php` |
| `github.com/zerodev-ca/Kora-SDKs/packages/go/v3` | Go 1.21+ | `packages/go` |
| `zerodev-kora` | Rust | `packages/rust` |

## Templates

| Template | What it is | Directory |
| --- | --- | --- |
| Paper plugin | A Minecraft plugin with the license check, seat activation and an offline fallback built in | `templates/paper` |

## Features

- License validation with the license, product, customer and addons in the answer.
- Optional Ed25519 signature check on validation answers.
- Device activation and deactivation against whitelist seats.
- Signed offline license files, checked without a network call.
- Create, read, update, delete and list licenses, products and customers, plus customer merge.

## Server

Set `url` to your Kora API address. Validation calls `POST /api/v1/licenses/validate`, `POST /api/v1/licenses/activate` and `POST /api/v1/licenses/deactivate` and needs no API key unless **Require API Key** is on. Offline files come from `GET /api/v1/licenses/:id/file`. Licenses, products and customers use `/api/v1/licenses`, `/api/v1/products` and `/api/v1/customers` and need an API token from the **API Tokens** page with the matching scopes. The **Integration** page in the dashboard has the request URL, the public key and a license tester.

## Releasing

Bump the version in every package, then publish a GitHub release named `v<version>`. The **Release SDKs** workflow runs on the release, or by hand for one package.

| Package | Published to | Needs |
| --- | --- | --- |
| Node | npm, staged for approval | Trusted publisher on npmjs.com |
| Python | PyPI | Trusted publisher on pypi.org, `pypi` environment |
| Java, FiveM | Release assets | Nothing |
| Go | `packages/go/v<version>` tag, Go proxy | Nothing |
| PHP | Packagist, from the root `composer.json` and the release tag | Repository submitted on packagist.org |
| .NET | NuGet | `NUGET_API_KEY` secret |
| Rust | crates.io | `CARGO_REGISTRY_TOKEN` secret |
