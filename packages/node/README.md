# @zerodev-ca/kora

Official Node.js and TypeScript SDK for Kora licensing.

## Installation

```bash
npm install @zerodev-ca/kora
# or
bun add @zerodev-ca/kora
```

## Usage

```typescript
import { Client } from "@zerodev-ca/kora";

const client = new Client({
    url: "https://api.yourdomain.com/api/v1",
    product: "MySoftware",
    key: process.env.LICENSE_KEY,
    publicKey: "-----BEGIN PUBLIC KEY-----\n...",
    heartbeatIntervalMs: 300000
});

const result = await client.validate();
if (result.valid) {
    console.log("Licensed to", result.user, "with", result.addons);
} else {
    console.error(`${result.code}: ${result.message}`);
}
```

`url` is your Kora API address followed by `/api/v1`. The public key is shown under **Settings** in the Kora dashboard and at `GET /api/v1/public-key`. With a public key set, every answer is checked for a valid `x-kora-signature`, the nonce the client sent, and a timestamp within `maxSkewMs` (five minutes by default).

`heartbeatIntervalMs` re-validates in the background with the session token, which keeps a concurrent session slot alive.

## Releasing the device

```typescript
await client.deactivate();
```

## Offline licenses

```typescript
import { writeFileSync, readFileSync } from "fs";

writeFileSync("license.json", JSON.stringify(await client.requestOffline()));

const offline = client.verifyOffline(readFileSync("license.json", "utf8"));
if (offline !== null) {
    console.log("Offline license valid until", new Date(offline.expires_at + offline.grace_period_ms));
}
```

## Updates

```typescript
const update = await client.checkUpdate("1.4.2");
if (update.update_available && update.latest && update.download_url) {
    console.log(`Version ${update.latest.version} is available at ${update.download_url}`);
}
```
