# @zerodev-ca/kora

Official Node.js and TypeScript SDK for Kora licensing.

## Installation

```bash
npm install @zerodev-ca/kora
```

## Usage

```typescript
import { Client } from "@zerodev-ca/kora";

const client = new Client({
    url: "https://api.yourdomain.com",
    product: "MySoftware",
    key: process.env.LICENSE_KEY,
    publicKey: process.env.KORA_PUBLIC_KEY
});

const result = await client.validate();
if (!result.valid) {
    console.error(result.message);
    process.exit(1);
}
```

`result` holds `valid`, `message`, `code` (the HTTP status) and `license`, which carries the key, status, expiry, product, customer and your custom `data`. With `publicKey` set, every answer must carry a valid `x-kora-signature`, the nonce the client sent and a recent timestamp, or the call throws.

## Options

| Option | Description |
|---|---|
| `apiKey` | Sent as `Authorization: Bearer`. Needed when Require API Key is on. |
| `publicKey` | Kora's Ed25519 public key. Turns on signature checks. |
| `hwid` | Override the hardware ID, which is otherwise a hash of this machine. |
| `version` | Your product version, shown on the device in Kora. |
| `intervalMs` | Re-check the license on this interval after a valid result. |
| `onInvalid` | Called with the result when a re-check fails. |
| `timeoutMs` / `maxSkewMs` | Request timeout and allowed clock drift. |

## Offline licenses

```typescript
import { writeFileSync, readFileSync } from "fs";

const answer = await client.offline();
if (answer.offline !== null) {
    writeFileSync("license.json", JSON.stringify(answer.offline));
}

if (client.verifyOffline(readFileSync("license.json", "utf8")) === null) {
    process.exit(1);
}
```

## Updates

```typescript
const update = await client.checkUpdate("1.4.2", "stable");
if (update.update_available && update.latest !== null) {
    console.log(`Version ${update.latest.version}: ${update.download_url}`);
}
```

Call `client.stop()` to stop re-checking.
